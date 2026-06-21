package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.SecurityException;
import com.consi.fitme.model.ErrorCode;

public class LoginFailedException extends SecurityException {
  public LoginFailedException(String message) {
    super(ErrorCode.LOGIN_FAILED, message);
  }
}
