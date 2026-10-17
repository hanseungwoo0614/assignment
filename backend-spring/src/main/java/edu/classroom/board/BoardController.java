package edu.classroom.board;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BoardController {
  private final PostStore store;
  private final AuthService auth;
  private final Users users;

  @Value("${app.jwt.ttl}")
  private int ttl;

  public BoardController(PostStore store, AuthService auth, Users users) {
    this.store = store;
    this.auth = auth;
    this.users = users;
  }

  private Map<String, String> fields(Map<String, Object> body, Map<String, Integer> limits) {
    if (body == null || !limits.keySet().containsAll(body.keySet()))
      throw new ApiError(400, "BAD_INPUT", "허용된 JSON 필드만 보내세요.");
    Map<String, String> out = new HashMap<>();
    for (var e : limits.entrySet()) {
      Object v = body.get(e.getKey());
      if (!(v instanceof String)) throw new ApiError(400, "BAD_INPUT", "문자열을 입력하세요.");
      String s = (String) v, t = s.strip();
      if (t.isEmpty() || t.codePointCount(0, t.length()) > e.getValue())
        throw new ApiError(400, "BAD_INPUT", e.getKey() + " 입력을 확인하세요.");
      out.put(e.getKey(), e.getKey().equals("password") ? s : t);
    }
    return out;
  }

  private long postId(String value) {
    try {
      if (!value.matches("[1-9][0-9]*")) throw new NumberFormatException();
      long id = Long.parseLong(value);
      if (id > 9007199254740991L) throw new NumberFormatException();
      return id;
    } catch (NumberFormatException e) {
      throw new ApiError(400, "BAD_INPUT", "ID는 양의 정수여야 합니다.");
    }
  }

  private PostStore.Post existing(long id) {
    var p = store.get(id);
    if (p == null) throw new ApiError(404, "NOT_FOUND", "게시글이 없습니다.");
    return p;
  }

  @GetMapping("/health")
  public Object health() {
    return Map.of("status", "ok", "stack", "spring");
  }

  @PostMapping("/auth/login")
  public Object login(@RequestBody Map<String, Object> body) {
    var input = fields(body, Map.of("email", 254, "password", 128));
    var user = users.verify(input.get("email"), input.get("password"));
    if (user == null) throw new ApiError(401, "INVALID_CREDENTIALS", "이메일 또는 비밀번호를 확인하세요.");
    return Map.of("accessToken", auth.issueToken(user), "expiresIn", ttl, "user", user);
  }

  @GetMapping("/me")
  public Object me(HttpServletRequest req) {
    var user = users.find(auth.currentUserId(req));
    if (user == null) throw new ApiError(401, "INVALID_TOKEN", "사용자를 확인할 수 없습니다.");
    return user;
  }

  @GetMapping("/posts")
  public Object list() {
    return store.list();
  }

  @GetMapping("/posts/{id}")
  public Object get(@PathVariable String id) {
    return existing(postId(id));
  }

  @PostMapping("/posts")
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest req) {
    long user = auth.currentUserId(req);
    var input = fields(body, Map.of("title", 100, "content", 5000));
    var p = store.create(input.get("title"), input.get("content"), user);
    return ResponseEntity.created(URI.create("/api/posts/" + p.id())).body(p);
  }

  @PutMapping("/posts/{id}")
  public Object update(
      @PathVariable String id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
    long user = auth.currentUserId(req), pid = postId(id);
    var p = existing(pid);
    auth.assertOwner(user, p.authorId());
    var input = fields(body, Map.of("title", 100, "content", 5000));
    return store.update(pid, input.get("title"), input.get("content"));
  }

  @DeleteMapping("/posts/{id}")
  public ResponseEntity<?> delete(@PathVariable String id, HttpServletRequest req) {
    long user = auth.currentUserId(req), pid = postId(id);
    var p = existing(pid);
    auth.assertOwner(user, p.authorId());
    store.delete(pid);
    return ResponseEntity.noContent().build();
  }
}
