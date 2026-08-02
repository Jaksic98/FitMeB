package com.consi.fitme.repository;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.NewsletterTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsletterTemplateRepository extends JpaRepository<NewsletterTemplate, Long> {

  List<NewsletterTemplate> findAllByStatusNot(Status status);

  Optional<NewsletterTemplate> findByIdAndStatusNot(Long id, Status status);
}
