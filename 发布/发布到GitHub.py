"""Publish verified local APK archives to the configured project only. No credentials are saved.
Usage: python 发布/发布到GitHub.py --check | --publish
Requires Git, existing GitHub Git credentials, and user authorization to publish.
"""
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
REPO = "Shen-huang-123/Four-Symbols-Chess-Sovereign"
BASE = "https://api.github.com/repos/" + REPO
LATEST = "v5.6"
ORIGINAL = "98063f1c1304c2ffedee94f3558814c4d4db3bed"
HEADERS = {}

def git(*args, data=None):
    result = subprocess.run(["git", *args], input=data, cwd=ROOT, capture_output=True)
    if result.returncode:
        raise RuntimeError("git " + " ".join(args[:2]) + " failed: " + result.stderr.decode("utf-8", "replace"))
    return result.stdout.decode("utf-8").strip()

def request(url, method="GET", data=None, binary=False):
    if not url.startswith("https://") or urllib.parse.urlparse(url).hostname not in ("api.github.com", "uploads.github.com"):
        raise ValueError("Unapproved API endpoint")
    content = data if binary else json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
    headers = dict(HEADERS)
    if content is not None:
        headers["Content-Type"] = "application/octet-stream" if binary else "application/json"
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, data=content, headers=headers, method=method), timeout=120) as response:
                body = response.read()
                return json.loads(body) if body else None
        except urllib.error.HTTPError as error:
            if error.code not in (429, 500, 502, 503, 504) or attempt == 3:
                raise RuntimeError(f"GitHub {method} returned HTTP {error.code}: {error.read().decode('utf-8', 'replace')[:500]}") from None
        except (TimeoutError, urllib.error.URLError):
            if attempt == 3:
                raise RuntimeError(f"GitHub {method} network failure") from None
        time.sleep(2 ** attempt)

def inventory():
    items = json.loads((ROOT / "发布/版本清单.json").read_text(encoding="utf-8"))
    assert len(items) >= 38 and len({row["tag"] for row in items}) == len(items)
    for row in items:
        path = ROOT / row["apk"]
        assert path.is_file() and path.stat().st_size == row["size"], path
        assert hashlib.sha256(path.read_bytes()).hexdigest() == row["sha256"], path
        assert row["manifest"]["versionName"] == row["tag"][1:] + ".0", row["tag"]
        for name in ("notes", "checksum"):
            assert (ROOT / row[name]).is_file(), row[name]
    assert items[-1]["tag"] == LATEST
    old_apk = subprocess.run(["git", "show", ORIGINAL + ":安装包/最新版/四象棋尊-v3.3.apk"], cwd=ROOT, capture_output=True, check=True).stdout
    old = next(row for row in items if row["tag"] == "v3.3")
    assert hashlib.sha256(old_apk).hexdigest() == old["sha256"], "v3.3 source/APK mismatch"
    return items

def authenticate():
    env = dict(os.environ, GIT_TERMINAL_PROMPT="0", GCM_INTERACTIVE="never")
    result = subprocess.run(["git", "credential", "fill"], input="protocol=https\nhost=github.com\n\n", text=True, capture_output=True, env=env, timeout=30)
    values = dict(line.split("=", 1) for line in result.stdout.splitlines() if "=" in line)
    if not values.get("password"):
        raise RuntimeError("Existing GitHub credentials are required")
    HEADERS.update({"Authorization": "Bearer " + values["password"], "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "FourSymbolsRelease"})
    remote = git("remote", "get-url", "origin")
    if remote.rstrip("/").removesuffix(".git") != "https://github.com/" + REPO:
        raise RuntimeError("Origin does not match the authorized repository")

def prepare_tags(items, head):
    tags = []
    existing = set(git("tag", "--list").splitlines())
    for row in items:
        tag = row["tag"]
        if tag == LATEST:
            target = git("rev-parse", tag + "^{commit}") if tag in existing else head
            if tag in existing:
                # Release tags are immutable; later publishing-documentation commits may advance main.
                code = "源码/四象棋尊/app"
                assert git("rev-parse", target + ":" + code) == git("rev-parse", head + ":" + code), "Latest game source differs; publish a new version"
        elif tag == "v3.3":
            target = ORIGINAL
        elif tag in existing and row["source"] == "完整源码":
            # Preserve earlier verified full-source release snapshots.
            tags.append(tag)
            continue
        elif tag in existing:
            # Resuming a previous upload: validate the historical metadata instead of rewriting tags.
            content = subprocess.run(["git", "show", tag + ":README.md"], cwd=ROOT, capture_output=True, check=True).stdout
            assert content == (ROOT / row["notes"]).read_bytes(), "Archive tag metadata differs: " + tag
            tags.append(tag)
            continue
        else:
            note = git("hash-object", "-w", "--stdin", data=(ROOT / row["notes"]).read_bytes())
            sums = git("hash-object", "-w", "--stdin", data=(ROOT / row["checksum"]).read_bytes())
            tree = git("mktree", data=f"100644 blob {note}\tREADME.md\n100644 blob {sums}\tSHA256SUMS.txt\n".encode())
            target = git("commit-tree", tree, data=f"Archive release metadata for {tag}; original source snapshot unavailable\n".encode())
        if tag in existing:
            assert git("rev-parse", tag + "^{commit}") == target, "Existing tag points elsewhere: " + tag
        else:
            git("tag", "-a", tag, target, "-m", "Four Symbols Chess Sovereign " + tag + (" source release" if tag in (LATEST, "v3.3") else " APK archive; source snapshot unavailable"))
        tags.append(tag)
    git("push", "--atomic", "origin", "main", *["refs/tags/" + tag for tag in tags])

def asset_name(row, key):
    return "Four-Symbols-Chess-Sovereign-" + row["tag"] + ".apk" if key == "apk" else (ROOT / row[key]).name

def release_body(row):
    body = (ROOT / row["notes"]).read_text(encoding="utf-8")
    return body.replace("- APK：", "- 原始 APK 文件名：") + "\n下载附件：`" + asset_name(row, "apk") + "`（与原始 APK 内容一致）。\n"

def upload(row):
    tag = row["tag"]
    # List lookup handles both new drafts and resumed releases without overwriting artifacts.
    releases = request(BASE + "/releases?per_page=100")
    release = next((item for item in releases if item["tag_name"] == tag), None)
    body = release_body(row)
    if release is None:
        release = request(BASE + "/releases", "POST", {"tag_name": tag, "name": row["title"], "body": body, "draft": True, "prerelease": False, "make_latest": "false"})
    for key in ("apk", "checksum"):
        path = ROOT / row[key]
        assets = request(BASE + f"/releases/{release['id']}/assets?per_page=100")
        name = asset_name(row, key)
        existing = next((asset for asset in assets if asset["name"] == name), None)
        payload = path.read_bytes()
        digest = "sha256:" + hashlib.sha256(payload).hexdigest()
        if existing:
            assert existing["size"] == len(payload) and existing.get("digest") == digest, "Existing asset differs: " + path.name
        else:
            url = release["upload_url"].split("{")[0] + "?" + urllib.parse.urlencode({"name": name, "label": path.name})
            asset = request(url, "POST", payload, binary=True)
            assert asset["size"] == len(payload) and asset.get("digest") == digest, "Uploaded asset mismatch"
    result = request(BASE + f"/releases/{release['id']}", "PATCH", {"name": row["title"], "body": body, "draft": False, "prerelease": False, "make_latest": "true" if tag == LATEST else "false"})
    print("PUBLISHED", tag, result["html_url"], flush=True)

def verify(items, head):
    releases = request(BASE + "/releases?per_page=100")
    for row in items:
        release = next(item for item in releases if item["tag_name"] == row["tag"])
        assert not release["draft"] and not release["prerelease"]
        for key in ("apk", "checksum"):
            path = ROOT / row[key]
            asset = next(item for item in release["assets"] if item["name"] == asset_name(row, key))
            assert asset["size"] == path.stat().st_size
            assert asset.get("digest") == "sha256:" + hashlib.sha256(path.read_bytes()).hexdigest()
    assert request(BASE + "/releases/latest")["tag_name"] == LATEST
    assert request(BASE + "/branches/main")["commit"]["sha"] == head
    print("VERIFIED", len(items), "releases;", len(items) * 2, "assets; latest", LATEST, "; main", head, flush=True)

if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    rows = inventory()
    print("CHECKED", len(rows), "local APKs and checksums", flush=True)
    if sys.argv[1:] == ["--publish"]:
        authenticate()
        if git("status", "--porcelain"):
            raise RuntimeError("Commit the reviewed source and release files before publishing")
        commit = git("rev-parse", "HEAD")
        prepare_tags(rows, commit)
        with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
            list(pool.map(upload, rows[:-1]))
        upload(rows[-1])
        verify(rows, commit)
    elif sys.argv[1:] != ["--check"]:
        raise SystemExit("Use --check or --publish")
