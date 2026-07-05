package com.consi.fitme.controller;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.LoginRequestDTO;
import com.consi.fitme.dto.request.RegisterRequestDTO;
import com.consi.fitme.dto.request.SendOtpRequestDTO;
import com.consi.fitme.dto.request.VerifyOtpRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.AuthService;
import com.consi.fitme.service.PhoneVerificationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthService service;
  private final PhoneVerificationService phoneVerificationService;

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

  @PostMapping("/register")
  public ResponseEntity<SuccessResponseDTO<UserDTO>> register(
      @Valid @RequestBody RegisterRequestDTO registerRequestDTO, HttpServletRequest request) {

    return ResponseEntity.ok(
        ResponseUtil.success(
            service.register(registerRequestDTO),
            "Registracija je uspešna, proverite email radi aktivacije naloga",
            request.getRequestURI()));
  }

  @GetMapping("/activate")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> activate(
      @RequestParam String token, HttpServletRequest request) {

    service.activate(token);
    return ResponseEntity.ok(
        ResponseUtil.success(
            new MessageResponseDTO("Nalog je aktiviran"),
            "Nalog je aktiviran",
            request.getRequestURI()));
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

  @PostMapping("/phone/send-otp")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> sendOtp(
      @Valid @RequestBody SendOtpRequestDTO sendOtpRequestDTO, HttpServletRequest request) {

    phoneVerificationService.sendOtp(sendOtpRequestDTO.getPhoneNumber());
    return ResponseEntity.ok(
        ResponseUtil.success(
            new MessageResponseDTO("Kod za verifikaciju je poslat"),
            "Kod za verifikaciju je poslat",
            request.getRequestURI()));
  }

  @PostMapping("/phone/verify-otp")
  public ResponseEntity<SuccessResponseDTO<MessageResponseDTO>> verifyOtp(
      @Valid @RequestBody VerifyOtpRequestDTO verifyOtpRequestDTO, HttpServletRequest request) {

    phoneVerificationService.verifyOtp(
        verifyOtpRequestDTO.getPhoneNumber(), verifyOtpRequestDTO.getCode());
    return ResponseEntity.ok(
        ResponseUtil.success(
            new MessageResponseDTO("Broj telefona je uspešno verifikovan"),
            "Broj telefona je uspešno verifikovan",
            request.getRequestURI()));
  }
}
