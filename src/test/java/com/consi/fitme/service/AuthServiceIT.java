package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.exception.auth.LoginFailedException;
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

  private UserDTO createActiveUser(String seed, String email, String password) {
    UserDTO createdUser =
        userService.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.login.user." + seed)
                .fullName("Integration Login User")
                .email(email)
                .password(password)
                .build());

    return userService.updateUser(
        createdUser.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());
  }
}
