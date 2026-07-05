package com.consi.fitme.model.entity;

import com.consi.fitme.model.ReminderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointment_reminder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentReminder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(updatable = false, nullable = false)
  private Long id;

  @Column(name = "appointment_id", nullable = false)
  private Long appointmentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReminderType type;

  @Column(name = "scheduled_at", nullable = false)
  private LocalDateTime scheduledAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;
}
