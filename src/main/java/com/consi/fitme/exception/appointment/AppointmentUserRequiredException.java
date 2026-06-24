package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class AppointmentUserRequiredException extends CustomException {
  public AppointmentUserRequiredException() {
    super("Korisnik je obavezan za admin rezervaciju", ErrorCode.APPOINTMENT_USER_REQUIRED);
  }
}
