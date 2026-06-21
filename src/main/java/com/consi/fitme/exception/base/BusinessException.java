package com.consi.fitme.exception.base;

import com.consi.fitme.model.ErrorCode;

public class BusinessException extends RuntimeException implements CustomError {
  private final ErrorCode errorCode;

  public BusinessException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
