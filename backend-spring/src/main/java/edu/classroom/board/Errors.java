package edu.classroom.board;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(ApiError.class)
  ResponseEntity<?> api(ApiError e) {
    var response = ResponseEntity.status(e.status);
    if (e.status == 401) response.header("WWW-Authenticate", "Bearer");
    return response.body(Map.of("code", e.code, "message", e.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<?> json(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "BAD_INPUT", "message", "올바른 JSON 객체를 보내세요."));
  }
}
