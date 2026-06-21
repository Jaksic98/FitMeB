package com.consi.fitme.exception.user;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class UsernameAlreadyExistsException extends CustomException {
  public UsernameAlreadyExistsException(String username) {
    super(
        "Korisnik sa korisničkim imenom već postoji: " + username,
        ErrorCode.USERNAME_ALREADY_EXISTS);
  }
}
