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

import com.consi.fitme.dto.TerminTemplateDTO;
import com.consi.fitme.dto.request.CreateTerminTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminTemplateRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.service.TerminTemplateService;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
class TerminTemplateControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private TerminTemplateService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenCallTerminTemplateEndpoints_thenReturnsOk() throws Exception {
    TerminTemplateDTO templateDto =
        TerminTemplateDTO.builder()
            .id(1L)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .status(Status.ACTIVE)
            .build();

    when(service.getAllTerminTemplates()).thenReturn(List.of(templateDto));
    when(service.getTerminTemplate(1L)).thenReturn(templateDto);
    when(service.createTerminTemplate(any(CreateTerminTemplateRequestDTO.class)))
        .thenReturn(templateDto);
    when(service.updateTerminTemplate(anyLong(), any(UpdateTerminTemplateRequestDTO.class)))
        .thenReturn(templateDto);
    when(service.deleteTerminTemplate(1L)).thenReturn(new MessageResponseDTO("deleted"));

    mockMvc.perform(get("/api/termin-templates")).andExpect(status().isOk());
    mockMvc.perform(get("/api/termin-templates/1")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/termin-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "09:00:00",
                      "endTime": "10:00:00"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/api/termin-templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "INACTIVE"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/termin-templates/1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenNonAdmin_whenCallTerminTemplateEndpoints_thenReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/termin-templates")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/termin-templates/1")).andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/termin-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "09:00:00",
                      "endTime": "10:00:00"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/termin-templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "INACTIVE"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/termin-templates/1")).andExpect(status().isForbidden());

    verify(service, never()).getAllTerminTemplates();
    verify(service, never()).getTerminTemplate(anyLong());
    verify(service, never()).createTerminTemplate(any());
    verify(service, never()).updateTerminTemplate(anyLong(), any());
    verify(service, never()).deleteTerminTemplate(anyLong());
  }
}
