package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.exception.auth.InvalidOtpException;
import com.consi.fitme.exception.auth.OtpResendCooldownException;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.time.LocalDateTime;
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
class PhoneVerificationServiceIT {

  @Autowired private PhoneVerificationService phoneVerificationService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private WhatsAppSender whatsAppSender;

  @Test
  void givenInactiveUser_whenSendOtp_thenSetsOtpHashAndExpiresAtAndSendsTemplate() {
    String seed = String.valueOf(System.currentTimeMillis());
    String phoneNumber = "+381601" + seed.substring(seed.length() - 6);
    String email = "itest.otp.send." + seed + "@fitme.com";

    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.otp.send." + seed)
            .fullName("Integration OTP Send User")
            .email(email)
            .phoneNumber(phoneNumber)
            .password("itest.otp.send.fitme123!")
            .build());

    phoneVerificationService.sendOtp(phoneNumber);

    User user =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElseThrow();
    assertThat(user.getOtpHash()).isNotNull();
    assertThat(user.getOtpExpiresAt()).isNotNull();

    ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<java.util.List> placeholdersCaptor =
        ArgumentCaptor.forClass(java.util.List.class);
    verify(whatsAppSender)
        .sendTemplate(eq(phoneNumber), templateNameCaptor.capture(), placeholdersCaptor.capture());

    assertThat(templateNameCaptor.getValue()).isEqualTo("fitme_otp");
    assertThat(placeholdersCaptor.getValue()).hasSize(1);
    String code = (String) placeholdersCaptor.getValue().get(0);
    assertThat(code).matches("\\d{6}");
    assertThat(passwordEncoder.matches(code, user.getOtpHash())).isTrue();
  }

  @Test
  void givenValidOtp_whenVerifyOtp_thenActivatesInactiveUserAndClearsOtp() {
    String seed = String.valueOf(System.currentTimeMillis());
    String phoneNumber = "+381601" + seed.substring(seed.length() - 6);
    String email = "itest.otp.verify." + seed + "@fitme.com";

    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.otp.verify." + seed)
            .fullName("Integration OTP Verify User")
            .email(email)
            .phoneNumber(phoneNumber)
            .password("itest.otp.verify.fitme123!")
            .build());

    phoneVerificationService.sendOtp(phoneNumber);
    User userAfterSend =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElseThrow();
    String otpHash = userAfterSend.getOtpHash();

    ArgumentCaptor<java.util.List> placeholdersCaptor =
        ArgumentCaptor.forClass(java.util.List.class);
    verify(whatsAppSender).sendTemplate(eq(phoneNumber), any(), placeholdersCaptor.capture());
    String code = (String) placeholdersCaptor.getValue().get(0);

    phoneVerificationService.verifyOtp(phoneNumber, code);

    User userAfterVerify =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElseThrow();
    assertThat(userAfterVerify.getPhoneVerified()).isTrue();
    assertThat(userAfterVerify.getOtpHash()).isNull();
    assertThat(userAfterVerify.getOtpExpiresAt()).isNull();
    assertThat(userAfterVerify.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(userAfterVerify.getFailedLoginAttempts()).isZero();
  }

  @Test
  void givenWrongCode_whenVerifyOtp_thenThrowsInvalidOtpException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String phoneNumber = "+381601" + seed.substring(seed.length() - 6);
    String email = "itest.otp.wrong." + seed + "@fitme.com";

    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.otp.wrong." + seed)
            .fullName("Integration OTP Wrong User")
            .email(email)
            .phoneNumber(phoneNumber)
            .password("itest.otp.wrong.fitme123!")
            .build());

    phoneVerificationService.sendOtp(phoneNumber);

    assertThatThrownBy(() -> phoneVerificationService.verifyOtp(phoneNumber, "999999"))
        .isInstanceOf(InvalidOtpException.class);
  }

  @Test
  void givenExpiredOtp_whenVerifyOtp_thenThrowsInvalidOtpException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String phoneNumber = "+381601" + seed.substring(seed.length() - 6);
    String email = "itest.otp.expired." + seed + "@fitme.com";

    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.otp.expired." + seed)
            .fullName("Integration OTP Expired User")
            .email(email)
            .phoneNumber(phoneNumber)
            .password("itest.otp.expired.fitme123!")
            .build());

    phoneVerificationService.sendOtp(phoneNumber);

    User user =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElseThrow();
    user.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
    userRepository.save(user);

    assertThatThrownBy(() -> phoneVerificationService.verifyOtp(phoneNumber, "123456"))
        .isInstanceOf(InvalidOtpException.class);
  }

  @Test
  void givenActiveSendOtpWithinCooldown_whenSendOtpAgain_thenThrowsOtpResendCooldownException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String phoneNumber = "+381601" + seed.substring(seed.length() - 6);
    String email = "itest.otp.cooldown." + seed + "@fitme.com";

    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.otp.cooldown." + seed)
            .fullName("Integration OTP Cooldown User")
            .email(email)
            .phoneNumber(phoneNumber)
            .password("itest.otp.cooldown.fitme123!")
            .build());

    phoneVerificationService.sendOtp(phoneNumber);

    assertThatThrownBy(() -> phoneVerificationService.sendOtp(phoneNumber))
        .isInstanceOf(OtpResendCooldownException.class);
  }

  @Test
  void givenNonexistentPhoneNumber_whenSendOtp_thenDoesNotThrowAndDoesNotSend() {
    String nonexistentPhoneNumber = "+381609999999";

    phoneVerificationService.sendOtp(nonexistentPhoneNumber);

    verify(whatsAppSender, never()).sendTemplate(any(), any(), any());
  }

  @Test
  void givenAlreadyVerifiedActiveUser_whenSendOtp_thenDoesNotSend() {
    String seed = String.valueOf(System.currentTimeMillis());
    String phoneNumber = "+381601" + seed.substring(seed.length() - 6);
    String email = "itest.otp.already.verified." + seed + "@fitme.com";

    userService.createUser(
        CreateUserRequestDTO.builder()
            .username("itest.otp.already.verified." + seed)
            .fullName("Integration OTP Already Verified User")
            .email(email)
            .phoneNumber(phoneNumber)
            .password("itest.otp.already.verified.fitme123!")
            .build());

    User user =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElseThrow();
    user.setPhoneVerified(true);
    user.setStatus(Status.ACTIVE);
    userRepository.save(user);

    phoneVerificationService.sendOtp(phoneNumber);

    verify(whatsAppSender, never()).sendTemplate(any(), any(), any());
  }
}
