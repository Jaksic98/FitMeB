package com.consi.fitme.scheduler;

import com.consi.fitme.service.TerminGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TerminScheduler {

  private final TerminGenerationService terminGenerationService;

  @Scheduled(cron = "0 0 2 * * *")
  public void generateTerminiFromTemplates() {
    log.info("Započeta generisanja termina iz šablona");
    terminGenerationService.generateForAllActiveTemplates();
    log.info("Završena generisanja termina iz šablona");
  }
}
