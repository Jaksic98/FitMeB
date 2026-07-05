package com.consi.fitme.scheduler;

import com.consi.fitme.service.AppointmentReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

  private final AppointmentReminderService appointmentReminderService;

  @Scheduled(cron = "0 */5 * * * *")
  public void sendDueReminders() {
    log.info("Započeto slanje dospelih podsetnika");
    appointmentReminderService.sendDueReminders();
    log.info("Završeno slanje dospelih podsetnika");
  }
}
