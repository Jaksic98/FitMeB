package com.consi.fitme.exception.user;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class UserNotFoundException extends NotFoundException {
  public UserNotFoundException(String username) {
    super("Korisnik sa korisničkim imenom nije pronađen: " + username, ErrorCode.USER_NOT_FOUND);
  }

  public UserNotFoundException(Long id) {
    super("Korisnik za ID nije pronađen: " + id, ErrorCode.USER_NOT_FOUND);
  }
}
