package com.consi.fitme.repository;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Pilates;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PilatesRepository extends JpaRepository<Pilates, Long> {

  List<Pilates> findAllByStatusNot(Status status);

  List<Pilates> findAllByStatus(Status status);

  Optional<Pilates> findByIdAndStatusNot(Long id, Status status);
}
