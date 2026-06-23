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

import com.consi.fitme.dto.TerminDTO;
import com.consi.fitme.dto.request.CreateTerminRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.service.TerminService;
import java.time.LocalDate;
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
class TerminControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private TerminService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenCallTerminEndpoints_thenReturnsOk() throws Exception {
    TerminDTO terminDto =
        TerminDTO.builder()
            .id(1L)
            .date(LocalDate.of(2026, 7, 1))
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .status(Status.ACTIVE)
            .build();

    when(service.getAllTermini()).thenReturn(List.of(terminDto));
    when(service.getTermin(1L)).thenReturn(terminDto);
    when(service.createTermin(any(CreateTerminRequestDTO.class))).thenReturn(terminDto);
    when(service.updateTermin(anyLong(), any(UpdateTerminRequestDTO.class))).thenReturn(terminDto);
    when(service.deleteTermin(1L)).thenReturn(new MessageResponseDTO("deleted"));

    mockMvc.perform(get("/api/termini")).andExpect(status().isOk());
    mockMvc.perform(get("/api/termini/1")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/termini")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "date": "2026-07-01",
                      "startTime": "09:00:00",
                      "endTime": "10:00:00"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/api/termini/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "INACTIVE"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/termini/1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenNonAdmin_whenCallTerminEndpoints_thenReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/termini")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/termini/1")).andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/termini")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "date": "2026-07-01",
                      "startTime": "09:00:00",
                      "endTime": "10:00:00"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/termini/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "INACTIVE"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/termini/1")).andExpect(status().isForbidden());

    verify(service, never()).getAllTermini();
    verify(service, never()).getTermin(anyLong());
    verify(service, never()).createTermin(any(CreateTerminRequestDTO.class));
    verify(service, never()).updateTermin(anyLong(), any(UpdateTerminRequestDTO.class));
    verify(service, never()).deleteTermin(anyLong());
  }
}
