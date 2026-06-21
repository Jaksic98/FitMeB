package com.consi.fitme.service;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.exception.auth.InvalidJwtTokenException;
import com.consi.fitme.exception.auth.LoginFailedException;
import com.consi.fitme.exception.auth.MissingCookieException;
import com.consi.fitme.exception.auth.MissingJwtTokenException;
import com.consi.fitme.exception.user.UserNotFoundException;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
  private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

  private final JwtService jwtService;
  private final AuthenticationProvider authenticationProvider;
  private final CustomUserDetailsService customUserDetailsService;
  private final UserRepository userRepository;

  public void login(String email, String password, HttpServletResponse response) {
    Optional<User> foundUser = userRepository.findByEmailAndStatusNot(email, Status.DELETED);
    if (foundUser.isPresent() && foundUser.get().getStatus() == Status.LOCKED) {
      throw new LoginFailedException("Neispravno korisničko ime ili lozinka");
    }

    try {
      Authentication authentication =
          authenticationProvider.authenticate(
              new UsernamePasswordAuthenticationToken(email, password));
      assert authentication != null;
      UserDetails user = (UserDetails) authentication.getPrincipal();
      if (user instanceof User authenticatedUser) {
        resetFailedAttemptsIfNeeded(authenticatedUser);
      }
      String jwt = jwtService.generateToken(user);
      response.addCookie(jwtService.generateCookie(jwt));
    } catch (BadCredentialsException ex) {
      foundUser.ifPresent(this::incrementFailedAttemptsAndLockIfNeeded);
      throw new LoginFailedException("Neispravno korisničko ime ili lozinka");
    } catch (AuthenticationException ex) {
      throw new LoginFailedException("Autentikacija nije uspela: " + ex.getMessage());
    }
  }

  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String jwt = jwtService.extractTokenFromCookie(request);

    if (jwt != null && !jwt.isEmpty()) {
      HttpSession session = request.getSession(false);

      if (session != null) {
        session.invalidate();
      }

      response.addCookie(createExpiredCookie(jwtService.getCookieName()));
    } else {
      throw new MissingJwtTokenException();
    }
  }

  public UserDTO me(HttpServletRequest request) {
    return getJwtUserDTO(jwtService.extractTokenFromCookie(request));
  }

  public void validateSession(HttpServletRequest request) {
    if (request.getCookies() == null) {
      throw new MissingCookieException();
    }

    String jwt = jwtService.extractTokenFromCookie(request);
    UserDetails userDetails = getJwtUserDetails(jwt);

    if (!jwtService.isTokenValid(jwt, userDetails)) {
      throw new InvalidJwtTokenException();
    }
  }

  public UserDTO getJwtUserDTO(String jwt) {
    return toDto((User) getJwtUserDetails(jwt));
  }

  public UserDetails getJwtUserDetails(String jwt) {
    if (jwt == null || jwt.isEmpty()) {
      throw new MissingJwtTokenException();
    }

    String email = jwtService.extractUsername(jwt);

    User user;
    try {
      user = (User) customUserDetailsService.loadUserByUsername(email);
    } catch (UsernameNotFoundException ex) {
      throw new UserNotFoundException(email);
    }

    return user;
  }

  private Cookie createExpiredCookie(String cookieName) {
    Cookie cookie = new Cookie(cookieName, null);
    cookie.setHttpOnly(true);
    cookie.setSecure(jwtService.isCookieSecure());
    cookie.setPath("/");
    cookie.setMaxAge(0);
    return cookie;
  }

  private UserDTO toDto(User user) {
    List<Role> roles =
        userRepository.findRoleCodesByUserId(user.getId()).stream().map(Role::valueOf).toList();
    return UserDTO.builder()
        .id(user.getId())
        .username(user.getUsername())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .status(user.getStatus())
        .roles(roles)
        .build();
  }

  private void resetFailedAttemptsIfNeeded(User user) {
    if (user.getFailedLoginAttempts() == null || user.getFailedLoginAttempts() == 0) {
      return;
    }
    user.setFailedLoginAttempts(0);
    userRepository.save(user);
  }

  private void incrementFailedAttemptsAndLockIfNeeded(User user) {
    if (user.getStatus() != Status.ACTIVE) {
      return;
    }

    int failedAttempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
    failedAttempts++;
    user.setFailedLoginAttempts(failedAttempts);

    if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
      user.setStatus(Status.LOCKED);
    }

    userRepository.save(user);
  }
}
