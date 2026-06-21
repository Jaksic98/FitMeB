package com.consi.fitme.exception.base;

import com.consi.fitme.model.ErrorCode;

public abstract class NotFoundException extends RuntimeException implements CustomError {
  private final ErrorCode errorCode;

  protected NotFoundException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
