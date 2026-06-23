package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class InvalidActivationTokenException extends CustomException {
  public InvalidActivationTokenException() {
    super("Token za aktivaciju nije ispravan ili je istekao", ErrorCode.INVALID_ACTIVATION_TOKEN);
  }
}
