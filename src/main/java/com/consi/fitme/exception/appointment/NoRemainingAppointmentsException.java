package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class NoRemainingAppointmentsException extends CustomException {
  public NoRemainingAppointmentsException() {
    super("Nema preostalih termina za rezervaciju", ErrorCode.NO_REMAINING_APPOINTMENTS);
  }
}
