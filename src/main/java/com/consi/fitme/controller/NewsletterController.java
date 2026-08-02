package com.consi.fitme.controller;

import com.consi.fitme.dto.NewsletterSendResultDTO;
import com.consi.fitme.dto.request.SendNewsletterRequestDTO;
import com.consi.fitme.dto.response.SuccessResponseDTO;
import com.consi.fitme.service.NewsletterService;
import com.consi.fitme.util.ApiPaths;
import com.consi.fitme.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(ApiPaths.NEWSLETTER)
@RestController
@AllArgsConstructor
public class NewsletterController {

  private final NewsletterService service;

  @PostMapping("/send")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SuccessResponseDTO<NewsletterSendResultDTO>> sendNewsletter(
      @Valid @RequestBody SendNewsletterRequestDTO sendNewsletterRequestDTO,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        ResponseUtil.success(
            service.sendNewsletter(sendNewsletterRequestDTO),
            "Newsletter je uspešno poslat",
            request.getRequestURI()));
  }
}
