package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.RegisterRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.exception.auth.InvalidActivationTokenException;
import com.consi.fitme.exception.auth.LoginFailedException;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class AuthServiceIT {

  private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

  @Autowired private AuthService service;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private ActivationTokenService activationTokenService;

  @Test
  void givenInactiveUser_whenLogin_thenThrowsLoginFailedException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.login.inactive." + seed + "@fitme.com";

    UserDTO createdUser =
        userService.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.login.inactive." + seed)
                .fullName("Integration Inactive Login")
                .email(email)
                .phoneNumber("+381601234567")
                .password("itest.login.inactive.fitme123!")
                .build());

    assertThat(createdUser.getStatus()).isEqualTo(Status.INACTIVE);

    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(() -> service.login(email, "itest.login.inactive.fitme123!", response))
        .isInstanceOf(LoginFailedException.class);

    Cookie[] cookies = response.getCookies();
    assertThat(cookies).isNullOrEmpty();
  }

  @Test
  void givenActiveUser_whenLoginFailsMultipleTimes_thenFailedAttemptsIncreaseAndUserGetsLocked() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.login.lock." + seed + "@fitme.com";
    String password = "itest.login.lock.fitme123!";
    String invalidPassword = password + "x";

    UserDTO activeUser = createActiveUser(seed, email, password);

    for (int i = 1; i <= MAX_FAILED_LOGIN_ATTEMPTS; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      assertThatThrownBy(() -> service.login(email, invalidPassword, response))
          .isInstanceOf(LoginFailedException.class);

      User persisted = userRepository.findById(activeUser.getId()).orElseThrow();
      assertThat(persisted.getFailedLoginAttempts()).isEqualTo(i);
      if (i < MAX_FAILED_LOGIN_ATTEMPTS) {
        assertThat(persisted.getStatus()).isEqualTo(Status.ACTIVE);
      } else {
        assertThat(persisted.getStatus()).isEqualTo(Status.LOCKED);
      }
      assertThat(response.getCookies()).isNullOrEmpty();
    }
  }

  @Test
  void givenLockedUser_whenLoginWithValidPassword_thenLoginFailsAndNoCookieIsIssued() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.login.locked." + seed + "@fitme.com";
    String password = "itest.login.locked.fitme123!";
    String invalidPassword = password + "x";

    UserDTO activeUser = createActiveUser(seed, email, password);

    for (int i = 0; i < MAX_FAILED_LOGIN_ATTEMPTS; i++) {
      MockHttpServletResponse failedResponse = new MockHttpServletResponse();
      assertThatThrownBy(() -> service.login(email, invalidPassword, failedResponse))
          .isInstanceOf(LoginFailedException.class);
    }

    MockHttpServletResponse response = new MockHttpServletResponse();
    assertThatThrownBy(() -> service.login(email, password, response))
        .isInstanceOf(LoginFailedException.class);

    User persisted = userRepository.findById(activeUser.getId()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(Status.LOCKED);
    assertThat(persisted.getFailedLoginAttempts()).isEqualTo(MAX_FAILED_LOGIN_ATTEMPTS);
    assertThat(response.getCookies()).isNullOrEmpty();
  }

  @Test
  void givenActiveUserWithFailedAttempts_whenLoginSucceeds_thenFailedAttemptsResetToZero() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.login.reset." + seed + "@fitme.com";
    String password = "itest.login.reset.fitme123!";
    String invalidPassword = password + "x";

    UserDTO activeUser = createActiveUser(seed, email, password);

    MockHttpServletResponse failedResponse1 = new MockHttpServletResponse();
    MockHttpServletResponse failedResponse2 = new MockHttpServletResponse();
    assertThatThrownBy(() -> service.login(email, invalidPassword, failedResponse1))
        .isInstanceOf(LoginFailedException.class);
    assertThatThrownBy(() -> service.login(email, invalidPassword, failedResponse2))
        .isInstanceOf(LoginFailedException.class);

    MockHttpServletResponse successResponse = new MockHttpServletResponse();
    service.login(email, password, successResponse);

    User persisted = userRepository.findById(activeUser.getId()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(persisted.getFailedLoginAttempts()).isZero();
    assertThat(successResponse.getCookies()).isNotNull();
    assertThat(successResponse.getCookies()).isNotEmpty();
  }

  @Test
  void givenLockedUser_whenAdminSetsStatusActive_thenUserUnlocksAndFailedAttemptsReset() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.login.unlock." + seed + "@fitme.com";
    String password = "itest.login.unlock.fitme123!";
    String invalidPassword = password + "x";

    UserDTO activeUser = createActiveUser(seed, email, password);

    for (int i = 0; i < MAX_FAILED_LOGIN_ATTEMPTS; i++) {
      MockHttpServletResponse failedResponse = new MockHttpServletResponse();
      assertThatThrownBy(() -> service.login(email, invalidPassword, failedResponse))
          .isInstanceOf(LoginFailedException.class);
    }

    User beforeUnlock = userRepository.findById(activeUser.getId()).orElseThrow();
    assertThat(beforeUnlock.getStatus()).isEqualTo(Status.LOCKED);
    assertThat(beforeUnlock.getFailedLoginAttempts()).isEqualTo(MAX_FAILED_LOGIN_ATTEMPTS);

    userService.updateUser(
        activeUser.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());

    User afterUnlock = userRepository.findById(activeUser.getId()).orElseThrow();
    assertThat(afterUnlock.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(afterUnlock.getFailedLoginAttempts()).isZero();
  }

  @Test
  void givenValidRegistration_whenRegister_thenCreatesInactiveClientUserWithDefaults() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.register." + seed + "@fitme.com";

    UserDTO registeredUser =
        service.register(
            RegisterRequestDTO.builder()
                .username("itest.register." + seed)
                .fullName("Integration Register User")
                .email(email)
                .phoneNumber("+381601234567")
                .password("itest.register.fitme123!")
                .build());

    assertThat(registeredUser.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(registeredUser.getRoles()).containsExactly(Role.CLIENT);
    assertThat(registeredUser.getRemainingAppointments()).isZero();
    assertThat(registeredUser.getEmailNotifications()).isTrue();
    assertThat(registeredUser.getCalendarNotifications()).isTrue();
    assertThat(registeredUser.getPhoneNumber()).isEqualTo("+381601234567");
  }

  @Test
  void givenValidRegistration_whenRegister_thenPhoneVerifiedIsFalse() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.register.phoneverified." + seed + "@fitme.com";

    UserDTO registeredUser =
        service.register(
            RegisterRequestDTO.builder()
                .username("itest.register.phoneverified." + seed)
                .fullName("Integration Register Phone Verified")
                .email(email)
                .phoneNumber("+381601234567")
                .password("itest.register.fitme123!")
                .build());

    User persistedUser = userRepository.findById(registeredUser.getId()).orElseThrow();
    assertThat(persistedUser.getPhoneVerified()).isFalse();
  }

  @Test
  void givenRegisteredUser_whenActivateWithValidToken_thenStatusBecomesActive() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.activate." + seed + "@fitme.com";

    UserDTO registeredUser =
        service.register(
            RegisterRequestDTO.builder()
                .username("itest.activate." + seed)
                .fullName("Integration Activate User")
                .email(email)
                .phoneNumber("+381601234567")
                .password("itest.activate.fitme123!")
                .build());

    String activationToken = activationTokenService.generateToken(email);
    service.activate(activationToken);

    User persisted = userRepository.findById(registeredUser.getId()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(persisted.getFailedLoginAttempts()).isZero();
  }

  @Test
  void givenAlreadyActiveUser_whenActivateAgain_thenRemainsActiveWithoutError() {
    String seed = String.valueOf(System.currentTimeMillis());
    String email = "itest.activate.twice." + seed + "@fitme.com";

    UserDTO registeredUser =
        service.register(
            RegisterRequestDTO.builder()
                .username("itest.activate.twice." + seed)
                .fullName("Integration Activate Twice User")
                .email(email)
                .phoneNumber("+381601234567")
                .password("itest.activate.twice.fitme123!")
                .build());

    String activationToken = activationTokenService.generateToken(email);
    service.activate(activationToken);
    service.activate(activationToken);

    User persisted = userRepository.findById(registeredUser.getId()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void givenMalformedToken_whenActivate_thenThrowsInvalidActivationTokenException() {
    assertThatThrownBy(() -> service.activate("not-a-real-token"))
        .isInstanceOf(InvalidActivationTokenException.class);
  }

  @Test
  void givenTokenForUnknownEmail_whenActivate_thenThrowsInvalidActivationTokenException() {
    String token =
        activationTokenService.generateToken("itest.unknown." + System.nanoTime() + "@fitme.com");

    assertThatThrownBy(() -> service.activate(token))
        .isInstanceOf(InvalidActivationTokenException.class);
  }

  private UserDTO createActiveUser(String seed, String email, String password) {
    UserDTO createdUser =
        userService.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.login.user." + seed)
                .fullName("Integration Login User")
                .email(email)
                .phoneNumber("+381601234567")
                .password(password)
                .build());

    return userService.updateUser(
        createdUser.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());
  }
}
