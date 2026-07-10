package com.consi.fitme.dto.request;

import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TerminSearchRequestDTO extends PagingRequestDTO {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;
}
