package com.consi.fitme.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsletterSendResultDTO {

  private final int totalRecipients;
  private final int sentCount;
  private final int failedCount;
}
