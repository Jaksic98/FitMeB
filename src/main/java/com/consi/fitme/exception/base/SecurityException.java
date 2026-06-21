package com.consi.fitme.exception.base;

import com.consi.fitme.model.ErrorCode;

public class SecurityException extends RuntimeException implements CustomError {
  private final ErrorCode errorCode;

  public SecurityException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
