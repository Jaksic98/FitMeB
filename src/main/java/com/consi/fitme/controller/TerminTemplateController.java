package com.consi.fitme.controller;

import com.consi.fitme.dto.TerminTemplateDTO;
import com.consi.fitme.dto.request.CreateTerminTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminTemplateRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.TerminTemplateService;
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

@RequestMapping(ApiPaths.TERMIN_TEMPLATES)
@RestController
@AllArgsConstructor
public class TerminTemplateController {

  private final TerminTemplateService service;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<List<TerminTemplateDTO>>> getAllTerminTemplates(
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getAllTerminTemplates(),
            "Šabloni termina su uspešno preuzeti",
            request.getRequestURI()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<TerminTemplateDTO>> getTerminTemplate(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getTerminTemplate(id),
            "Šablon termina je uspešno preuzet",
            request.getRequestURI()));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<TerminTemplateDTO>> createTerminTemplate(
      @Valid @RequestBody CreateTerminTemplateRequestDTO createTerminTemplateRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.createTerminTemplate(createTerminTemplateRequestDTO),
            "Šablon termina je uspešno kreiran",
            request.getRequestURI()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<TerminTemplateDTO>> updateTerminTemplate(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTerminTemplateRequestDTO updateTerminTemplateRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.updateTerminTemplate(id, updateTerminTemplateRequestDTO),
            "Šablon termina je uspešno ažuriran",
            request.getRequestURI()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> deleteTerminTemplate(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.deleteTerminTemplate(id),
            "Šablon termina je uspešno obrisan",
            request.getRequestURI()));
  }
}
