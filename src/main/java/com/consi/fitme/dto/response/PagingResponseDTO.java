package com.consi.fitme.dto.response;

import java.util.Collection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PagingResponseDTO<T> {

  private Collection<T> data;
  private Integer totalPages;
  private long totalElements;
  private Integer size;
  private Integer page;
  private boolean empty;

  public PagingResponseDTO(
      Collection<T> data,
      Integer totalPages,
      long totalElements,
      Integer size,
      Integer page,
      boolean empty) {
    this.data = data;
    this.totalPages = totalPages;
    this.totalElements = totalElements;
    this.size = size;
    this.page = page + 1;
    this.empty = empty;
  }
}
