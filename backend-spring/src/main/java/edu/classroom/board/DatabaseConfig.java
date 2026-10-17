package edu.classroom.board;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DatabaseConfig {
  @Bean
  public DataSource dataSource(@Value("${app.db.file}") String dbFile) throws IOException {
    Path file = Path.of(dbFile).toAbsolutePath().normalize();
    Files.createDirectories(file.getParent());
    var source = new DriverManagerDataSource();
    source.setDriverClassName("org.sqlite.JDBC");
    source.setUrl("jdbc:sqlite:" + file);
    return source;
  }
}
