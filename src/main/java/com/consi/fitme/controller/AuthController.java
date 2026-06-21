package com.consi.fitme.controller;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.LoginRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.AuthService;
import com.consi.fitme.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthService service;

  @PostMapping("/login")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> login(
      @Valid @RequestBody LoginRequestDTO loginRequestDTO,
      HttpServletRequest request,
      HttpServletResponse response) {

    service.login(loginRequestDTO.getEmail(), loginRequestDTO.getPassword(), response);
    return ResponseEntity.ok(
        ResponseUtil.success(
            new MessageResponseDTO("Uspešna prijava"), "Uspešna prijava", request.getRequestURI()));
  }

  @PostMapping("/logout")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> logout(
      HttpServletRequest request, HttpServletResponse response) {

    service.logout(request, response);
    return ResponseEntity.ok(
        ResponseUtil.success(
            new MessageResponseDTO("Uspešna odjava"), "Uspešna odjava", request.getRequestURI()));
  }

  @GetMapping("/me")
  public ResponseEntity<SuccessResponseDTO<UserDTO>> me(HttpServletRequest request) {

    return ResponseEntity.ok(
        ResponseUtil.success(
            service.me(request), "Korisnik je identifikovan", request.getRequestURI()));
  }

  @GetMapping("/validate-session")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> validateSession(
      HttpServletRequest request) {

    service.validateSession(request);
    return ResponseEntity.ok(
        ResponseUtil.success(
            new MessageResponseDTO("Sesija je aktivna"),
            "Sesija je aktivna",
            request.getRequestURI()));
  }
}
