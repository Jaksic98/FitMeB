package com.consi.fitme.controller;

import com.consi.fitme.dto.NewsletterTemplateDTO;
import com.consi.fitme.dto.request.CreateNewsletterTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateNewsletterTemplateRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.NewsletterTemplateService;
import com.consi.fitme.util.ApiPaths;
import com.consi.fitme.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(ApiPaths.NEWSLETTER_TEMPLATES)
@RestController
@AllArgsConstructor
public class NewsletterTemplateController {

  private final NewsletterTemplateService service;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<List<NewsletterTemplateDTO>>> getAllTemplates(
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getAllTemplates(),
            "Šabloni newsletter-a su uspešno preuzeti",
            request.getRequestURI()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<NewsletterTemplateDTO>> getTemplate(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getTemplate(id),
            "Šablon newsletter-a je uspešno preuzet",
            request.getRequestURI()));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<NewsletterTemplateDTO>> createTemplate(
      @Valid @RequestBody CreateNewsletterTemplateRequestDTO createNewsletterTemplateRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.createTemplate(createNewsletterTemplateRequestDTO),
            "Šablon newsletter-a je uspešno kreiran",
            request.getRequestURI()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<NewsletterTemplateDTO>> updateTemplate(
      @PathVariable Long id,
      @Valid @RequestBody UpdateNewsletterTemplateRequestDTO updateNewsletterTemplateRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.updateTemplate(id, updateNewsletterTemplateRequestDTO),
            "Šablon newsletter-a je uspešno ažuriran",
            request.getRequestURI()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> deleteTemplate(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.deleteTemplate(id),
            "Šablon newsletter-a je uspešno obrisan",
            request.getRequestURI()));
  }
}
