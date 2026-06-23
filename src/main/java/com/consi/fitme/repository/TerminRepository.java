package com.consi.fitme.repository;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Termin;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminRepository extends JpaRepository<Termin, Long> {

  List<Termin> findAllByStatusNot(Status status);

  List<Termin> findAllByStatus(Status status);

  Optional<Termin> findByIdAndStatusNot(Long id, Status status);

  List<Termin> findByDateAndStatus(LocalDate date, Status status);
}
