package com.consi.fitme.exception.auth;

import com.consi.fitme.exception.base.SecurityException;
import com.consi.fitme.model.ErrorCode;

public class AccountLockedException extends SecurityException {
  public AccountLockedException() {
    super(
        ErrorCode.ACCOUNT_LOCKED,
        "Vaš profil je zaključan. Molimo obratite se studiju za pomoć.");
  }
}
