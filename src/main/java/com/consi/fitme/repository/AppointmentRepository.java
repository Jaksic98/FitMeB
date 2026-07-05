package com.consi.fitme.repository;

import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.model.entity.Appointment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  boolean existsByTerminIdAndPilatesId(Long terminId, Long pilatesId);

  List<Appointment> findAllByUserId(Long userId);

  List<Appointment> findAllByStatus(AppointmentStatus status);

  boolean existsByTerminIdAndStatus(Long terminId, AppointmentStatus status);

  List<Appointment> findAllByTerminIdAndStatus(Long terminId, AppointmentStatus status);
}
