package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.SecurityException;
import com.consi.fitme.model.ErrorCode;

public class LogoutFailedException extends SecurityException {
  public LogoutFailedException(String message) {
    super(ErrorCode.LOGOUT_FAILED, message);
  }
}
