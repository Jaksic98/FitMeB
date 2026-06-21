package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.SecurityException;
import com.consi.fitme.model.ErrorCode;

public class MissingJwtTokenException extends SecurityException {
  public MissingJwtTokenException() {
    super(ErrorCode.MISSING_JWT_TOKEN, "JWT token nedostaje");
  }
}
