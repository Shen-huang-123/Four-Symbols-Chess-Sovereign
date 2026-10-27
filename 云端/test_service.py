"""Local contract/security tests with real Argon2id and a CAS repository, no cloud credentials."""
import copy
import json
import unittest
import uuid
from app import Service, Failure, Changed, PASSWORDS, default_document, validate_document, MAX_BYTES


class MemoryRepository:
    def __init__(self):
        self.files = {}
        self.sequence = 0
        self.before_write = None

    def get(self, path):
        return copy.deepcopy(self.files.get(path, (None, None)))

    def put(self, path, row, expected):
        if self.before_write:
            callback, self.before_write = self.before_write, None
            callback(path)
        _, current = self.files.get(path, (None, None))
        if current != expected:
            raise Changed()
        self.sequence += 1
        self.files[path] = (copy.deepcopy(row), str(self.sequence))


class ArchiveServiceTests(unittest.TestCase):
    def setUp(self):
        self.repo = MemoryRepository()
        self.now = 1800000000
        self.service = Service(self.repo, "local-test-signing-key-only-" + "x" * 40, lambda: self.now)
        self.auth = self.register("alice_01")

    def register(self, account):
        return self.service.handle("POST", "/v1/register", {"account": account, "password": "a-strong-test-passphrase"}, ip="local")

    def request(self, method, path, data=None, session=None):
        return self.service.handle(method, path, data, "Bearer " + (session or self.auth["session"]), ip="local")

    def fails(self, code, callback):
        with self.assertRaises(Failure) as error:
            callback()
        self.assertEqual(code, error.exception.status)
        return error.exception.body

    def test_registration_unique_uuid_argon2id_no_plaintext(self):
        player = self.auth["document"]["id"]
        self.assertEqual(str(uuid.UUID(player)), player)
        auth, _ = self.repo.get(self.service.account_path("alice_01"))
        self.assertTrue(auth["hash"].startswith("$argon2id$"))
        self.assertNotIn("a-strong-test-passphrase", json.dumps(self.repo.files))
        self.fails(409, lambda: self.register("ALICE_01"))

    def test_bad_format_and_password(self):
        self.fails(400, lambda: self.register("../escape"))
        self.fails(400, lambda: self.service.handle("POST", "/v1/register", {"account": "long_name", "password": "short"}))

    def test_login_wrong_password_correct_password(self):
        self.fails(401, lambda: self.service.handle("POST", "/v1/login", {"account": "alice_01", "password": "wrong-password"}))
        login = self.service.handle("POST", "/v1/login", {"account": "alice_01", "password": "a-strong-test-passphrase"})
        self.assertEqual(login["document"]["id"], self.auth["document"]["id"])

    def test_restore_after_client_cache_deleted(self):
        doc = self.auth["document"]
        doc["profile"].update(nickname="月下棋友", avatar=2, bio="四方知音")
        written = self.request("PUT", "/v1/profile", {"revision": 1, "document": doc})
        self.assertEqual(2, written["revision"])
        # A new device knows only account/password, no local profile UUID or session.
        restored = self.service.handle("POST", "/v1/login", {"account": "alice_01", "password": "a-strong-test-passphrase"})
        self.assertEqual("月下棋友", restored["document"]["profile"]["nickname"])
        self.assertEqual(2, restored["revision"])

    def test_unowned_id_cannot_write_or_read(self):
        bob = self.register("bob_0001")
        self.fails(400, lambda: self.request("PUT", "/v1/profile", {"revision": 1, "document": bob["document"]}))
        self.fails(404, lambda: self.request("GET", "/players/" + bob["document"]["id"]))
        self.fails(401, lambda: self.service.handle("GET", "/v1/profile"))
        self.assertNotIn("a-strong-test-passphrase", json.dumps(self.request("GET", "/v1/profile")))

    def test_signed_session_tamper_and_expiry(self):
        token = self.auth["session"]
        self.fails(401, lambda: self.request("GET", "/v1/profile", session=token[:-1] + ("A" if token[-1] != "A" else "B")))
        self.now += 15 * 86400
        self.fails(401, lambda: self.request("GET", "/v1/profile"))

    def test_old_revision_conflict_returns_current_doc(self):
        doc = self.auth["document"]
        self.request("PUT", "/v1/profile", {"revision": 1, "document": doc})
        error = self.fails(409, lambda: self.request("PUT", "/v1/profile", {"revision": 1, "document": doc}))
        self.assertEqual(2, error["revision"])
        self.assertEqual(doc["id"], error["document"]["id"])

    def test_race_after_revision_read_is_caught_by_sha(self):
        # Install write hook only on player file so rate-limit CAS writes do not consume it.
        original_put = self.repo.put
        path = "players/" + self.auth["document"]["id"] + "/document.json"
        def raced(p, value, expected):
            if p == path:
                self.repo.put = original_put
                row, sha = self.repo.get(p)
                row["revision"] += 1
                original_put(p, row, sha)
            return original_put(p, value, expected)
        self.repo.put = raced
        self.fails(409, lambda: self.request("PUT", "/v1/profile", {"revision": 1, "document": self.auth["document"]}))

    def test_logout_revokes_previous_sessions(self):
        self.request("POST", "/v1/logout", {})
        self.fails(401, lambda: self.request("GET", "/v1/profile"))
        self.test_login_wrong_password_correct_password()

    def test_password_change_rejects_old_and_rotates_all_sessions(self):
        self.fails(401, lambda: self.request("POST", "/v1/password", {"oldPassword": "incorrect-long", "newPassword": "new-secure-passphrase"}))
        response = self.request("POST", "/v1/password", {"oldPassword": "a-strong-test-passphrase", "newPassword": "new-secure-passphrase"})
        self.fails(401, lambda: self.request("GET", "/v1/profile"))
        self.request("GET", "/v1/profile", session=response["session"])
        self.fails(401, lambda: self.service.handle("POST", "/v1/login", {"account": "alice_01", "password": "a-strong-test-passphrase"}))

    def test_request_limits_persist_between_service_instances(self):
        for i in range(14):
            self.fails(401, lambda: self.service.handle("POST", "/v1/login", {"account": "alice_01", "password": "wrong-password"}, ip="other"))
        restarted = Service(self.repo, self.service.secret.decode(), lambda: self.now)
        self.fails(429, lambda: restarted.handle("POST", "/v1/login", {"account": "alice_01", "password": "wrong-password"}, ip="other"))
        self.now += 901
        restarted.handle("POST", "/v1/login", {"account": "alice_01", "password": "a-strong-test-passphrase"}, ip="other")

    def test_interrupted_registration_recovers_own_profile(self):
        path = "players/" + self.auth["document"]["id"] + "/document.json"
        del self.repo.files[path]
        self.request("GET", "/v1/profile")
        self.assertIn(path, self.repo.files)

    def test_schema_unknown_secret_field_size_and_bad_outfit_rejected(self):
        d = copy.deepcopy(self.auth["document"])
        d["password"] = "never-store-this"
        self.fails(400, lambda: validate_document(d, d["id"]))
        d = copy.deepcopy(self.auth["document"])
        d["outfit"]["wall"] = "hero9000"
        self.fails(400, lambda: validate_document(d, d["id"]))
        d["profile"]["bio"] = "x" * MAX_BYTES
        self.fails(413, lambda: validate_document(d, d["id"]))


if __name__ == "__main__":
    unittest.main(verbosity=2)


class GitHubAdapterTests(unittest.TestCase):
    def test_private_repository_contents_request_and_cas_header(self):
        import base64
        import io
        from unittest.mock import patch
        from app import GitHubRepository
        doc = {"revision": 1, "document": {"schema": 1}}
        responses = [{"private": True}, {"encoding": "base64", "sha": "current-sha", "content": base64.b64encode(json.dumps(doc).encode()).decode()}, {"content": {"sha": "next-sha"}}]
        requests = []
        def opened(request, timeout):
            requests.append(request)
            return io.BytesIO(json.dumps(responses.pop(0)).encode())
        with patch.dict("os.environ", {"ARCHIVE_REPOSITORY": "test/private-archive", "GITHUB_ARCHIVE_TOKEN": "test-adapter-only", "ARCHIVE_BRANCH": "main"}), patch("urllib.request.urlopen", side_effect=opened):
            repo = GitHubRepository()
            row, sha = repo.get("players/test/document.json")
            self.assertEqual(doc, row)
            repo.put("players/test/document.json", row, sha)
        self.assertEqual("PUT", requests[-1].get_method())
        self.assertEqual("application/json", requests[-1].get_header("Content-type"))
        body = json.loads(requests[-1].data)
        self.assertEqual("current-sha", body["sha"])
        self.assertEqual("main", body["branch"])
        self.assertEqual(doc, json.loads(base64.b64decode(body["content"])))

    def test_public_repository_refused(self):
        import io
        from unittest.mock import patch
        from app import GitHubRepository
        with patch.dict("os.environ", {"ARCHIVE_REPOSITORY": "test/public-archive", "GITHUB_ARCHIVE_TOKEN": "test-adapter-only"}), patch("urllib.request.urlopen", return_value=io.BytesIO(b'{"private": false}')):
            with self.assertRaises(Failure) as error:
                GitHubRepository()
            self.assertEqual(503, error.exception.status)
