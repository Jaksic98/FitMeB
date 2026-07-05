package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.TerminTemplateDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.dto.request.CreateTerminTemplateRequestDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.repository.TerminRepository;
import com.consi.fitme.repository.TerminTemplateRepository;
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
class TerminGenerationServiceIT {

  @Autowired private TerminGenerationService terminGenerationService;
  @Autowired private TerminTemplateService terminTemplateService;
  @Autowired private TerminService terminService;
  @Autowired private PilatesService pilatesService;
  @Autowired private TerminRepository terminRepository;
  @Autowired private TerminTemplateRepository terminTemplateRepository;

  @Test
  void givenActiveTemplate_whenGenerateForTemplate_thenCreatesTerminiInHorizon() {
    DayOfWeek targetDay = DayOfWeek.MONDAY;
    TerminTemplateDTO template =
        terminTemplateService.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(targetDay)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build());

    LocalDate today = LocalDate.now();
    LocalDate endDate = today.plusDays(90);
    int expectedCount = countOccurrencesOfDayOfWeekInRange(targetDay, today, endDate);

    terminGenerationService.generateForTemplate(template.getId());

    List<com.consi.fitme.model.entity.Termin> generatedTermini =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
            .toList();

    assertThat(generatedTermini).hasSize(expectedCount);
    generatedTermini.forEach(
        termin -> {
          assertThat(termin.getDate().getDayOfWeek()).isEqualTo(targetDay);
          assertThat(termin.getStartTime()).isEqualTo(LocalTime.of(9, 0));
          assertThat(termin.getEndTime()).isEqualTo(LocalTime.of(10, 0));
          assertThat(termin.getStatus()).isEqualTo(Status.ACTIVE);
        });
  }

  @Test
  void givenActiveTemplateWithActivePilates_whenGenerate_thenCreatesAppointmentSlots() {
    String seed = String.valueOf(System.currentTimeMillis());
    pilatesService.createPilates(
        CreatePilatesRequestDTO.builder().position("GEN1." + seed).name("Reformer").build());

    DayOfWeek targetDay = DayOfWeek.TUESDAY;
    TerminTemplateDTO template =
        terminTemplateService.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(targetDay)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .build());

    terminGenerationService.generateForTemplate(template.getId());

    List<com.consi.fitme.model.entity.Termin> generatedTermini =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
            .toList();

    assertThat(generatedTermini).isNotEmpty();
    generatedTermini.forEach(
        termin ->
            assertThat(terminRepository.findById(termin.getId()).orElseThrow().getTemplateId())
                .isNotNull());
  }

  @Test
  void givenGeneratedTermini_whenGenerateCalledAgain_thenNoDuplicatesCreated() {
    DayOfWeek targetDay = DayOfWeek.WEDNESDAY;
    TerminTemplateDTO template =
        terminTemplateService.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(targetDay)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .build());

    terminGenerationService.generateForTemplate(template.getId());
    int countAfterFirst =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
            .toList()
            .size();

    terminGenerationService.generateForTemplate(template.getId());
    int countAfterSecond =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
            .toList()
            .size();

    assertThat(countAfterFirst).isEqualTo(countAfterSecond);
  }

  @Test
  void givenManuallyCreatedTerminOnGeneratedDate_whenGenerate_thenSkipsDate() {
    DayOfWeek targetDay = DayOfWeek.THURSDAY;
    TerminTemplateDTO template =
        terminTemplateService.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(targetDay)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

    LocalDate today = LocalDate.now();
    LocalDate nextTargetDate =
        today.plusDays((7 - today.getDayOfWeek().getValue() + targetDay.getValue() - 1) % 7 + 1);

    TerminDTO manualTermin =
        terminService.createTermin(
            CreateTerminRequestDTO.builder()
                .date(nextTargetDate)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .build());

    terminGenerationService.generateForTemplate(template.getId());

    List<com.consi.fitme.model.entity.Termin> onDate =
        terminRepository.findAll().stream()
            .filter(t -> t.getDate().equals(nextTargetDate))
            .toList();

    assertThat(onDate).hasSize(2);
    assertThat(onDate)
        .extracting(com.consi.fitme.model.entity.Termin::getId)
        .contains(manualTermin.getId());
    assertThat(onDate)
        .filteredOn(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
        .hasSize(1);
  }

  @Test
  void givenDeletedGeneratedTermin_whenGenerateCalledAgain_thenDoesNotRegenerate() {
    DayOfWeek targetDay = DayOfWeek.FRIDAY;
    TerminTemplateDTO template =
        terminTemplateService.createTerminTemplate(
            CreateTerminTemplateRequestDTO.builder()
                .dayOfWeek(targetDay)
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(17, 0))
                .build());

    terminGenerationService.generateForTemplate(template.getId());

    List<com.consi.fitme.model.entity.Termin> generatedBefore =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
            .filter(t -> t.getStatus() != Status.DELETED)
            .toList();

    generatedBefore.forEach(
        termin -> {
          termin.setStatus(Status.DELETED);
          terminRepository.save(termin);
        });

    terminGenerationService.generateForTemplate(template.getId());

    List<com.consi.fitme.model.entity.Termin> generatedAfter =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(template.getId()))
            .filter(t -> t.getStatus() != Status.DELETED)
            .toList();

    assertThat(generatedAfter).isEmpty();
  }

  @Test
  void givenMixedActiveAndInactiveTemplates_whenGenerateForAll_thenOnlyGeneratesForActive() {
    com.consi.fitme.model.entity.TerminTemplate tempActive =
        com.consi.fitme.model.entity.TerminTemplate.builder()
            .dayOfWeek(DayOfWeek.SATURDAY)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build();
    com.consi.fitme.model.entity.TerminTemplate savedActive =
        terminTemplateRepository.save(tempActive);

    com.consi.fitme.model.entity.TerminTemplate tempInactive =
        com.consi.fitme.model.entity.TerminTemplate.builder()
            .dayOfWeek(DayOfWeek.SUNDAY)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build();
    com.consi.fitme.model.entity.TerminTemplate savedInactive =
        terminTemplateRepository.save(tempInactive);
    savedInactive.setStatus(Status.INACTIVE);
    terminTemplateRepository.save(savedInactive);

    terminGenerationService.generateForAllActiveTemplates();

    List<com.consi.fitme.model.entity.Termin> forActive =
        terminRepository.findAll().stream()
            .filter(t -> t.getTemplateId() != null && t.getTemplateId().equals(savedActive.getId()))
            .toList();

    List<com.consi.fitme.model.entity.Termin> forInactive =
        terminRepository.findAll().stream()
            .filter(
                t -> t.getTemplateId() != null && t.getTemplateId().equals(savedInactive.getId()))
            .toList();

    assertThat(forActive).isNotEmpty();
    assertThat(forInactive).isEmpty();
  }

  private int countOccurrencesOfDayOfWeekInRange(
      DayOfWeek dayOfWeek, LocalDate startDate, LocalDate endDate) {
    int count = 0;
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (date.getDayOfWeek() == dayOfWeek) {
        count++;
      }
    }
    return count;
  }
}
