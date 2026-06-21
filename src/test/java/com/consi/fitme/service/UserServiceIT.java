package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.dto.request.UserSearchRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.PagingResponseDTO;
import com.consi.fitme.exception.user.UserNotFoundException;
import com.consi.fitme.exception.user.UsernameAlreadyExistsException;
import com.consi.fitme.exception.user.WeakPasswordException;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class UserServiceIT {

  @Autowired private UserService service;
  @Autowired private UserRepository repository;

  @Test
  void givenNewUser_whenCreatedAndAssignedAdditionalRole_thenContainsDefaultAndAdditionalRoles() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.user." + seed;
    String email = "itest.user." + seed + "@fitme.com";

    CreateUserRequestDTO createRequest =
        CreateUserRequestDTO.builder()
            .username(username)
            .fullName("Integration Test User")
            .email(email)
            .password("itest.user.fitme123!")
            .build();

    UserDTO createdUser = service.createUser(createRequest);

    assertThat(createdUser.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(createdUser.getRoles()).containsExactly(Role.CLIENT);

    UpdateUserRequestDTO updateRequest =
        UpdateUserRequestDTO.builder()
            .username(username)
            .fullName("Integration Test User")
            .email(email)
            .status(Status.ACTIVE)
            .roles(List.of(Role.ADMIN))
            .build();

    UserDTO updatedUser = service.updateUser(createdUser.getId(), updateRequest);

    assertThat(updatedUser.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(updatedUser.getRoles()).contains(Role.CLIENT, Role.ADMIN);
    assertThat(repository.findRoleCodesByUserId(updatedUser.getId())).contains("CLIENT", "ADMIN");
  }

  @Test
  void givenNewUser_whenCreated_thenAssignsDefaultUserRole() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.default.role." + seed;
    String email = "itest.default.role." + seed + "@fitme.com";

    UserDTO createdUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(username)
                .fullName("Integration Default Role User")
                .email(email)
                .password("itest.default.role.fitme123!")
                .build());

    assertThat(createdUser.getRoles()).containsExactly(Role.CLIENT);
    assertThat(repository.findRoleCodesByUserId(createdUser.getId())).containsExactly("CLIENT");
  }

  @Test
  void givenNewUser_whenCreated_thenInitialStatusIsInactive() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.default.status." + seed;
    String email = "itest.default.status." + seed + "@fitme.com";

    UserDTO createdUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(username)
                .fullName("Integration Default Status User")
                .email(email)
                .password("itest.default.status.fitme123!")
                .build());

    assertThat(createdUser.getStatus()).isEqualTo(Status.INACTIVE);
  }

  @Test
  void givenExistingUser_whenUpdateWithOnlyIdAndStatus_thenUpdatesOnlyStatus() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.partial.update." + seed;
    String email = "itest.partial.update." + seed + "@fitme.com";

    UserDTO createdUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(username)
                .fullName("Integration Partial Update User")
                .email(email)
                .password("itest.partial.update.fitme123!")
                .build());

    UpdateUserRequestDTO updateRequest =
        UpdateUserRequestDTO.builder().status(Status.ACTIVE).build();

    UserDTO updatedUser = service.updateUser(createdUser.getId(), updateRequest);

    assertThat(updatedUser.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(updatedUser.getUsername()).isEqualTo(username);
    assertThat(updatedUser.getEmail()).isEqualTo(email);
    assertThat(updatedUser.getFullName()).isEqualTo("Integration Partial Update User");
  }

  @Test
  void givenPasswordWithMinLengthDigitAndSpecial_whenCreateUser_thenCreatesUser() {
    String seed = String.valueOf(System.currentTimeMillis());

    UserDTO createdUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username("itest.password.valid." + seed)
                .fullName("Integration Password Valid")
                .email("itest.password.valid." + seed + "@fitme.com")
                .password("Aa1@aaaa")
                .build());

    assertThat(createdUser.getId()).isNotNull();
    assertThat(createdUser.getStatus()).isEqualTo(Status.INACTIVE);
  }

  @Test
  void givenPasswordWithoutSpecialCharacter_whenCreateUser_thenThrowsWeakPasswordException() {
    String seed = String.valueOf(System.currentTimeMillis());
    CreateUserRequestDTO weakPasswordRequest =
        CreateUserRequestDTO.builder()
            .username("itest.password.no.special." + seed)
            .fullName("Integration Password No Special")
            .email("itest.password.no.special." + seed + "@fitme.com")
            .password("Password1")
            .build();

    assertThatThrownBy(() -> service.createUser(weakPasswordRequest))
        .isInstanceOf(WeakPasswordException.class);
  }

  @Test
  void givenPasswordWithoutDigit_whenCreateUser_thenThrowsWeakPasswordException() {
    String seed = String.valueOf(System.currentTimeMillis());
    CreateUserRequestDTO weakPasswordRequest =
        CreateUserRequestDTO.builder()
            .username("itest.password.no.digit." + seed)
            .fullName("Integration Password No Digit")
            .email("itest.password.no.digit." + seed + "@fitme.com")
            .password("Password@")
            .build();

    assertThatThrownBy(() -> service.createUser(weakPasswordRequest))
        .isInstanceOf(WeakPasswordException.class);
  }

  @Test
  void givenExistingUser_whenDeleteUser_thenPerformsSoftDeleteAndUserIsNotAccessible() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.delete.user." + seed;
    String email = "itest.delete.user." + seed + "@fitme.com";

    UserDTO createdUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(username)
                .fullName("Integration Delete User")
                .email(email)
                .password("itest.delete.user.fitme123!")
                .build());

    MessageResponseDTO response = service.deleteUser(createdUser.getId());

    assertThat(response.getMessage()).contains("Uspešno obrisan korisnik");
    assertThat(repository.findById(createdUser.getId())).isPresent();
    assertThat(repository.findById(createdUser.getId()).orElseThrow().getStatus())
        .isEqualTo(Status.DELETED);
    Long deletedUserId = createdUser.getId();
    assertThatThrownBy(() -> service.getUser(deletedUserId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void givenExistingUserId_whenGetUser_thenReturnsUserDetails() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.get." + seed;
    String email = "itest.get." + seed + "@fitme.com";

    CreateUserRequestDTO createRequest =
        CreateUserRequestDTO.builder()
            .username(username)
            .fullName("Integration Get User")
            .email(email)
            .password("itest.get.fitme123!")
            .build();

    UserDTO createdUser = service.createUser(createRequest);
    UserDTO fetchedUser = service.getUser(createdUser.getId());

    assertThat(fetchedUser.getId()).isEqualTo(createdUser.getId());
    assertThat(fetchedUser.getUsername()).isEqualTo(username);
    assertThat(fetchedUser.getEmail()).isEqualTo(email);
    assertThat(fetchedUser.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(fetchedUser.getRoles()).containsExactly(Role.CLIENT);
  }

  @Test
  void givenSearchRequestWithPaginationAndSorting_whenGetUsers_thenReturnsFilteredAndSortedPage() {
    String seed = String.valueOf(System.currentTimeMillis());
    String prefix = "itest.list." + seed;

    service.createUser(
        CreateUserRequestDTO.builder()
            .username(prefix + ".b")
            .fullName("Integration List B")
            .email(prefix + ".b@fitme.com")
            .password("itest.list.fitme123!")
            .build());

    service.createUser(
        CreateUserRequestDTO.builder()
            .username(prefix + ".a")
            .fullName("Integration List A")
            .email(prefix + ".a@fitme.com")
            .password("itest.list.fitme123!")
            .build());

    UserSearchRequestDTO searchRequest =
        UserSearchRequestDTO.builder()
            .username(prefix)
            .status(Collections.singletonList("INACTIVE"))
            .sortField("username")
            .direction(Sort.Direction.ASC)
            .page(1)
            .size(10)
            .build();

    PagingResponseDTO<UserDTO> page = service.getUsers(searchRequest);

    List<UserDTO> pageData = new ArrayList<>(page.getData());

    assertThat(pageData).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(pageData.get(0).getUsername()).isEqualTo(prefix + ".a");
    assertThat(pageData.get(1).getUsername()).isEqualTo(prefix + ".b");
  }

  @Test
  void givenRoleFilter_whenGetUsers_thenReturnsOnlyUsersWithRequestedRole() {
    String seed = String.valueOf(System.currentTimeMillis());
    String prefix = "itest.role.filter." + seed;

    UserDTO adminUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(prefix + ".admin")
                .fullName("Integration Role Admin")
                .email(prefix + ".admin@fitme.com")
                .password("itest.role.filter.fitme123!")
                .build());

    service.updateUser(
        adminUser.getId(),
        UpdateUserRequestDTO.builder().status(Status.ACTIVE).roles(List.of(Role.ADMIN)).build());

    UserDTO otherUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(prefix + ".other")
                .fullName("Integration Role Other")
                .email(prefix + ".other@fitme.com")
                .password("itest.role.filter.fitme123!")
                .build());

    service.updateUser(
        otherUser.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());

    UserSearchRequestDTO searchRequest =
        UserSearchRequestDTO.builder()
            .username(prefix)
            .roles(List.of(Role.ADMIN))
            .sortField("username")
            .direction(Sort.Direction.ASC)
            .page(1)
            .size(10)
            .build();

    PagingResponseDTO<UserDTO> page = service.getUsers(searchRequest);
    List<UserDTO> pageData = new ArrayList<>(page.getData());

    assertThat(pageData).hasSize(1);
    assertThat(pageData.getFirst().getId()).isEqualTo(adminUser.getId());
    assertThat(pageData.getFirst().getRoles()).contains(Role.ADMIN);
  }

  @Test
  void givenNonExistingUserId_whenGetUser_thenThrowsUserNotFoundException() {
    assertThatThrownBy(() -> service.getUser(Long.MAX_VALUE))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void givenDeletedUserId_whenGetUser_thenThrowsUserNotFoundException() {
    String seed = String.valueOf(System.currentTimeMillis());
    String username = "itest.deleted." + seed;
    String email = "itest.deleted." + seed + "@fitme.com";

    UserDTO createdUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(username)
                .fullName("Integration Deleted User")
                .email(email)
                .password("itest.deleted.fitme123!")
                .build());

    service.deleteUser(createdUser.getId());

    Long deletedUserId = createdUser.getId();
    assertThatThrownBy(() -> service.getUser(deletedUserId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void givenDeletedAndNonDeletedUsers_whenGetUsersWithoutStatusFilter_thenExcludesDeletedUsers() {
    String seed = String.valueOf(System.currentTimeMillis());
    String prefix = "itest.exclude.deleted." + seed;

    UserDTO activeLikeUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(prefix + ".one")
                .fullName("Exclude Deleted One")
                .email(prefix + ".one@fitme.com")
                .password("itest.exclude.fitme123!")
                .build());

    UserDTO toDeleteUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(prefix + ".two")
                .fullName("Exclude Deleted Two")
                .email(prefix + ".two@fitme.com")
                .password("itest.exclude.fitme123!")
                .build());

    service.deleteUser(toDeleteUser.getId());

    UserSearchRequestDTO searchRequest =
        UserSearchRequestDTO.builder()
            .username(prefix)
            .sortField("username")
            .direction(Sort.Direction.ASC)
            .page(1)
            .size(10)
            .build();

    PagingResponseDTO<UserDTO> page = service.getUsers(searchRequest);
    List<UserDTO> pageData = new ArrayList<>(page.getData());

    assertThat(pageData).hasSize(1);
    assertThat(pageData.getFirst().getId()).isEqualTo(activeLikeUser.getId());
    assertThat(pageData.getFirst().getUsername()).isEqualTo(prefix + ".one");
  }

  @Test
  void
      givenAnotherActiveUserWithSameUsername_whenUpdateUserStatusToActive_thenThrowsUsernameAlreadyExists() {
    String seed = String.valueOf(System.currentTimeMillis());
    String sharedUsername = "itest.update.active.conflict." + seed;

    UserDTO activeUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(sharedUsername)
                .fullName("Active Username Owner")
                .email("itest.active.owner." + seed + "@fitme.com")
                .password("itest.update.active.fitme123!")
                .build());

    service.updateUser(
        activeUser.getId(),
        UpdateUserRequestDTO.builder()
            .username(sharedUsername)
            .fullName("Active Username Owner")
            .email("itest.active.owner." + seed + "@fitme.com")
            .status(Status.ACTIVE)
            .build());

    UserDTO inactiveUserWithSameUsername =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(sharedUsername)
                .fullName("Inactive Same Username")
                .email("itest.inactive.same.username." + seed + "@fitme.com")
                .password("itest.update.active.fitme123!")
                .build());

    assertThat(inactiveUserWithSameUsername.getStatus()).isEqualTo(Status.INACTIVE);

    UpdateUserRequestDTO activateWithDuplicateUsernameRequest =
        UpdateUserRequestDTO.builder().status(Status.ACTIVE).build();

    Long inactiveUserId = inactiveUserWithSameUsername.getId();
    assertThatThrownBy(
            () -> service.updateUser(inactiveUserId, activateWithDuplicateUsernameRequest))
        .isInstanceOf(UsernameAlreadyExistsException.class);
  }

  @Test
  void
      givenActiveAndInactiveUsersWithSameUsername_whenUpdatingInactiveWithoutActivation_thenUpdateSucceeds() {
    String seed = String.valueOf(System.currentTimeMillis());
    String sharedUsername = "itest.update.inactive.allowed." + seed;

    UserDTO activeUser =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(sharedUsername)
                .fullName("Active Username Owner")
                .email("itest.inactive.allowed.active." + seed + "@fitme.com")
                .password("itest.update.inactive.fitme123!")
                .build());

    service.updateUser(
        activeUser.getId(), UpdateUserRequestDTO.builder().status(Status.ACTIVE).build());

    UserDTO inactiveUserWithSameUsername =
        service.createUser(
            CreateUserRequestDTO.builder()
                .username(sharedUsername)
                .fullName("Inactive Same Username")
                .email("itest.inactive.allowed.inactive." + seed + "@fitme.com")
                .password("itest.update.inactive.fitme123!")
                .build());

    UserDTO updatedInactiveUser =
        service.updateUser(
            inactiveUserWithSameUsername.getId(),
            UpdateUserRequestDTO.builder()
                .fullName("Inactive Same Username Updated")
                .status(Status.INACTIVE)
                .build());

    assertThat(updatedInactiveUser.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(updatedInactiveUser.getUsername()).isEqualTo(sharedUsername);
    assertThat(updatedInactiveUser.getFullName()).isEqualTo("Inactive Same Username Updated");
  }
}
