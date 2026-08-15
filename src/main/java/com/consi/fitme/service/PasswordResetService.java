package com.consi.fitme.service;

import com.consi.fitme.exception.auth.InvalidResetTokenException;
import com.consi.fitme.exception.auth.ResetTokenCooldownException;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

  private static final int RESET_TOKEN_VALIDITY_MINUTES = 30;
  private static final int RESET_COOLDOWN_MINUTES = 2;
  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final SecureRandom random = new SecureRandom();

  private final UserRepository userRepository;
  private final UserService userService;
  private final EmailService emailService;

  @Value("${email.templates.password-reset:fitme_password_reset}")
  private String passwordResetTemplateName;

  @Value("${frontend.base-url:https://pilates.fitme.rs}")
  private String frontendBaseUrl;

  @Transactional
  public void requestReset(String email) {
    User user = userRepository.findByEmailAndStatusNot(email, Status.DELETED).orElse(null);

    if (user == null) {
      logger.info("requestReset: korisnik sa email adresom nije pronađen: {}", email);
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    if (user.getPasswordResetExpiresAt() != null
        && user.getPasswordResetExpiresAt().isAfter(now.plusMinutes(RESET_COOLDOWN_MINUTES))) {
      throw new ResetTokenCooldownException();
    }

    String rawToken = generateRawToken();
    user.setPasswordResetTokenHash(hashToken(rawToken));
    user.setPasswordResetExpiresAt(now.plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
    userRepository.save(user);

    String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
    try {
      emailService.sendTemplate(user.getEmail(), passwordResetTemplateName, List.of(resetLink));
    } catch (Exception ex) {
      logger.error(
          "Greška pri slanju linka za reset lozinke: email={}, error={}",
          email,
          ex.getMessage(),
          ex);
    }
  }

  @Transactional
  public void resetPassword(String rawToken, String newPassword) {
    String hash = hashToken(rawToken);
    User user = userRepository.findByPasswordResetTokenHash(hash).orElse(null);

    if (user == null
        || user.getPasswordResetExpiresAt() == null
        || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidResetTokenException();
    }

    userService.applyNewPassword(user, newPassword);
    logger.info("Lozinka resetovana putem linka: email={}", user.getEmail());
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algoritam nije dostupan", ex);
    }
  }
}
