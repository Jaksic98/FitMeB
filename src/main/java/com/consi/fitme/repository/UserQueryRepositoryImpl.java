package com.consi.fitme.repository;

import com.consi.fitme.dto.request.UserSearchRequestDTO;
import com.consi.fitme.exception.base.BusinessException;
import com.consi.fitme.model.ErrorCode;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.QUser;
import com.consi.fitme.model.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

  private static final Set<String> ALLOWED_SORT_FIELDS =
      new HashSet<>(List.of("id", "username", "fullName", "email", "status"));

  private final RoleRepository roleRepository;
  @PersistenceContext private EntityManager entityManager;

  @Override
  public Page<User> searchUsers(UserSearchRequestDTO searchRequest, Pageable pageable) {
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    QUser user = QUser.user;
    BooleanBuilder predicate = buildPredicate(searchRequest, user);

    List<User> content =
        queryFactory
            .selectFrom(user)
            .where(predicate)
            .orderBy(
                buildOrderSpecifier(searchRequest.getSortField(), searchRequest.getDirection()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total = queryFactory.select(user.count()).from(user).where(predicate).fetchOne();
    long totalElements = total != null ? total : 0L;

    return new PageImpl<>(content, pageable, totalElements);
  }

  private BooleanBuilder buildPredicate(UserSearchRequestDTO searchRequest, QUser user) {
    BooleanBuilder predicate = new BooleanBuilder();

    List<Status> statuses = parseStatuses(searchRequest.getStatus());
    if (!statuses.isEmpty()) {
      predicate.and(user.status.in(statuses));
    } else {
      predicate.and(user.status.ne(Status.DELETED));
    }

    if (searchRequest.getId() != null) {
      predicate.and(user.id.eq(searchRequest.getId()));
    }
    if (hasText(searchRequest.getUsername())) {
      predicate.and(user.username.containsIgnoreCase(searchRequest.getUsername().trim()));
    }
    if (hasText(searchRequest.getFullName())) {
      predicate.and(user.fullName.containsIgnoreCase(searchRequest.getFullName().trim()));
    }
    if (hasText(searchRequest.getEmail())) {
      predicate.and(user.email.containsIgnoreCase(searchRequest.getEmail().trim()));
    }
    if (searchRequest.getRoles() != null && !searchRequest.getRoles().isEmpty()) {
      Set<Long> userIds = findUserIdsByRoles(searchRequest.getRoles());
      if (userIds.isEmpty()) {
        predicate.and(user.id.isNull());
      } else {
        predicate.and(user.id.in(userIds));
      }
    }
    return predicate;
  }

  private OrderSpecifier<?> buildOrderSpecifier(String sortField, Sort.Direction direction) {
    String resolvedSortField =
        hasText(sortField) && ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "id";
    QUser user = QUser.user;
    boolean asc = direction == Sort.Direction.ASC;

    return switch (resolvedSortField) {
      case "username" -> asc ? user.username.asc() : user.username.desc();
      case "fullName" -> asc ? user.fullName.asc() : user.fullName.desc();
      case "email" -> asc ? user.email.asc() : user.email.desc();
      case "status" -> asc ? user.status.asc() : user.status.desc();
      default -> asc ? user.id.asc() : user.id.desc();
    };
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private Set<Long> findUserIdsByRoles(List<Role> roles) {
    List<String> roleCodes =
        roles.stream().filter(Objects::nonNull).map(Role::name).distinct().toList();
    if (roleCodes.isEmpty()) {
      return Set.of();
    }

    return new HashSet<>(roleRepository.findDistinctUserIdsByRoleCodes(roleCodes));
  }

  private List<Status> parseStatuses(List<String> rawStatuses) {
    if (rawStatuses == null || rawStatuses.isEmpty()) {
      return List.of();
    }

    return rawStatuses.stream()
        .filter(Objects::nonNull)
        .flatMap(statusValue -> Stream.of(statusValue.split(",")))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(value -> parseStatus(value, rawStatuses))
        .distinct()
        .toList();
  }

  private Status parseStatus(String value, List<String> rawStatuses) {
    try {
      return Status.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(
          "Neispravan status parametar: " + String.join(", ", rawStatuses),
          ErrorCode.VALIDATION_FAILED);
    }
  }
}
