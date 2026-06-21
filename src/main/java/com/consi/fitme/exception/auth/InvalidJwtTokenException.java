package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.SecurityException;
import com.consi.fitme.model.ErrorCode;

public class InvalidJwtTokenException extends SecurityException {
  public InvalidJwtTokenException() {
    super(ErrorCode.INVALID_JWT_TOKEN, "JWT token nije validan");
  }
}
