package com.consi.fitme.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.consi.fitme.dto.NewsletterSendResultDTO;
import com.consi.fitme.service.NewsletterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class NewsletterControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private NewsletterService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenSendNewsletter_thenReturnsOk() throws Exception {
    when(service.sendNewsletter(eq("Naslov"), eq("<p>Sadržaj</p>")))
        .thenReturn(
            NewsletterSendResultDTO.builder()
                .totalRecipients(2)
                .sentCount(2)
                .failedCount(0)
                .build());

    mockMvc
        .perform(
            post("/api/newsletter/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "subject": "Naslov",
                      "htmlContent": "<p>Sadržaj</p>"
                    }
                    """))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenBlankSubject_whenSendNewsletter_thenValidationFails() throws Exception {
    mockMvc
        .perform(
            post("/api/newsletter/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "subject": "",
                      "htmlContent": "<p>Sadržaj</p>"
                    }
                    """))
        .andExpect(status().isBadRequest());

    verify(service, never()).sendNewsletter(any(), any());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenNonAdmin_whenSendNewsletter_thenReturnsForbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/newsletter/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "subject": "Naslov",
                      "htmlContent": "<p>Sadržaj</p>"
                    }
                    """))
        .andExpect(status().isForbidden());

    verify(service, never()).sendNewsletter(any(), any());
  }
}
