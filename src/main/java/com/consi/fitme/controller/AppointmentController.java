package com.consi.fitme.controller;

import com.consi.fitme.dto.AppointmentDTO;
import com.consi.fitme.dto.request.AppointmentSearchRequestDTO;
import com.consi.fitme.dto.request.BookAppointmentRequestDTO;
import com.consi.fitme.dto.request.UpdateAppointmentRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.AppointmentService;
import com.consi.fitme.util.ApiPaths;
import com.consi.fitme.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(ApiPaths.APPOINTMENTS)
@RestController
@AllArgsConstructor
public class AppointmentController {

  private final AppointmentService service;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<List<AppointmentDTO>>> getAllAppointments(
      @ModelAttribute AppointmentSearchRequestDTO filter, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getAllAppointments(filter),
            "Appointment-i su uspešno preuzeti",
            request.getRequestURI()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<AppointmentDTO>> getAppointment(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getAppointment(id), "Appointment je uspešno preuzet", request.getRequestURI()));
  }

  @GetMapping("/available")
  @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
  public ResponseEntity<SuccessResponseDTO<List<AppointmentDTO>>> getAvailableAppointments(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getAvailableAppointments(date),
            "Dostupni appointment-i su uspešno preuzeti",
            request.getRequestURI()));
  }

  @GetMapping("/user/{userId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
  public ResponseEntity<SuccessResponseDTO<List<AppointmentDTO>>> getByUserId(
      @PathVariable Long userId, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getByUserId(userId),
            "Appointment-i korisnika su uspešno preuzeti",
            request.getRequestURI()));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
  public ResponseEntity<SuccessResponseDTO<AppointmentDTO>> bookAppointment(
      @Valid @RequestBody BookAppointmentRequestDTO bookAppointmentRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.bookAppointment(bookAppointmentRequestDTO),
            "Appointment je uspešno rezervisan",
            request.getRequestURI()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
  public ResponseEntity<SuccessResponseDTO<AppointmentDTO>> updateAppointment(
      @PathVariable Long id,
      @Valid @RequestBody UpdateAppointmentRequestDTO updateAppointmentRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.updateAppointment(id, updateAppointmentRequestDTO),
            "Appointment je uspešno ažuriran",
            request.getRequestURI()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> deleteAppointment(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.deleteAppointment(id),
            "Appointment je uspešno obrisan",
            request.getRequestURI()));
  }
}
