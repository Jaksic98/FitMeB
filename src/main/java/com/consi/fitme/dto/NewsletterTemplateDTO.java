package com.consi.fitme.dto;

import com.consi.fitme.model.Status;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsletterTemplateDTO {

  private final Long id;
  private final String title;
  private final String description;
  private final String htmlContent;
  private final String defaultSubject;
  private final Status status;
}
