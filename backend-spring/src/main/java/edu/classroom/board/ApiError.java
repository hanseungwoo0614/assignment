package edu.classroom.board;

public class ApiError extends RuntimeException {
  public final int status;
  public final String code;

  public ApiError(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
