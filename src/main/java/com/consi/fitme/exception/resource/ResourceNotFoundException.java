package com.consi.fitme.exception.resource;

import com.consi.fitme.exception.base.NotFoundException;
import com.consi.fitme.model.ErrorCode;

public class ResourceNotFoundException extends NotFoundException {
  public ResourceNotFoundException(String message) {
    super(message, ErrorCode.RESOURCE_NOT_FOUND);
  }
}
