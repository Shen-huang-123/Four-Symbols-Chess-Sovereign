"""AWS HTTP API/Lambda adapter. GitHub private JSON, own-profile authorization, CAS revisions.

No SQL, no client-side GitHub credentials, no credential logging. Local tests inject a repository.
"""
import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

from argon2 import PasswordHasher, Type
from argon2.exceptions import VerificationError, InvalidHashError

PASSWORDS = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=1, hash_len=32, salt_len=16, type=Type.ID)
DUMMY = PASSWORDS.hash(secrets.token_urlsafe(24))
MAX_BYTES = 750_000
SESSION_SECONDS = 14 * 24 * 3600


class Failure(Exception):
    def __init__(self, status, message, **fields):
        self.status, self.body = status, {"error": message, **fields}


class Changed(Exception):
    pass


class GitHubRepository:
    def __init__(self):
        self.repo = os.environ.get("ARCHIVE_REPOSITORY", "")
        self.token = os.environ.get("GITHUB_ARCHIVE_TOKEN", "")
        self.branch = os.environ.get("ARCHIVE_BRANCH", "main")
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", self.repo) or not self.token:
            raise Failure(503, "云存档仓库尚未配置")
        self.base = f"https://api.github.com/repos/{self.repo}"
        if (self.call(self.base) or {}).get("private") is not True:
            raise Failure(503, "存档仓库必须为私有仓库")

    def call(self, url, method="GET", body=None):
        request = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(), method=method,
                                        headers={"Authorization": "Bearer " + self.token, "Accept": "application/vnd.github+json",
                                                 "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "Four-Symbols-Archive", "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=18) as response:
                return json.load(response)
        except urllib.error.HTTPError as e:
            if e.code == 404 and method == "GET":
                return None
            if e.code in (409, 422):
                raise Changed() from None
            raise Failure(503, "存档服务暂不可用，请保留本地数据并稍后重试") from None
        except (OSError, ValueError):
            raise Failure(503, "云存档网络暂不可用") from None

    def get(self, path):
        row = self.call(self.base + "/contents/" + urllib.parse.quote(path, safe="/") + "?ref=" + urllib.parse.quote(self.branch, safe=""))
        if row is None:
            return None, None
        if row.get("encoding") != "base64":
            raise Failure(503, "存档超过仓库内容接口容量")
        return json.loads(base64.b64decode(row["content"])), row["sha"]

    def put(self, path, value, expected):
        body = {"message": "Update private game archive", "branch": self.branch,
                "content": base64.b64encode(json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode()).decode()}
        if expected is not None:
            body["sha"] = expected
        self.call(self.base + "/contents/" + urllib.parse.quote(path, safe="/"), "PUT", body)


def default_document(player, now):
    return {"schema": 1, "id": player, "created": now * 1000, "guest": False, "cloud": True,
            "profile": {"nickname": "棋友", "bio": "", "avatar": 0}, "matches": {}, "removedMatches": {}, "friends": {},
            "badges": [], "outfit": {"frame": "plain", "wall": "night", "card": "night", "title": "auto",
                                      "board": "ivory", "piece": "ivory", "effect": "quiet"}}


def validate_document(doc, player):
    if not isinstance(doc, dict) or doc.get("schema") != 1 or doc.get("id") != player:
        raise Failure(400, "档案结构或所属玩家无效")
    if len(json.dumps(doc, ensure_ascii=False).encode()) > MAX_BYTES:
        raise Failure(413, "档案容量超限，请先导出备份")
    allowed = {"schema", "id", "created", "guest", "cloud", "profile", "matches", "removedMatches", "friends", "badges", "outfit",
               "statistics", "characters", "friendStats", "achievements", "unlocked", "xp", "level", "localRevision", "cloudRevision", "dirty", "milestones"}
    if set(doc) - allowed:
        raise Failure(400, "不支持的档案字段")
    p = doc.get("profile")
    if not isinstance(p, dict) or not isinstance(p.get("nickname"), str) or not 1 <= len(p["nickname"]) <= 24 or not isinstance(p.get("bio"), str) or len(p["bio"]) > 160 or p.get("avatar") not in (0, 1, 2, 3):
        raise Failure(400, "个人资料格式无效")
    for name, maximum in (("matches", 5000), ("friends", 500), ("removedMatches", 5000)):
        if not isinstance(doc.get(name, {}), dict) or len(doc.get(name, {})) > maximum:
            raise Failure(400, "档案集合格式或数量无效")
        if any(not re.fullmatch(r"[a-f0-9-]{36}", k) for k in doc.get(name, {})):
            raise Failure(400, "档案记录ID无效")
    for key, match in doc["matches"].items():
        if not isinstance(match, dict) or match.get("id") != key or match.get("side") not in (0, 1, 2, 3) or match.get("result") not in ("win", "loss") or match.get("mode") not in ("classic", "skills"):
            raise Failure(400, "棋局记录无效")
        if not isinstance(match.get("players"), list) or len(match["players"]) != 4 or any(not isinstance(v, str) or (v and not re.fullmatch(r"[a-f0-9-]{36}", v)) for v in match["players"]):
            raise Failure(400, "参与者记录无效")
        for count in ("captures", "skills"):
            if type(match.get(count)) is not int or not 0 <= match[count] <= 100000:
                raise Failure(400, "棋局统计格式无效")
        if type(match.get("started")) is not int or type(match.get("ended")) is not int or match["ended"] < match["started"]:
            raise Failure(400, "棋局时间无效")
    outfits = {"frame": {"plain", "gold", "jade", "legend"}, "wall": {"night", "hero0", "hero1", "hero2", "hero3"},
               "card": {"night", "jade"}, "title": {"auto", "hero0", "hero1", "hero2", "hero3"}, "board": {"ivory", "jade"}, "piece": {"ivory", "jade"}, "effect": {"quiet", "ink"}}
    if not isinstance(doc.get("outfit"), dict) or any(k not in outfits or v not in outfits[k] for k, v in doc["outfit"].items()):
        raise Failure(400, "装扮配置无效")
    if not isinstance(doc.get("badges"), list) or len(doc["badges"]) > 3:
        raise Failure(400, "展示徽章最多三个")
    result = json.loads(json.dumps(doc))
    result.update(id=player, guest=False, cloud=True)
    for name in ("localRevision", "cloudRevision", "dirty"):
        result.pop(name, None)
    return result


class Service:
    def __init__(self, repository, secret, clock=time.time):
        if len(secret.encode()) < 32:
            raise Failure(503, "服务端会话签名密钥未配置")
        self.repo, self.secret, self.clock = repository, secret.encode(), clock

    @staticmethod
    def account(value):
        if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z0-9_]{4,32}", value):
            raise Failure(400, "账号须为4～32位字母、数字或下划线")
        return value.lower()

    @staticmethod
    def password(value):
        if not isinstance(value, str) or not 10 <= len(value) <= 128:
            raise Failure(400, "密码须为10～128个字符")
        return value

    def account_path(self, account):
        return "accounts/" + hashlib.sha256(account.encode()).hexdigest() + ".json"

    def limited(self, bucket, limit):
        # Shared across cold starts; no raw IP or account names appear in rate filenames.
        now = int(self.clock())
        path = "limits/" + hmac.new(self.secret, bucket.encode(), hashlib.sha256).hexdigest() + ".json"
        for _ in range(6):
            row, sha = self.repo.get(path)
            window = now // 900
            count = row["count"] if row and row.get("window") == window else 0
            if count >= limit:
                raise Failure(429, "请求过于频繁，请15分钟后重试")
            try:
                self.repo.put(path, {"window": window, "count": count + 1}, sha)
                return
            except Changed:
                continue
        raise Failure(503, "服务繁忙，请稍后重试")

    def session(self, account, auth):
        payload = {"uid": auth["id"], "account": account, "ver": auth["version"], "exp": int(self.clock()) + SESSION_SECONDS,
                   "iat": int(self.clock()), "nonce": secrets.token_urlsafe(16)}
        encoded = base64.urlsafe_b64encode(json.dumps(payload, separators=(",", ":")).encode()).rstrip(b"=")
        sig = base64.urlsafe_b64encode(hmac.new(self.secret, encoded, hashlib.sha256).digest()).rstrip(b"=")
        return (encoded + b"." + sig).decode()

    def authorize(self, authorization):
        try:
            if not authorization.startswith("Bearer ") or len(authorization) > 2048:
                raise ValueError()
            token = authorization[7:]
            encoded, sig = token.split(".")
            expected = base64.urlsafe_b64encode(hmac.new(self.secret, encoded.encode(), hashlib.sha256).digest()).rstrip(b"=").decode()
            if not hmac.compare_digest(expected, sig):
                raise ValueError()
            claims = json.loads(base64.urlsafe_b64decode(encoded + "=" * (-len(encoded) % 4)))
            if claims["exp"] <= self.clock() or claims["iat"] > self.clock() + 60 or claims["exp"] - claims["iat"] > SESSION_SECONDS:
                raise ValueError()
            account = self.account(claims["account"])
            auth, sha = self.repo.get(self.account_path(account))
            if auth is None or auth["id"] != claims["uid"] or auth["version"] != claims["ver"]:
                raise ValueError()
            return account, auth, sha
        except Failure:
            raise
        except (ValueError, KeyError, TypeError):
            raise Failure(401, "登录会话已失效，请重新登录") from None

    def profile(self, auth):
        path = f"players/{auth['id']}/document.json"
        row, sha = self.repo.get(path)
        if row is None:
            # Recover a registration interrupted between unique account creation and initial profile commit.
            row = {"revision": 1, "document": default_document(auth["id"], auth["created"])}
            try:
                self.repo.put(path, row, None)
            except Changed:
                row, sha = self.repo.get(path)
            else:
                row, sha = self.repo.get(path)
        return row, sha

    def handle(self, method, path, body=None, authorization="", ip="unknown"):
        body = body or {}
        if method == "POST" and path in ("/v1/register", "/v1/login"):
            account = self.account(body.get("account"))
            password = self.password(body.get("password"))
            self.limited("ip:" + ip, 80)
            self.limited("account:" + account, 15)
            account_path = self.account_path(account)
            auth, sha = self.repo.get(account_path)
            if path.endswith("register"):
                if auth is not None:
                    raise Failure(409, "账号已存在")
                auth = {"id": str(uuid.uuid4()), "hash": PASSWORDS.hash(password), "version": 1, "created": int(self.clock())}
                try:
                    self.repo.put(account_path, auth, None)
                except Changed:
                    raise Failure(409, "账号已存在") from None
            else:
                try:
                    PASSWORDS.verify(auth["hash"] if auth else DUMMY, password)
                    if auth is None:
                        raise ValueError()
                except (VerificationError, InvalidHashError, ValueError):
                    raise Failure(401, "账号或密码错误") from None
            row, _ = self.profile(auth)
            return {**row, "session": self.session(account, auth)}

        account, auth, auth_sha = self.authorize(authorization)
        self.limited("session:" + auth["id"], 120)
        if path == "/v1/profile" and method in ("GET", "PUT"):
            row, sha = self.profile(auth)
            if method == "GET":
                return row
            if type(body.get("revision")) is not int or body["revision"] != row["revision"]:
                raise Failure(409, "云端档案已有新修订，请合并后同步", **row)
            doc = validate_document(body.get("document"), auth["id"])
            updated = {"revision": row["revision"] + 1, "document": doc}
            try:
                self.repo.put(f"players/{auth['id']}/document.json", updated, sha)
            except Changed:
                latest, _ = self.profile(auth)
                raise Failure(409, "另一设备已更新档案，请合并后同步", **latest) from None
            return updated
        if method == "POST" and path in ("/v1/password", "/v1/logout"):
            if path.endswith("password"):
                try:
                    PASSWORDS.verify(auth["hash"], self.password(body.get("oldPassword")))
                except (VerificationError, InvalidHashError):
                    raise Failure(401, "原密码错误") from None
                auth["hash"] = PASSWORDS.hash(self.password(body.get("newPassword")))
            auth["version"] += 1
            try:
                self.repo.put(self.account_path(account), auth, auth_sha)
            except Changed:
                raise Failure(409, "账号状态已更新，请重新登录") from None
            return {"ok": True, **({"session": self.session(account, auth)} if path.endswith("password") else {})}
        raise Failure(404, "接口不存在，不允许按玩家ID读取私人档案")


_service = None


def lambda_handler(event, context):
    global _service
    try:
        if _service is None:
            _service = Service(GitHubRepository(), os.environ.get("SESSION_SIGNING_KEY", ""))
        http = event.get("requestContext", {}).get("http", {})
        method = http.get("method", "")
        path = event.get("rawPath", "")
        raw = event.get("body") or "{}"
        if event.get("isBase64Encoded"):
            raw = base64.b64decode(raw).decode()
        if len(raw.encode()) > MAX_BYTES + 10000:
            raise Failure(413, "请求过大")
        body = json.loads(raw)
        if not isinstance(body, dict):
            raise Failure(400, "请求必须为JSON对象")
        headers = {k.lower(): v for k, v in event.get("headers", {}).items()}
        result = _service.handle(method, path, body, headers.get("authorization", ""), http.get("sourceIp", "unknown"))
        status = 200
    except Failure as error:
        status, result = error.status, error.body
    except (ValueError, TypeError, KeyError):
        status, result = 400, {"error": "请求格式无效"}
    except Exception:
        # Deliberately do not log event/body/header values or exception repr (can contain credentials).
        status, result = 503, {"error": "服务暂不可用，本地档案不受影响"}
    return {"statusCode": status, "headers": {"Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store",
                                             "X-Content-Type-Options": "nosniff"}, "body": json.dumps(result, ensure_ascii=False)}
