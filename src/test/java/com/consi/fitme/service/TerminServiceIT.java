package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.termin.InvalidTerminTimeRangeException;
import com.consi.fitme.exception.termin.TerminNotFoundException;
import com.consi.fitme.exception.termin.TerminOverlapException;
import com.consi.fitme.model.Status;
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
class TerminServiceIT {

  @Autowired private TerminService service;

  @Test
  void givenNewTermin_whenCreated_thenStatusDefaultsToActive() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    TerminDTO createdTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    assertThat(createdTermin.getId()).isNotNull();
    assertThat(createdTermin.getDate()).isEqualTo(date);
    assertThat(createdTermin.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void givenEndTimeBeforeStartTime_whenCreateTermin_thenThrowsInvalidTerminTimeRangeException() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());
    CreateTerminRequestDTO invalidRangeRequest =
        CreateTerminRequestDTO.builder()
            .date(date)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(9, 0))
            .build();

    assertThatThrownBy(() -> service.createTermin(invalidRangeRequest))
        .isInstanceOf(InvalidTerminTimeRangeException.class);
  }

  @Test
  void givenOverlappingTermin_whenCreateTermin_thenThrowsTerminOverlapException() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    service.createTermin(
        CreateTerminRequestDTO.builder()
            .date(date)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    CreateTerminRequestDTO overlappingRequest =
        CreateTerminRequestDTO.builder()
            .date(date)
            .startTime(LocalTime.of(9, 30))
            .endTime(LocalTime.of(10, 30))
            .build();

    assertThatThrownBy(() -> service.createTermin(overlappingRequest))
        .isInstanceOf(TerminOverlapException.class);
  }

  @Test
  void givenNonOverlappingTermin_whenCreateTermin_thenSucceeds() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    service.createTermin(
        CreateTerminRequestDTO.builder()
            .date(date)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    TerminDTO secondTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

    assertThat(secondTermin.getId()).isNotNull();
  }

  @Test
  void givenDeletedOverlappingTermin_whenCreateTermin_thenSucceeds() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    TerminDTO firstTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    service.deleteTermin(firstTermin.getId());

    TerminDTO secondTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .build());

    assertThat(secondTermin.getId()).isNotNull();
  }

  @Test
  void
      givenExistingTermin_whenUpdateWithOverlappingAnotherTermin_thenThrowsTerminOverlapException() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    service.createTermin(
        CreateTerminRequestDTO.builder()
            .date(date)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build());

    TerminDTO secondTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .build());

    Long secondTerminId = secondTermin.getId();
    UpdateTerminRequestDTO overlappingUpdateRequest =
        UpdateTerminRequestDTO.builder().startTime(LocalTime.of(9, 30)).build();

    assertThatThrownBy(() -> service.updateTermin(secondTerminId, overlappingUpdateRequest))
        .isInstanceOf(TerminOverlapException.class);
  }

  @Test
  void givenExistingTermin_whenUpdateWithItsOwnUnchangedTimes_thenSucceeds() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    TerminDTO createdTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    TerminDTO updatedTermin =
        service.updateTermin(
            createdTermin.getId(),
            UpdateTerminRequestDTO.builder().status(Status.INACTIVE).build());

    assertThat(updatedTermin.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(updatedTermin.getStartTime()).isEqualTo(LocalTime.of(9, 0));
  }

  @Test
  void givenNonExistingTerminId_whenGetTermin_thenThrowsTerminNotFoundException() {
    assertThatThrownBy(() -> service.getTermin(Long.MAX_VALUE))
        .isInstanceOf(TerminNotFoundException.class);
  }

  @Test
  void givenExistingTermin_whenDeleteTermin_thenPerformsSoftDeleteAndIsNotAccessible() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    TerminDTO createdTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    MessageResponseDTO response = service.deleteTermin(createdTermin.getId());

    assertThat(response.getMessage()).contains("Uspešno obrisan termin");
    Long deletedTerminId = createdTermin.getId();
    assertThatThrownBy(() -> service.getTermin(deletedTerminId))
        .isInstanceOf(TerminNotFoundException.class);
  }

  @Test
  void givenDeletedTermin_whenGetAllTermini_thenExcludesDeletedTermin() {
    LocalDate date = LocalDate.now().plusDays(uniqueDayOffset());

    TerminDTO activeTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());
    TerminDTO toDeleteTermin =
        service.createTermin(
            CreateTerminRequestDTO.builder()
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

    service.deleteTermin(toDeleteTermin.getId());

    List<TerminDTO> allTermini = service.getAllTermini();

    assertThat(allTermini).extracting(TerminDTO::getId).contains(activeTermin.getId());
    assertThat(allTermini).extracting(TerminDTO::getId).doesNotContain(toDeleteTermin.getId());
  }

  private long uniqueDayOffset() {
    return System.nanoTime() % 100000;
  }
}
