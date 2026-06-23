package com.consi.fitme.service;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.dto.request.UserSearchRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.PagingResponseDTO;
import com.consi.fitme.exception.user.UserNotFoundException;
import com.consi.fitme.exception.user.UsernameAlreadyExistsException;
import com.consi.fitme.exception.user.WeakPasswordException;
import com.consi.fitme.mapper.UserPatchMapper;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import com.consi.fitme.repository.UserRepository;
import com.consi.fitme.util.PaginationUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*\\d)(?=.*[\\W_]).{8,72}$");
  private static final String ENTITY_TYPE = "USER";

  private final UserRepository repository;
  private final UserPatchMapper userPatchMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuditLogService auditLogService;

  public PagingResponseDTO<UserDTO> getUsers(UserSearchRequestDTO searchRequest) {
    Pageable pageable = PaginationUtils.getPageable(searchRequest);
    Page<User> page = repository.searchUsers(searchRequest, pageable);
    List<UserDTO> data = page.getContent().stream().map(this::toDto).toList();

    return new PagingResponseDTO<>(
        data,
        page.getTotalPages(),
        page.getTotalElements(),
        page.getSize(),
        page.getNumber(),
        page.isEmpty());
  }

  public UserDTO getUser(Long id) {
    User user =
        repository
            .findById(id)
            .filter(existing -> existing.getStatus() != Status.DELETED)
            .orElseThrow(() -> new UserNotFoundException(id));
    return toDto(user);
  }

  @Transactional
  public UserDTO createUser(CreateUserRequestDTO createUserRequestDTO) {
    if (createUserRequestDTO.getPassword() == null
        || !PASSWORD_PATTERN.matcher(createUserRequestDTO.getPassword()).matches()) {
      throw new WeakPasswordException();
    }
    ensureEmailUnique(createUserRequestDTO.getEmail(), null);

    User user =
        User.builder()
            .username(createUserRequestDTO.getUsername())
            .fullName(createUserRequestDTO.getFullName())
            .email(createUserRequestDTO.getEmail())
            .password(passwordEncoder.encode(createUserRequestDTO.getPassword()))
            .phoneNumber(createUserRequestDTO.getPhoneNumber())
            .build();
    user.setStatus(Status.INACTIVE);
    if (createUserRequestDTO.getRemainingAppointments() != null) {
      user.setRemainingAppointments(createUserRequestDTO.getRemainingAppointments());
    }
    if (createUserRequestDTO.getEmailNotifications() != null) {
      user.setEmailNotifications(createUserRequestDTO.getEmailNotifications());
    }
    if (createUserRequestDTO.getCalendarNotifications() != null) {
      user.setCalendarNotifications(createUserRequestDTO.getCalendarNotifications());
    }
    ensureUsernameUniqueForActiveStatus(createUserRequestDTO.getUsername(), null, user.getStatus());

    try {
      User savedUser = repository.save(user);
      syncUserRoles(savedUser.getId(), createUserRequestDTO.getRoles());
      UserDTO created = toDto(savedUser);
      auditLogService.logCreate(ENTITY_TYPE, savedUser.getId(), created);
      return created;
    } catch (DataIntegrityViolationException ex) {
      throw new UsernameAlreadyExistsException(createUserRequestDTO.getUsername());
    }
  }

  @Transactional
  public UserDTO updateUser(Long id, UpdateUserRequestDTO updateUserRequestDTO) {
    User existingUser = repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    UserDTO oldState = toDto(existingUser);

    String nextUsername =
        updateUserRequestDTO.getUsername() != null
            ? updateUserRequestDTO.getUsername()
            : existingUser.getUsername();
    String nextEmail =
        updateUserRequestDTO.getEmail() != null
            ? updateUserRequestDTO.getEmail()
            : existingUser.getEmail();
    Status nextStatus =
        updateUserRequestDTO.getStatus() != null
            ? updateUserRequestDTO.getStatus()
            : existingUser.getStatus();
    ensureUsernameUniqueForActiveStatus(nextUsername, id, nextStatus);
    ensureEmailUnique(nextEmail, id);

    userPatchMapper.applyPatch(updateUserRequestDTO, existingUser);

    if (updateUserRequestDTO.getPassword() != null
        && !updateUserRequestDTO.getPassword().isBlank()) {
      if (!PASSWORD_PATTERN.matcher(updateUserRequestDTO.getPassword()).matches()) {
        throw new WeakPasswordException();
      }
      existingUser.setPassword(passwordEncoder.encode(updateUserRequestDTO.getPassword()));
    }

    if (updateUserRequestDTO.getStatus() == Status.ACTIVE) {
      existingUser.setFailedLoginAttempts(0);
    }

    try {
      User savedUser = repository.save(existingUser);
      if (updateUserRequestDTO.getRoles() != null) {
        syncUserRoles(savedUser.getId(), updateUserRequestDTO.getRoles());
      }
      UserDTO updated = toDto(savedUser);
      auditLogService.logUpdate(ENTITY_TYPE, savedUser.getId(), oldState, updated);
      return updated;
    } catch (DataIntegrityViolationException ex) {
      throw new UsernameAlreadyExistsException(updateUserRequestDTO.getUsername());
    }
  }

  @Transactional
  public MessageResponseDTO deleteUser(Long id) {
    User user =
        repository
            .findById(id)
            .filter(existing -> existing.getStatus() != Status.DELETED)
            .orElseThrow(() -> new UserNotFoundException(id));

    UserDTO oldState = toDto(user);
    user.setStatus(Status.DELETED);
    repository.save(user);
    auditLogService.logDelete(ENTITY_TYPE, user.getId(), oldState);
    return new MessageResponseDTO("Uspešno obrisan korisnik za ID: " + id);
  }

  private void ensureEmailUnique(String email, Long currentUserId) {
    Optional<User> existing = repository.findByEmailAndStatusNot(email, Status.DELETED);
    if (existing.isPresent()
        && (currentUserId == null || !existing.get().getId().equals(currentUserId))) {
      throw new UsernameAlreadyExistsException(email);
    }
  }

  private void ensureUsernameUniqueForActiveStatus(
      String username, Long currentUserId, Status status) {
    if (status != Status.ACTIVE) {
      return;
    }

    Optional<User> existing = repository.findByUsernameIgnoreCaseAndStatus(username, Status.ACTIVE);
    if (existing.isPresent()
        && (currentUserId == null || !existing.get().getId().equals(currentUserId))) {
      throw new UsernameAlreadyExistsException(username);
    }
  }

  private UserDTO toDto(User user) {
    List<Role> roles =
        repository.findRoleCodesByUserId(user.getId()).stream().map(Role::valueOf).toList();

    return UserDTO.builder()
        .id(user.getId())
        .username(user.getUsername())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .phoneNumber(user.getPhoneNumber())
        .status(user.getStatus())
        .roles(roles)
        .remainingAppointments(user.getRemainingAppointments())
        .emailNotifications(user.getEmailNotifications())
        .calendarNotifications(user.getCalendarNotifications())
        .build();
  }

  private void syncUserRoles(Long userId, List<Role> additionalRoles) {
    Set<Role> targetRoles = EnumSet.of(Role.CLIENT);
    if (additionalRoles != null) {
      targetRoles.addAll(additionalRoles);
    }

    repository.deleteUserRolesByUserId(userId);
    targetRoles.forEach(role -> repository.addUserRole(userId, role.name()));
  }
}
