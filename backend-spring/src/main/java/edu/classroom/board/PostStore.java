package edu.classroom.board;

import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PostStore {
  public record Post(
      long id,
      String title,
      String content,
      long authorId,
      String authorName,
      String createdAt,
      String updatedAt) {}

  private final JdbcTemplate db;
  private final Users users;

  public PostStore(JdbcTemplate db, Users users) {
    this.db = db;
    this.users = users;
  }

  private Post shape(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    long author = r.getLong("author_id");
    return new Post(
        r.getLong("id"),
        r.getString("title"),
        r.getString("content"),
        author,
        users.find(author).name(),
        r.getString("created_at"),
        r.getString("updated_at"));
  }

  public List<Post> list() {
    return db.query("SELECT * FROM posts ORDER BY id DESC", this::shape);
  }

  public Post get(long id) {
    return db.query("SELECT * FROM posts WHERE id=?", this::shape, id).stream()
        .findFirst()
        .orElse(null);
  }

  public Post create(String title, String content, long author) {
    String now = Instant.now().toString();
    var key = new GeneratedKeyHolder();
    db.update(
        c -> {
          var s =
              c.prepareStatement(
                  "INSERT INTO posts(title,content,author_id,created_at,updated_at)"
                      + " VALUES(?,?,?,?,?)",
                  Statement.RETURN_GENERATED_KEYS);
          s.setString(1, title);
          s.setString(2, content);
          s.setLong(3, author);
          s.setString(4, now);
          s.setString(5, now);
          return s;
        },
        key);
    return get(key.getKey().longValue());
  }

  public Post update(long id, String title, String content) {
    db.update(
        "UPDATE posts SET title=?,content=?,updated_at=? WHERE id=?",
        title,
        content,
        Instant.now().toString(),
        id);
    return get(id);
  }

  public void delete(long id) {
    db.update("DELETE FROM posts WHERE id=?", id);
  }
}
