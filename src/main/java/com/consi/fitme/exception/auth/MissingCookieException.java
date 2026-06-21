package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.SecurityException;
import com.consi.fitme.model.ErrorCode;

public class MissingCookieException extends SecurityException {
  public MissingCookieException() {
    super(ErrorCode.MISSING_COOKIE, "Kolačić nedostaje");
  }
}
