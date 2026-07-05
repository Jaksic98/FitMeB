package com.consi.fitme.exception.user;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class PhoneNumberAlreadyExistsException extends CustomException {
  public PhoneNumberAlreadyExistsException(String phoneNumber) {
    super(
        "Korisnik sa brojem telefona već postoji: " + phoneNumber,
        ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
  }
}
