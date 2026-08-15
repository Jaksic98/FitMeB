package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class InvalidResetTokenException extends CustomException {
  public InvalidResetTokenException() {
    super("Link za reset lozinke nije ispravan ili je istekao", ErrorCode.INVALID_RESET_TOKEN);
  }
}
