package com.consi.fitme.repository;

import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserQueryRepository {

  Optional<User> findByEmailAndStatusNot(String email, Status status);

  Optional<User> findByEmailAndStatus(String email, Status status);

  Optional<User> findByUsernameIgnoreCaseAndStatus(String username, Status status);

  Optional<User> findByPhoneNumberAndStatusNot(String phoneNumber, Status status);

  @Query(
      value =
          """
          SELECT DISTINCT r.code
          FROM roles r
          JOIN user_roles ur ON ur.role_id = r.id
          WHERE ur.user_id = :userId
          """,
      nativeQuery = true)
  List<String> findRoleCodesByUserId(@Param("userId") Long userId);

  @Modifying
  @Query(value = "DELETE FROM user_roles WHERE user_id = :userId", nativeQuery = true)
  void deleteUserRolesByUserId(@Param("userId") Long userId);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO user_roles (user_id, role_id)
          SELECT :userId, r.id
          FROM roles r
          WHERE r.code = :roleCode
          """,
      nativeQuery = true)
  void addUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
