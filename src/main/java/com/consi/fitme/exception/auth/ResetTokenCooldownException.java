package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class ResetTokenCooldownException extends CustomException {
  public ResetTokenCooldownException() {
    super("Sačekajte pre ponovnog slanja linka za reset lozinke", ErrorCode.RESET_TOKEN_COOLDOWN);
  }
}
