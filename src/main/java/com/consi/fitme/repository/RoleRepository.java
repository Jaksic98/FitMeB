package com.consi.fitme.repository;

import com.consi.fitme.model.Role;
import com.consi.fitme.model.entity.RoleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

  Optional<RoleEntity> findByCode(Role code);

  @Query(
      value =
          """
                            SELECT DISTINCT ur.user_id
                            FROM user_roles ur
                            JOIN roles r ON r.id = ur.role_id
                            WHERE r.code IN (:roleCodes)
                            """,
      nativeQuery = true)
  List<Long> findDistinctUserIdsByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
