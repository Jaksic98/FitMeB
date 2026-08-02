package com.consi.fitme.dto.request;

import com.consi.fitme.model.Status;
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
public class UpdateNewsletterTemplateRequestDTO {

  @Size(max = 120, message = "Naziv može imati najviše 120 karaktera")
  private String title;

  @Size(max = 500, message = "Opis može imati najviše 500 karaktera")
  private String description;

  private String htmlContent;

  @Size(max = 200, message = "Naslov može imati najviše 200 karaktera")
  private String defaultSubject;

  private Status status;
}
