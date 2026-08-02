package com.consi.fitme.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.consi.fitme.dto.NewsletterTemplateDTO;
import com.consi.fitme.dto.request.CreateNewsletterTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateNewsletterTemplateRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.service.NewsletterTemplateService;
import java.util.List;
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
class NewsletterTemplateControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private NewsletterTemplateService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenCallNewsletterTemplateEndpoints_thenReturnsOk() throws Exception {
    NewsletterTemplateDTO templateDto =
        NewsletterTemplateDTO.builder()
            .id(1L)
            .title("Promocija")
            .description("Letnja akcija")
            .htmlContent("<p>Sadržaj</p>")
            .defaultSubject("Naslov")
            .status(Status.ACTIVE)
            .build();

    when(service.getAllTemplates()).thenReturn(List.of(templateDto));
    when(service.getTemplate(1L)).thenReturn(templateDto);
    when(service.createTemplate(any(CreateNewsletterTemplateRequestDTO.class)))
        .thenReturn(templateDto);
    when(service.updateTemplate(anyLong(), any(UpdateNewsletterTemplateRequestDTO.class)))
        .thenReturn(templateDto);
    when(service.deleteTemplate(1L)).thenReturn(new MessageResponseDTO("deleted"));

    mockMvc.perform(get("/api/newsletter-templates")).andExpect(status().isOk());
    mockMvc.perform(get("/api/newsletter-templates/1")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/newsletter-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Promocija",
                      "htmlContent": "<p>Sadržaj</p>"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/api/newsletter-templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Nova promocija"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/newsletter-templates/1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenNonAdmin_whenCallNewsletterTemplateEndpoints_thenReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/newsletter-templates")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/newsletter-templates/1")).andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/newsletter-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Promocija",
                      "htmlContent": "<p>Sadržaj</p>"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/newsletter-templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Nova promocija"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/newsletter-templates/1")).andExpect(status().isForbidden());

    verify(service, never()).getAllTemplates();
    verify(service, never()).getTemplate(anyLong());
    verify(service, never()).createTemplate(any(CreateNewsletterTemplateRequestDTO.class));
    verify(service, never())
        .updateTemplate(anyLong(), any(UpdateNewsletterTemplateRequestDTO.class));
    verify(service, never()).deleteTemplate(anyLong());
  }
}
