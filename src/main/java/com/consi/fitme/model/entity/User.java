package com.consi.fitme.model.entity;

import com.consi.fitme.model.Status;
import jakarta.persistence.*;
import java.io.Serial;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(updatable = false, nullable = false)
  private Long id;

  @Column(nullable = false)
  private String username;

  private String fullName;

  @Column(unique = true, nullable = false)
  private String email;

  private String password;

  @Column(name = "status", nullable = false)
  @Builder.Default
  private Status status = Status.ACTIVE;

  @Column(name = "failed_login_attempts", nullable = false)
  @Builder.Default
  private Integer failedLoginAttempts = 0;

  @Column(name = "phone_number", nullable = false)
  private String phoneNumber;

  @Column(name = "remaining_appointments", nullable = false)
  @Builder.Default
  private Integer remainingAppointments = 0;

  @Column(name = "email_notifications", nullable = false)
  @Builder.Default
  private Boolean emailNotifications = true;

  @Column(name = "calendar_notifications", nullable = false)
  @Builder.Default
  private Boolean calendarNotifications = true;

  @Column(name = "phone_verified", nullable = false)
  @Builder.Default
  private Boolean phoneVerified = false;

  @Column(name = "membership_expires_at")
  private LocalDate membershipExpiresAt;

  @Transient private List<SimpleGrantedAuthority> grantedAuthorities;

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    if (grantedAuthorities != null && !grantedAuthorities.isEmpty()) {
      return grantedAuthorities;
    }
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public @NonNull String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return getStatus() != Status.LOCKED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return getStatus() == Status.ACTIVE;
  }
}
