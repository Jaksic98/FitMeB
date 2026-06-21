package com.consi.fitme.exception.user;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class WeakPasswordException extends CustomException {
  public WeakPasswordException() {
    super(
        "Lozinka mora imati najmanje 8 karaktera, bar jedan broj i bar jedan specijalni karakter",
        ErrorCode.WEAK_PASSWORD);
  }
}
