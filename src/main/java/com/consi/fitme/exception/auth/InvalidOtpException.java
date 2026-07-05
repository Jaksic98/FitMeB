package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class InvalidOtpException extends CustomException {
  public InvalidOtpException() {
    super("Kod za verifikaciju nije ispravan ili je istekao", ErrorCode.OTP_INVALID);
  }
}
