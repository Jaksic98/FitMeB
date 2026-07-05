package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class OtpResendCooldownException extends CustomException {
  public OtpResendCooldownException() {
    super("Sačekajte pre ponovnog slanja koda", ErrorCode.OTP_RESEND_COOLDOWN);
  }
}
