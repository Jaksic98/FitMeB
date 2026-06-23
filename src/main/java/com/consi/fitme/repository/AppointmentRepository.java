package com.consi.fitme.repository;

import com.consi.fitme.model.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  boolean existsByTerminIdAndPilatesId(Long terminId, Long pilatesId);
}
