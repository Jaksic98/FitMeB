package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class SamePasswordException extends CustomException {
  public SamePasswordException() {
    super("Nova lozinka mora biti različita od trenutne", ErrorCode.SAME_AS_OLD_PASSWORD);
  }
}
