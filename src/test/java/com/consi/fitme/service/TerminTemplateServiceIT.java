package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.TerminTemplateDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.CreateTerminTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminTemplateRequestDTO;
import com.consi.fitme.exception.termintemplate.InvalidTerminTemplateTimeRangeException;
import com.consi.fitme.exception.termintemplate.TerminTemplateNotFoundException;
import com.consi.fitme.exception.termintemplate.TerminTemplateOverlapException;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Appointment;
import com.consi.fitme.model.entity.Termin;
import com.consi.fitme.repository.AppointmentRepository;
import com.consi.fitme.repository.TerminRepository;
import java.time.DayOfWeek;
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
class TerminTemplateServiceIT {

  @Autowired private TerminTemplateService service;
  @Autowired private PilatesService pilatesService;
  @Autowired private TerminRepository terminRepository;
  @Autowired private AppointmentRepository appointmentRepository;

  @Test
  void givenNewTerminTemplate_whenCreated_thenStatusDefaultsToActive() {
    TerminTemplateDTO createdTemplate =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    assertThat(createdTemplate.getId()).isNotNull();
    assertThat(createdTemplate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(createdTemplate.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void
      givenEndTimeBeforeStartTime_whenCreateTerminTemplate_thenThrowsInvalidTerminTemplateTimeRangeException() {
    CreateTerminTemplateRequestDTO invalidRangeRequest =
        CreateTerminTemplateRequestDTO.builder()
            .dayOfWeek(DayOfWeek.TUESDAY)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(9, 0))
            .build();

    assertThatThrownBy(() -> service.createTerminTemplate(invalidRangeRequest))
        .isInstanceOf(InvalidTerminTemplateTimeRangeException.class);
  }

  @Test
  void
      givenOverlappingTerminTemplate_whenCreateTerminTemplate_thenThrowsTerminTemplateOverlapException() {
    DayOfWeek day = DayOfWeek.WEDNESDAY;

    service.createTerminTemplate(
        CreateTerminTemplateRequestDTO.builder()
            .dayOfWeek(day)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    CreateTerminTemplateRequestDTO overlappingRequest =
        CreateTerminTemplateRequestDTO.builder()
            .dayOfWeek(day)
            .startTime(LocalTime.of(9, 30))
            .endTime(LocalTime.of(10, 30))
            .build();

    assertThatThrownBy(() -> service.createTerminTemplate(overlappingRequest))
        .isInstanceOf(TerminTemplateOverlapException.class);
  }

  @Test
  void givenNonOverlappingTerminTemplate_whenCreateTerminTemplate_thenSucceeds() {
    DayOfWeek day = DayOfWeek.THURSDAY;

    service.createTerminTemplate(
        CreateTerminTemplateRequestDTO.builder()
            .dayOfWeek(day)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    TerminTemplateDTO secondTemplate =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

    assertThat(secondTemplate.getId()).isNotNull();
  }

  @Test
  void givenSameDayDifferentDayOfWeek_whenCreateTerminTemplate_thenSucceeds() {
    DayOfWeek day1 = DayOfWeek.FRIDAY;
    DayOfWeek day2 = DayOfWeek.SATURDAY;

    service.createTerminTemplate(
        CreateTerminTemplateRequestDTO.builder()
            .dayOfWeek(day1)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    TerminTemplateDTO secondTemplate =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day2)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    assertThat(secondTemplate.getId()).isNotNull();
  }

  @Test
  void givenDeletedOverlappingTerminTemplate_whenCreateTerminTemplate_thenSucceeds() {
    DayOfWeek day = DayOfWeek.SUNDAY;

    TerminTemplateDTO firstTemplate =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    service.deleteTerminTemplate(firstTemplate.getId());

    TerminTemplateDTO secondTemplate =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .build());

    assertThat(secondTemplate.getId()).isNotNull();
  }

  @Test
  void
      givenExistingTerminTemplate_whenUpdateWithOverlappingAnotherTemplate_thenThrowsTerminTemplateOverlapException() {
    DayOfWeek day = DayOfWeek.MONDAY;

    service.createTerminTemplate(
        CreateTerminTemplateRequestDTO.builder()
            .dayOfWeek(day)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    TerminTemplateDTO secondTemplate =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .build());

    UpdateTerminTemplateRequestDTO overlappingUpdateRequest =
        UpdateTerminTemplateRequestDTO.builder()
            .startTime(LocalTime.of(9, 30))
            .endTime(LocalTime.of(10, 30))
            .build();

    assertThatThrownBy(
            () -> service.updateTerminTemplate(secondTemplate.getId(), overlappingUpdateRequest))
        .isInstanceOf(TerminTemplateOverlapException.class);
  }

  @Test
  void givenExistingTerminTemplate_whenUpdateWithSameInterval_thenSucceeds() {
    DayOfWeek day = DayOfWeek.TUESDAY;

    TerminTemplateDTO template =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    UpdateTerminTemplateRequestDTO sameUpdateRequest =
        UpdateTerminTemplateRequestDTO.builder()
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build();

    TerminTemplateDTO updatedTemplate =
        service.updateTerminTemplate(template.getId(), sameUpdateRequest);

    assertThat(updatedTemplate.getId()).isEqualTo(template.getId());
  }

  @Test
  void givenExistingTerminTemplate_whenDeleted_thenIsNotReturnedInGetAll() {
    TerminTemplateDTO template =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .build());

    service.deleteTerminTemplate(template.getId());

    List<TerminTemplateDTO> allTemplates = service.getAllTerminTemplates();
    assertThat(allTemplates).noneMatch(t -> t.getId().equals(template.getId()));
  }

  @Test
  void givenDeletedTerminTemplate_whenGetById_thenThrowsTerminTemplateNotFoundException() {
    TerminTemplateDTO template =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(DayOfWeek.THURSDAY)
                .startTime(LocalTime.of(15, 0))
                .endTime(LocalTime.of(16, 0))
                .build());

    service.deleteTerminTemplate(template.getId());

    assertThatThrownBy(() -> service.getTerminTemplate(template.getId()))
        .isInstanceOf(TerminTemplateNotFoundException.class);
  }

  @Test
  void givenDeletedTemplate_whenTerminiWithoutBookedExist_thenDeletesTermini() {
    DayOfWeek day = DayOfWeek.MONDAY;
    TerminTemplateDTO template =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    LocalDate futureDate = LocalDate.now().plusDays(1);
    Termin futureTermin =
        Termin.builder()
            .date(futureDate)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .templateId(template.getId())
            .build();
    Termin savedTermin = terminRepository.save(futureTermin);

    service.deleteTerminTemplate(template.getId());

    Termin deletedTermin = terminRepository.findById(savedTermin.getId()).orElseThrow();
    assertThat(deletedTermin.getStatus()).isEqualTo(Status.DELETED);
  }

  @Test
  void givenDeletedTemplate_whenTerminWithBookedExists_thenKeepsTerminButCancelsAvailable() {
    String seed = String.valueOf(System.currentTimeMillis());
    PilatesDTO pilates1 =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("CASC1." + seed).name("Reformer").build());
    PilatesDTO pilates2 =
        pilatesService.createPilates(
            CreatePilatesRequestDTO.builder().position("CASC2." + seed).name("Cadillac").build());

    DayOfWeek day = DayOfWeek.TUESDAY;
    TerminTemplateDTO template =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

    LocalDate futureDate = LocalDate.now().plusDays(1);
    Termin futureTermin =
        Termin.builder()
            .date(futureDate)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .templateId(template.getId())
            .build();
    Termin savedTermin = terminRepository.save(futureTermin);

    Appointment availableAppointment =
        Appointment.builder()
            .terminId(savedTermin.getId())
            .pilatesId(pilates1.getId())
            .status(AppointmentStatus.AVAILABLE)
            .build();
    appointmentRepository.save(availableAppointment);

    Appointment bookedAppointment =
        Appointment.builder()
            .terminId(savedTermin.getId())
            .pilatesId(pilates2.getId())
            .userId(1L)
            .status(AppointmentStatus.BOOKED)
            .build();
    appointmentRepository.save(bookedAppointment);

    service.deleteTerminTemplate(template.getId());

    Termin terminAfterDelete = terminRepository.findById(savedTermin.getId()).orElseThrow();
    assertThat(terminAfterDelete.getStatus()).isEqualTo(Status.ACTIVE);

    List<Appointment> appointments =
        appointmentRepository.findAllByTerminIdAndStatus(
            savedTermin.getId(), AppointmentStatus.AVAILABLE);
    assertThat(appointments).isEmpty();

    List<Appointment> allAppointments =
        appointmentRepository.findAll().stream()
            .filter(a -> a.getTerminId().equals(savedTermin.getId()))
            .toList();
    assertThat(allAppointments)
        .filteredOn(a -> a.getStatus() == AppointmentStatus.CANCELED)
        .hasSize(1);
    assertThat(allAppointments)
        .filteredOn(a -> a.getStatus() == AppointmentStatus.BOOKED)
        .hasSize(1);
  }

  @Test
  void givenDeletedTemplate_whenPastTerminExists_thenLeavesPastTerminUntouched() {
    DayOfWeek day = DayOfWeek.WEDNESDAY;
    TerminTemplateDTO template =
        service.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .build());

    LocalDate pastDate = LocalDate.now().minusDays(10);
    Termin pastTermin =
        Termin.builder()
            .date(pastDate)
            .startTime(LocalTime.of(11, 0))
            .endTime(LocalTime.of(12, 0))
            .templateId(template.getId())
            .build();
    Termin savedPastTermin = terminRepository.save(pastTermin);

    service.deleteTerminTemplate(template.getId());

    Termin pastTerminAfterDelete = terminRepository.findById(savedPastTermin.getId()).orElseThrow();
    assertThat(pastTerminAfterDelete.getStatus()).isEqualTo(Status.ACTIVE);
  }
}
