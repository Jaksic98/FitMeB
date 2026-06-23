package com.consi.fitme.controller;

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.UpdatePilatesRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.PilatesService;
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

@RequestMapping(ApiPaths.PILATES)
@RestController
@AllArgsConstructor
public class PilatesController {

  private final PilatesService service;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<List<PilatesDTO>>> getAllPilates(
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getAllPilates(), "Sprave su uspešno preuzete", request.getRequestURI()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<PilatesDTO>> getPilates(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getPilates(id), "Sprava je uspešno preuzeta", request.getRequestURI()));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<PilatesDTO>> createPilates(
      @Valid @RequestBody CreatePilatesRequestDTO createPilatesRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.createPilates(createPilatesRequestDTO),
            "Sprava je uspešno kreirana",
            request.getRequestURI()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<PilatesDTO>> updatePilates(
      @PathVariable Long id,
      @Valid @RequestBody UpdatePilatesRequestDTO updatePilatesRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.updatePilates(id, updatePilatesRequestDTO),
            "Sprava je uspešno ažurirana",
            request.getRequestURI()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> deletePilates(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.deletePilates(id), "Sprava je uspešno obrisana", request.getRequestURI()));
  }
}
