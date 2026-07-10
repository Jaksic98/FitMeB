package com.consi.fitme.repository;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Termin;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminRepository extends JpaRepository<Termin, Long> {

  List<Termin> findAllByStatusNot(Status status);

  Page<Termin> findAllByStatusNot(Status status, Pageable pageable);

  Page<Termin> findAllByStatusNotAndDate(Status status, LocalDate date, Pageable pageable);

  List<Termin> findAllByStatus(Status status);

  Optional<Termin> findByIdAndStatusNot(Long id, Status status);

  List<Termin> findByDateAndStatus(LocalDate date, Status status);

  boolean existsByTemplateIdAndDate(Long templateId, LocalDate date);

  List<Termin> findAllByTemplateIdAndDateGreaterThanEqualAndStatusNot(
      Long templateId, LocalDate date, Status status);
}
