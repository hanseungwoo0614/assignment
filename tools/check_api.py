#!/usr/bin/env python3
"""공통 API 동작 검사. 표준 라이브러리만 사용하며 실행 중인 로컬 서버를 검사합니다."""
import argparse, json, sys, time, urllib.request, urllib.error

parser = argparse.ArgumentParser()
parser.add_argument("--base", default="http://127.0.0.1:8000")
parser.add_argument("--mode", choices=["starter", "final"], default="final")
parser.add_argument(
    "--expiry", action="store_true", help="TOKEN_TTL=3으로 서버를 재시작한 뒤 사용"
)
args = parser.parse_args()
passed = 0
created = []


def call(method, path, body=None, token=None, raw=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = (
        raw
        if raw is not None
        else json.dumps(body).encode() if body is not None else None
    )
    req = urllib.request.Request(
        args.base + "/api" + path, data=data, headers=headers, method=method
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            status, content, h = r.status, r.read(), r.headers
    except urllib.error.HTTPError as e:
        status, content, h = e.code, e.read(), e.headers
    parsed = json.loads(content) if content else None
    return status, parsed, h


def check(label, response, status, code=None):
    global passed
    actual, data, headers = response
    assert actual == status, f"{label}: expected HTTP {status}, got {actual}: {data}"
    if code:
        assert data.get("code") == code, f"{label}: expected {code}, got {data}"
    if status == 401 and code != "INVALID_CREDENTIALS":
        assert headers.get("WWW-Authenticate", "").lower().startswith("bearer"), (
            label + ": Bearer challenge missing"
        )
    passed += 1
    print("PASS", label)
    return data


def login(email):
    data = check(
        "login " + email,
        call("POST", "/auth/login", dict(email=email, password="Classroom!2026")),
        200,
    )
    assert (
        isinstance(data.get("accessToken"), str)
        and len(data["accessToken"].split(".")) == 3
    ), "JWT must have 3 segments"
    assert data["user"]["email"] == email and set(data["user"]) == {
        "id",
        "name",
        "email",
    }
    assert isinstance(data.get("expiresIn"), int) and data["expiresIn"] > 0
    return data


def content(title):
    return dict(title=title, content="공통 API 검사에서 생성한 임시 게시글입니다.")


def create(token=None):
    response = call("POST", "/posts", content("API 검사 임시 글"), token)
    p = check("create post", response, 201)
    assert response[2].get("Location") == "/api/posts/" + str(p["id"])
    assert all(
        k in p
        for k in (
            "id",
            "title",
            "content",
            "authorId",
            "authorName",
            "createdAt",
            "updatedAt",
        )
    )
    created.append((p["id"], token))
    return p


def unchanged(post):
    actual = check(
        "failed request leaves data unchanged",
        call("GET", "/posts/" + str(post["id"])),
        200,
    )
    assert actual == post, "Post changed after rejected request"


try:
    check("health", call("GET", "/health"), 200)
    assert isinstance(check("public list", call("GET", "/posts"), 200), list)
    check("bad ID", call("GET", "/posts/abc"), 400, "BAD_INPUT")
    check("missing ID", call("GET", "/posts/9007199254740991"), 404, "NOT_FOUND")
    check(
        "wrong password",
        call("POST", "/auth/login", dict(email="alice@class.test", password="wrong")),
        401,
        "INVALID_CREDENTIALS",
    )
    check(
        "unknown email",
        call(
            "POST",
            "/auth/login",
            dict(email="nobody@class.test", password="Classroom!2026"),
        ),
        401,
        "INVALID_CREDENTIALS",
    )
    if args.mode == "starter":
        check(
            "JWT intentionally unfinished",
            call(
                "POST",
                "/auth/login",
                dict(email="alice@class.test", password="Classroom!2026"),
            ),
            501,
            "NOT_IMPLEMENTED",
        )
        p = create()
        assert p["authorId"] == 1
        check("detail", call("GET", "/posts/" + str(p["id"])), 200)
        p2 = check(
            "update demo",
            call("PUT", "/posts/" + str(p["id"]), content("수정 확인")),
            200,
        )
        assert p2["title"] == "수정 확인" and p2["createdAt"] == p["createdAt"]
        check("bad input", call("POST", "/posts", content("   ")), 400, "BAD_INPUT")
        check(
            "no client author",
            call("POST", "/posts", dict(**content("위조"), authorId=2)),
            400,
            "BAD_INPUT",
        )
    else:
        check("no token me", call("GET", "/me"), 401, "TOKEN_REQUIRED")
        check(
            "no token create",
            call("POST", "/posts", content("무토큰")),
            401,
            "TOKEN_REQUIRED",
        )
        alice = login("alice@class.test")
        bob = login("bob@class.test")
        a, b = alice["accessToken"], bob["accessToken"]
        assert check("Alice me", call("GET", "/me", token=a), 200)["id"] == 1
        assert check("Bob me", call("GET", "/me", token=b), 200)["id"] == 2
        p = create(a)
        assert p["authorId"] == 1
        path = "/posts/" + str(p["id"])
        check("public detail", call("GET", path), 200)
        for method in ("PUT", "DELETE"):
            body = content("타인 수정 시도") if method == "PUT" else None
            check("no token " + method, call(method, path, body), 401, "TOKEN_REQUIRED")
            check("Bob denied " + method, call(method, path, body, b), 403, "FORBIDDEN")
            unchanged(p)
        for field in ("authorId", "userId"):
            check(
                "reject " + field,
                call("POST", "/posts", dict(**content("위조"), **{field: 1}), b),
                400,
                "BAD_INPUT",
            )
        for body in (
            content(" "),
            dict(title="제목", content=" "),
            content("x" * 101),
            dict(title="제목", content="x" * 5001),
        ):
            check("input validation", call("POST", "/posts", body, a), 400, "BAD_INPUT")
        pieces = a.split(".")
        pieces[2] = ("A" if pieces[2][0] != "A" else "B") + pieces[2][1:]
        bad = ".".join(pieces)
        for method, path2, body in [
            ("GET", "/me", None),
            ("POST", "/posts", content("변조")),
            ("PUT", path, content("변조")),
            ("DELETE", path, None),
        ]:
            check(
                "tampered " + method + " " + path2,
                call(method, path2, body, bad),
                401,
                "INVALID_TOKEN",
            )
        unchanged(p)
        updated = check(
            "owner update", call("PUT", path, content("본인 수정 완료"), a), 200
        )
        assert (
            updated["title"] == "본인 수정 완료"
            and updated["authorId"] == 1
            and updated["createdAt"] == p["createdAt"]
        )
        bp = create(b)
        assert bp["authorId"] == 2
        check(
            "Bob own update",
            call("PUT", "/posts/" + str(bp["id"]), content("Bob 수정"), b),
            200,
        )
        for method in ("PUT", "DELETE"):
            check(
                "missing protected " + method,
                call(
                    method,
                    "/posts/9007199254740991",
                    content("없음") if method == "PUT" else None,
                    a,
                ),
                404,
                "NOT_FOUND",
            )
        if args.expiry:
            fresh = login("alice@class.test")
            ttl = fresh["expiresIn"]
            assert (
                ttl <= 10
            ), "Set TOKEN_TTL=3 before --expiry (maximum supported here is 10 seconds)"
            print("Waiting", ttl + 1, "seconds for expiration", flush=True)
            time.sleep(ttl + 1)
            for method, path2, body in [
                ("GET", "/me", None),
                ("POST", "/posts", content("만료")),
                ("PUT", path, content("만료")),
                ("DELETE", path, None),
            ]:
                check(
                    "expired " + method + " " + path2,
                    call(method, path2, body, fresh["accessToken"]),
                    401,
                    "TOKEN_EXPIRED",
                )
            unchanged(updated)
            # Cleanup uses fresh credentials after expiry.
            a = login("alice@class.test")["accessToken"]
            b = login("bob@class.test")["accessToken"]
            created = [
                (pid, a if token == alice["accessToken"] else b)
                for pid, token in created
            ]
    for pid, token in list(created):
        check(
            "delete owned test post",
            call("DELETE", "/posts/" + str(pid), token=token),
            204,
        )
        check(
            "deleted post missing", call("GET", "/posts/" + str(pid)), 404, "NOT_FOUND"
        )
        created.remove((pid, token))
    print(
        f"All {passed} checks passed ({args.mode}). UI and restart tests must be performed separately."
    )
except (AssertionError, urllib.error.URLError, ValueError, KeyError, TypeError) as e:
    print("FAIL", str(e), file=sys.stderr)
    if args.mode == "final":
        print(
            "시작 코드에서는 final 실패가 정상입니다. 구현 체크리스트를 완성한 뒤 재실행하세요.",
            file=sys.stderr,
        )
    if created:
        print(
            "Cleanup may be needed for post IDs:",
            [x[0] for x in created],
            file=sys.stderr,
        )
    sys.exit(1)
