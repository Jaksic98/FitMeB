package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class CurrentPasswordMismatchException extends CustomException {
  public CurrentPasswordMismatchException() {
    super("Trenutna lozinka nije ispravna", ErrorCode.CURRENT_PASSWORD_MISMATCH);
  }
}
