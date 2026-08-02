package com.consi.fitme.service;

import com.consi.fitme.dto.NewsletterTemplateDTO;
import com.consi.fitme.dto.request.CreateNewsletterTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateNewsletterTemplateRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.newslettertemplate.NewsletterTemplateNotFoundException;
import com.consi.fitme.mapper.NewsletterTemplatePatchMapper;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.NewsletterTemplate;
import com.consi.fitme.repository.NewsletterTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsletterTemplateService {

  private final NewsletterTemplateRepository repository;
  private final NewsletterTemplatePatchMapper patchMapper;

  public List<NewsletterTemplateDTO> getAllTemplates() {
    return repository.findAllByStatusNot(Status.DELETED).stream().map(this::toDto).toList();
  }

  public NewsletterTemplateDTO getTemplate(Long id) {
    return toDto(findActiveOrInactiveById(id));
  }

  @Transactional
  public NewsletterTemplateDTO createTemplate(
      CreateNewsletterTemplateRequestDTO createNewsletterTemplateRequestDTO) {
    NewsletterTemplate template =
        NewsletterTemplate.builder()
            .title(createNewsletterTemplateRequestDTO.getTitle())
            .description(createNewsletterTemplateRequestDTO.getDescription())
            .htmlContent(createNewsletterTemplateRequestDTO.getHtmlContent())
            .defaultSubject(createNewsletterTemplateRequestDTO.getDefaultSubject())
            .build();
    return toDto(repository.save(template));
  }

  @Transactional
  public NewsletterTemplateDTO updateTemplate(
      Long id, UpdateNewsletterTemplateRequestDTO updateNewsletterTemplateRequestDTO) {
    NewsletterTemplate existingTemplate = findActiveOrInactiveById(id);
    patchMapper.applyPatch(updateNewsletterTemplateRequestDTO, existingTemplate);
    return toDto(repository.save(existingTemplate));
  }

  @Transactional
  public MessageResponseDTO deleteTemplate(Long id) {
    NewsletterTemplate template = findActiveOrInactiveById(id);
    template.setStatus(Status.DELETED);
    repository.save(template);
    return new MessageResponseDTO("Uspešno obrisan šablon newsletter-a za ID: " + id);
  }

  private NewsletterTemplate findActiveOrInactiveById(Long id) {
    return repository
        .findByIdAndStatusNot(id, Status.DELETED)
        .orElseThrow(() -> new NewsletterTemplateNotFoundException(id));
  }

  private NewsletterTemplateDTO toDto(NewsletterTemplate template) {
    return NewsletterTemplateDTO.builder()
        .id(template.getId())
        .title(template.getTitle())
        .description(template.getDescription())
        .htmlContent(template.getHtmlContent())
        .defaultSubject(template.getDefaultSubject())
        .status(template.getStatus())
        .build();
  }
}
