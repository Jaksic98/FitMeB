package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class AppointmentOwnershipException extends CustomException {
  public AppointmentOwnershipException() {
    super("Nemate pravo pristupa ovom appointmentu", ErrorCode.APPOINTMENT_OWNERSHIP_VIOLATION);
  }
}
