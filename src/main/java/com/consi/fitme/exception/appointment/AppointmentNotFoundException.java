package com.consi.fitme.exception.appointment;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class AppointmentNotFoundException extends NotFoundException {
  public AppointmentNotFoundException(Long id) {
    super("Appointment nije pronađen za ID: " + id, ErrorCode.APPOINTMENT_NOT_FOUND);
  }
}
