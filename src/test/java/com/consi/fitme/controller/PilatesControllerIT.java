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

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.UpdatePilatesRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.model.Status;
import com.consi.fitme.service.PilatesService;
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
class PilatesControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private PilatesService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenCallPilatesEndpoints_thenReturnsOk() throws Exception {
    PilatesDTO pilatesDto =
        PilatesDTO.builder().id(1L).position("A1").name("Reformer").status(Status.ACTIVE).build();

    when(service.getAllPilates()).thenReturn(List.of(pilatesDto));
    when(service.getPilates(1L)).thenReturn(pilatesDto);
    when(service.createPilates(any(CreatePilatesRequestDTO.class))).thenReturn(pilatesDto);
    when(service.updatePilates(anyLong(), any(UpdatePilatesRequestDTO.class)))
        .thenReturn(pilatesDto);
    when(service.deletePilates(1L)).thenReturn(new MessageResponseDTO("deleted"));

    mockMvc.perform(get("/api/pilates")).andExpect(status().isOk());
    mockMvc.perform(get("/api/pilates/1")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/pilates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "position": "A1",
                      "name": "Reformer"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/api/pilates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Cadillac"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/pilates/1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenNonAdmin_whenCallPilatesEndpoints_thenReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/pilates")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/pilates/1")).andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/pilates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "position": "A1",
                      "name": "Reformer"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/pilates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Cadillac"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/pilates/1")).andExpect(status().isForbidden());

    verify(service, never()).getAllPilates();
    verify(service, never()).getPilates(anyLong());
    verify(service, never()).createPilates(any(CreatePilatesRequestDTO.class));
    verify(service, never()).updatePilates(anyLong(), any(UpdatePilatesRequestDTO.class));
    verify(service, never()).deletePilates(anyLong());
  }
}
