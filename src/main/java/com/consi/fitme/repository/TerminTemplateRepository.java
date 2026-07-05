package com.consi.fitme.repository;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.TerminTemplate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminTemplateRepository extends JpaRepository<TerminTemplate, Long> {

  List<TerminTemplate> findAllByStatusNot(Status status);

  List<TerminTemplate> findAllByStatus(Status status);

  Optional<TerminTemplate> findByIdAndStatusNot(Long id, Status status);

  List<TerminTemplate> findAllByDayOfWeekAndStatus(DayOfWeek dayOfWeek, Status status);
}
