package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class MembershipExpiredException extends CustomException {
  public MembershipExpiredException() {
    super("Članarina je istekla", ErrorCode.MEMBERSHIP_EXPIRED);
  }
}
