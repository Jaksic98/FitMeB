package com.consi.fitme.service;

import com.consi.fitme.exception.auth.InvalidOtpException;
import com.consi.fitme.exception.auth.OtpResendCooldownException;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

  private static final Logger logger = LoggerFactory.getLogger(PhoneVerificationService.class);

  private static final int OTP_VALIDITY_MINUTES = 5;
  private static final int OTP_COOLDOWN_MINUTES = 4;
  private static final int OTP_MIN = 100000;
  private static final int OTP_MAX = 999999;
  private static final SecureRandom random = new SecureRandom();

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final WhatsAppSender whatsAppSender;

  @Value("${infobip.templates.otp:fitme_otp}")
  private String otpTemplateName;

  @Transactional
  public void sendOtp(String phoneNumber) {
    User user =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElse(null);

    if (user == null) {
      logger.info("sendOtp: korisnik sa brojem telefona nije pronađen: {}", phoneNumber);
      return;
    }

    if (user.getPhoneVerified() && user.getStatus() != Status.INACTIVE) {
      logger.info("sendOtp: korisnik je već verifikovan: {}", phoneNumber);
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    if (user.getOtpExpiresAt() != null
        && user.getOtpExpiresAt().isAfter(now.plusMinutes(OTP_COOLDOWN_MINUTES))) {
      throw new OtpResendCooldownException();
    }

    String code = generateOtpCode();
    String otpHash = passwordEncoder.encode(code);
    LocalDateTime expiresAt = now.plusMinutes(OTP_VALIDITY_MINUTES);

    user.setOtpHash(otpHash);
    user.setOtpExpiresAt(expiresAt);
    userRepository.save(user);

    try {
      whatsAppSender.sendTemplate(phoneNumber, otpTemplateName, List.of(code));
    } catch (Exception ex) {
      logger.error(
          "Greška pri slanju OTP koda: phoneNumber={}, error={}", phoneNumber, ex.getMessage(), ex);
    }
  }

  @Transactional
  public void verifyOtp(String phoneNumber, String code) {
    User user =
        userRepository.findByPhoneNumberAndStatusNot(phoneNumber, Status.DELETED).orElse(null);

    if (user == null || user.getOtpHash() == null) {
      throw new InvalidOtpException();
    }

    LocalDateTime now = LocalDateTime.now();
    if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(now)) {
      throw new InvalidOtpException();
    }

    if (!passwordEncoder.matches(code, user.getOtpHash())) {
      throw new InvalidOtpException();
    }

    user.setPhoneVerified(true);
    user.setOtpHash(null);
    user.setOtpExpiresAt(null);

    if (user.getStatus() == Status.INACTIVE) {
      user.setStatus(Status.ACTIVE);
      user.setFailedLoginAttempts(0);
    }

    userRepository.save(user);
  }

  private String generateOtpCode() {
    int code = OTP_MIN + random.nextInt(OTP_MAX - OTP_MIN + 1);
    return String.valueOf(code);
  }
}
