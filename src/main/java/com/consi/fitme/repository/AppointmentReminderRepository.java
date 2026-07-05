package com.consi.fitme.repository;

import com.consi.fitme.model.entity.AppointmentReminder;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {

  List<AppointmentReminder> findAllByAppointmentIdAndSentAtIsNull(Long appointmentId);

  List<AppointmentReminder> findAllByScheduledAtLessThanEqualAndSentAtIsNull(LocalDateTime now);
}
