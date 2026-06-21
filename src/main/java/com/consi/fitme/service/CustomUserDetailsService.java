package com.consi.fitme.service;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String email)
      throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmailAndStatus(email, Status.ACTIVE)
            .orElseThrow(() -> new UsernameNotFoundException("Korisnik nije pronađen"));

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    userRepository.findRoleCodesByUserId(user.getId()).stream()
        .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
        .forEach(authorities::add);

    if (!authorities.isEmpty()) {
      user.setGrantedAuthorities(authorities);
    }
    return user;
  }
}
