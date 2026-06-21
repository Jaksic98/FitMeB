package com.consi.fitme.repository;

import com.consi.fitme.dto.request.UserSearchRequestDTO;
import com.consi.fitme.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserQueryRepository {

  Page<User> searchUsers(UserSearchRequestDTO searchRequest, Pageable pageable);
}
