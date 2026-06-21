package com.consi.fitme.exception.auth;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AuthMeException extends UsernameNotFoundException {
  public AuthMeException(String message) {
    super(message);
  }
}
