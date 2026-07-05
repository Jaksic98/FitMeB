package com.consi.fitme.service;

import com.consi.fitme.exception.termin.TerminOverlapException;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Termin;
import com.consi.fitme.model.entity.TerminTemplate;
import com.consi.fitme.repository.TerminRepository;
import com.consi.fitme.repository.TerminTemplateRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminGenerationService {

  private final TerminRepository terminRepository;
  private final TerminTemplateRepository terminTemplateRepository;
  private final TerminService terminService;
  private final AppointmentGenerationService appointmentGenerationService;

  @Value("${application.scheduling.termin-horizon-days:90}")
  private int horizonDays;

  @Transactional
  public void generateForTemplate(Long templateId) {
    TerminTemplate template =
        terminTemplateRepository.findByIdAndStatusNot(templateId, Status.DELETED).orElse(null);

    if (template == null || template.getStatus() != Status.ACTIVE) {
      return;
    }

    LocalDate today = LocalDate.now();
    LocalDate endDate = today.plusDays(horizonDays);

    for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (date.getDayOfWeek() != template.getDayOfWeek()) {
        continue;
      }

      if (terminRepository.existsByTemplateIdAndDate(templateId, date)) {
        continue;
      }

      try {
        Termin createdTermin = terminService.createTerminFromTemplate(template, date);
        appointmentGenerationService.generateForTermin(createdTermin.getId());
      } catch (TerminOverlapException e) {
        log.warn("Termin overlap for template {} on date {}: {}", templateId, date, e.getMessage());
      }
    }
  }

  @Transactional
  public void generateForAllActiveTemplates() {
    terminTemplateRepository
        .findAllByStatus(Status.ACTIVE)
        .forEach(template -> generateForTemplate(template.getId()));
  }
}
