package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.repository.AppointmentRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class AppointmentGenerationServiceIT {

  @Autowired private PilatesService pilatesService;
  @Autowired private TerminService terminService;
  @Autowired private AppointmentGenerationService appointmentGenerationService;
  @Autowired private AppointmentRepository appointmentRepository;

  @Test
  void givenExistingActiveTermin_whenPilatesCreated_thenGeneratesAvailableAppointment() {
    String seed = String.valueOf(System.currentTimeMillis());
    TerminDTO termin =
        terminService.createTermin(
            CreateTerminRequestDTO.builder()
                .date(LocalDate.now().plusDays(uniqueOffset()))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    PilatesDTO pilates =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("A1." + seed).name("Reformer").build());

    Appointment appointment =
        appointmentRepository.findAll().stream()
            .filter(
                a ->
                    a.getTerminId().equals(termin.getId())
                        && a.getPilatesId().equals(pilates.getId()))
            .findFirst()
            .orElseThrow();

    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.AVAILABLE);
    assertThat(appointment.getUserId()).isNull();
  }

  @Test
  void givenExistingActivePilates_whenTerminCreated_thenGeneratesAvailableAppointment() {
    String seed = String.valueOf(System.currentTimeMillis());
    PilatesDTO pilates =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("B1." + seed).name("Reformer").build());

    TerminDTO termin =
        terminService.createTermin(
            CreateTerminRequestDTO.builder()
                .date(LocalDate.now().plusDays(uniqueOffset()))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    boolean exists =
        appointmentRepository.existsByTerminIdAndPilatesId(termin.getId(), pilates.getId());
    assertThat(exists).isTrue();
  }

  @Test
  void givenMultipleActivePilatesAndTermini_whenNewTerminCreated_thenGeneratesOneSlotPerPilates() {
    String seed = String.valueOf(System.currentTimeMillis());
    PilatesDTO firstPilates =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("C1." + seed).name("Reformer").build());
    PilatesDTO secondPilates =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("C2." + seed).name("Cadillac").build());

    TerminDTO termin =
        terminService.createTermin(
            CreateTerminRequestDTO.builder()
                .date(LocalDate.now().plusDays(uniqueOffset()))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    assertThat(
            appointmentRepository.existsByTerminIdAndPilatesId(
                termin.getId(), firstPilates.getId()))
        .isTrue();
    assertThat(
            appointmentRepository.existsByTerminIdAndPilatesId(
                termin.getId(), secondPilates.getId()))
        .isTrue();
  }

  @Test
  void givenSlotsAlreadyGenerated_whenGenerationCalledAgain_thenNoDuplicatesAreCreated() {
    String seed = String.valueOf(System.currentTimeMillis());
    TerminDTO termin =
        terminService.createTermin(
            CreateTerminRequestDTO.builder()
                .date(LocalDate.now().plusDays(uniqueOffset()))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());
    PilatesDTO pilates =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("D1." + seed).name("Reformer").build());

    appointmentGenerationService.generateForTermin(termin.getId());
    appointmentGenerationService.generateForPilates(pilates.getId());
    appointmentGenerationService.generateForTermin(termin.getId());

    List<Appointment> matching =
        appointmentRepository.findAll().stream()
            .filter(
                a ->
                    a.getTerminId().equals(termin.getId())
                        && a.getPilatesId().equals(pilates.getId()))
            .toList();

    assertThat(matching).hasSize(1);
  }

  private long uniqueOffset() {
    return System.nanoTime() % 100000;
  }
}
