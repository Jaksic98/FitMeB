package com.consi.fitme.service;

import com.consi.fitme.model.AuditAction;
import com.consi.fitme.model.entity.AuditLog;
import com.consi.fitme.repository.AuditLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

  private static final String REQUEST_ID_MDC_KEY = "requestId";
  private static final String USER_MDC_KEY = "user";

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  public void logCreate(String entityType, Long entityId, Object newRow) {
    logChange(AuditAction.CREATE, entityType, entityId, null, newRow, null);
  }

  public void logDelete(String entityType, Long entityId, Object oldRow) {
    logChange(AuditAction.DELETE, entityType, entityId, oldRow, null, null);
  }

  public void logUpdate(String entityType, Long entityId, Object oldRow, Object newRow) {
    logChange(
        AuditAction.UPDATE,
        entityType,
        entityId,
        oldRow,
        newRow,
        computeChangedColumns(oldRow, newRow));
  }

  public void logChange(
      AuditAction action,
      String entityType,
      Long entityId,
      Object oldRow,
      Object newRow,
      String[] changedCols) {
    auditLogRepository.save(
        AuditLog.builder()
            .actorUsername(resolveActorUsername())
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .oldRow(toJsonNode(oldRow))
            .newRow(toJsonNode(newRow))
            .changedCols(changedCols)
            .requestId(MDC.get(REQUEST_ID_MDC_KEY))
            .build());
  }

  private JsonNode toJsonNode(Object value) {
    if (value == null) {
      return null;
    }
    return objectMapper.valueToTree(value);
  }

  private String[] computeChangedColumns(Object oldRow, Object newRow) {
    if (oldRow == null || newRow == null) {
      return new String[0];
    }

    Map<String, Object> oldMapRaw = objectMapper.convertValue(oldRow, new TypeReference<>() {});
    Map<String, Object> newMapRaw = objectMapper.convertValue(newRow, new TypeReference<>() {});
    Map<String, Object> oldMap = oldMapRaw != null ? oldMapRaw : Collections.emptyMap();
    Map<String, Object> newMap = newMapRaw != null ? newMapRaw : Collections.emptyMap();
    Set<String> keys = new LinkedHashSet<>();
    keys.addAll(oldMap.keySet());
    keys.addAll(newMap.keySet());

    return keys.stream()
        .filter(key -> !Objects.deepEquals(oldMap.get(key), newMap.get(key)))
        .toArray(String[]::new);
  }

  private String resolveActorUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      String principalName = authentication.getName();
      if (principalName != null && !principalName.isBlank()) {
        return principalName;
      }
    }

    String mdcUser = MDC.get(USER_MDC_KEY);
    if (mdcUser != null && !mdcUser.isBlank()) {
      return mdcUser;
    }

    return "system";
  }
}
