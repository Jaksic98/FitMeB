package com.consi.fitme.util;

import com.consi.fitme.dto.request.PagingRequestDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtils {

  public static Pageable getPageable(PagingRequestDTO pagingRequestDTO) {
    int page = pagingRequestDTO.getPage() != null ? Math.max(pagingRequestDTO.getPage() - 1, 0) : 0;
    int size = pagingRequestDTO.getSize() != null ? pagingRequestDTO.getSize() : 10;
    return PageRequest.of(
        page, size, pagingRequestDTO.getDirection(), pagingRequestDTO.getSortField());
  }
}
