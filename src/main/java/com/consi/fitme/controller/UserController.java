package com.consi.fitme.controller;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.dto.request.UserSearchRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.PagingResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.UserService;
import com.consi.fitme.util.ApiPaths;
import com.consi.fitme.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(ApiPaths.USERS)
@RestController
@AllArgsConstructor
public class UserController {

  private final UserService service;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<PagingResponseDTO<UserDTO>>> getUsers(
      @Valid @ModelAttribute UserSearchRequestDTO searchRequest, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getUsers(searchRequest),
            "Korisnici su uspešno preuzeti",
            request.getRequestURI()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<UserDTO>> getUser(
      @PathVariable Long id, HttpServletRequest request) {

    return ResponseEntity.ok(
        ResponseUtil.success(
            service.getUser(id), "Korisnik je uspešno preuzet", request.getRequestURI()));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<UserDTO>> createUser(
      @Valid @RequestBody CreateUserRequestDTO createUserRequestDTO, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.createUser(createUserRequestDTO),
            "Korisnik je uspešno kreiran",
            request.getRequestURI()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<UserDTO>> updateUser(
      @PathVariable Long id,
      @Valid @RequestBody UpdateUserRequestDTO updateUserRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.updateUser(id, updateUserRequestDTO),
            "Korisnik je uspešno ažuriran",
            request.getRequestURI()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> deleteUser(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.deleteUser(id), "Korisnik je uspešno obrisan", request.getRequestURI()));
  }
}
