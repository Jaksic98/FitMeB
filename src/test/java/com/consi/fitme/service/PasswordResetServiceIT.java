package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.exception.auth.InvalidResetTokenException;
import com.consi.fitme.exception.auth.ResetTokenCooldownException;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class PasswordResetServiceIT {

  @Autowired private PasswordResetService passwordResetService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private EmailService emailService;

  @Test
  void givenExistingUser_whenRequestReset_thenSetsTokenHashAndSendsResetLink() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.reset.send." + seed + "@fitme.com";
    createUser(seed, email);

    passwordResetService.requestReset(email);

    User user = userRepository.findByEmailAndStatusNot(email, Status.DELETED).orElseThrow();
    assertThat(user.getPasswordResetTokenHash()).isNotNull();
    assertThat(user.getPasswordResetExpiresAt()).isNotNull();

    ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> placeholdersCaptor = ArgumentCaptor.captor();
    verify(emailService)
        .sendTemplate(eq(email), templateNameCaptor.capture(), placeholdersCaptor.capture());

    assertThat(templateNameCaptor.getValue()).isEqualTo("fitme_password_reset");
    assertThat(placeholdersCaptor.getValue()).hasSize(1);
    assertThat(placeholdersCaptor.getValue().getFirst()).contains("/reset-password?token=");
  }

  @Test
  void givenValidToken_whenResetPassword_thenChangesPasswordAndClearsToken() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.reset.valid." + seed + "@fitme.com";
    createUser(seed, email);

    passwordResetService.requestReset(email);
    String rawToken = captureRawToken(email);
    String newPassword = "itest.reset.valid.newpass456!";

    passwordResetService.resetPassword(rawToken, newPassword);

    User user = userRepository.findByEmailAndStatusNot(email, Status.DELETED).orElseThrow();
    assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
    assertThat(user.getPasswordResetTokenHash()).isNull();
    assertThat(user.getPasswordResetExpiresAt()).isNull();
    assertThat(user.getPasswordChangedAt()).isNotNull();

    assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "irrelevant999!"))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void givenNonexistentEmail_whenRequestReset_thenDoesNotThrowAndDoesNotSend() {
    String nonexistentEmail = "itest.reset.nonexistent." + System.nanoTime() + "@fitme.com";

    passwordResetService.requestReset(nonexistentEmail);

    verify(emailService, never()).sendTemplate(any(), any(), Collections.singletonList(any()));
  }

  @Test
  void givenRecentResetRequest_whenRequestResetAgain_thenThrowsResetTokenCooldownException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.reset.cooldown." + seed + "@fitme.com";
    createUser(seed, email);

    passwordResetService.requestReset(email);

    assertThatThrownBy(() -> passwordResetService.requestReset(email))
        .isInstanceOf(ResetTokenCooldownException.class);
  }

  @Test
  void givenExpiredToken_whenResetPassword_thenThrowsInvalidResetTokenException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.reset.expired." + seed + "@fitme.com";
    createUser(seed, email);

    passwordResetService.requestReset(email);
    String rawToken = captureRawToken(email);

    User user = userRepository.findByEmailAndStatusNot(email, Status.DELETED).orElseThrow();
    user.setPasswordResetExpiresAt(LocalDateTime.now().minusMinutes(1));
    userRepository.save(user);

    assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "irrelevant999!"))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void givenGarbageToken_whenResetPassword_thenThrowsInvalidResetTokenException() {
    assertThatThrownBy(
            () -> passwordResetService.resetPassword("not-a-real-token", "irrelevant999!"))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  private void createUser(String seed, String email) {
    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.reset.user." + seed)
            .fullName("Integration Reset User")
            .email(email)
            .phoneNumber("+381601" + seed.substring(seed.length() - 6))
            .password("itest.reset.fitme123!")
            .build());
  }

  private String captureRawToken(String email) {
    ArgumentCaptor<List<String>> placeholdersCaptor = ArgumentCaptor.captor();
    verify(emailService).sendTemplate(eq(email), any(), placeholdersCaptor.capture());
    String resetLink = placeholdersCaptor.getValue().getFirst();
    return resetLink.substring(resetLink.indexOf("token=") + "token=".length());
  }
}
