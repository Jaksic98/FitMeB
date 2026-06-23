package com.consi.fitme.service;

import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.repository.AppointmentRepository;
import com.consi.fitme.repository.PilatesRepository;
import com.consi.fitme.repository.TerminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentGenerationService {

  private final AppointmentRepository appointmentRepository;
  private final PilatesRepository pilatesRepository;
  private final TerminRepository terminRepository;

  @Transactional
  public void generateForTermin(Long terminId) {
    pilatesRepository
        .findAllByStatus(Status.ACTIVE)
        .forEach(pilates -> generateIfMissing(terminId, pilates.getId()));
  }

  @Transactional
  public void generateForPilates(Long pilatesId) {
    terminRepository
        .findAllByStatus(Status.ACTIVE)
        .forEach(termin -> generateIfMissing(termin.getId(), pilatesId));
  }

  private void generateIfMissing(Long terminId, Long pilatesId) {
    if (appointmentRepository.existsByTerminIdAndPilatesId(terminId, pilatesId)) {
      return;
    }

    appointmentRepository.save(
        Appointment.builder()
            .terminId(terminId)
            .pilatesId(pilatesId)
            .status(AppointmentStatus.AVAILABLE)
            .build());
  }
}
