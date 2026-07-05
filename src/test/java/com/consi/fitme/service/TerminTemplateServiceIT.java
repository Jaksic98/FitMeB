package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.TerminTemplateDTO;
import com.consi.fitme.dto.request.CreateTerminTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminTemplateRequestDTO;
import com.consi.fitme.exception.termintemplate.InvalidTerminTemplateTimeRangeException;
import com.consi.fitme.exception.termintemplate.TerminTemplateNotFoundException;
import com.consi.fitme.exception.termintemplate.TerminTemplateOverlapException;
import com.consi.fitme.model.Status;
import java.time.DayOfWeek;
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
}
