package com.consi.fitme.exception.base;

import com.consi.fitme.model.ErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException implements CustomError {
  private final ErrorCode errorCode;

  public CustomException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
