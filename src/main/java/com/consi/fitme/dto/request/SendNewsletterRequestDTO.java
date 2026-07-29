package com.consi.fitme.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNewsletterRequestDTO {

  @NotBlank(message = "Naslov je obavezan")
  @Size(max = 200, message = "Naslov može imati najviše 200 karaktera")
  private String subject;

  @NotBlank(message = "Sadržaj je obavezan")
  private String htmlContent;
}
