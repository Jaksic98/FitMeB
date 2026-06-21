package com.consi.fitme.model.entity;

import com.consi.fitme.model.AuditAction;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(updatable = false, nullable = false)
  private Long id;

  @Column(name = "actor_username", nullable = false, length = 100)
  private String actorUsername;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AuditAction action;

  @Column(name = "entity_type", nullable = false, length = 100)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "old_row", columnDefinition = "jsonb")
  private JsonNode oldRow;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "new_row", columnDefinition = "jsonb")
  private JsonNode newRow;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "changed_cols", columnDefinition = "text[]")
  private String[] changedCols;

  @Column(name = "request_id", length = 100)
  private String requestId;

  @CreationTimestamp
  @Column(name = "occurred_at", nullable = false, updatable = false)
  private LocalDateTime occurredAt;
}
