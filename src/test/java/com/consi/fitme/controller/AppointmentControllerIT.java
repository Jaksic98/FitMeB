package com.consi.fitme.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.consi.fitme.dto.AppointmentDTO;
import com.consi.fitme.dto.request.BookAppointmentRequestDTO;
import com.consi.fitme.dto.request.UpdateAppointmentRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.model.AppointmentStatus;
import com.consi.fitme.service.AppointmentService;
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
class AppointmentControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private AppointmentService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenCallAdminOnlyEndpoints_thenReturnsOk() throws Exception {
    AppointmentDTO appointmentDto =
        AppointmentDTO.builder()
            .id(1L)
            .terminId(10L)
            .pilatesId(20L)
            .status(AppointmentStatus.AVAILABLE)
            .build();

    when(service.getAllAppointments()).thenReturn(List.of(appointmentDto));
    when(service.getAppointment(1L)).thenReturn(appointmentDto);
    when(service.deleteAppointment(1L)).thenReturn(new MessageResponseDTO("deleted"));

    mockMvc.perform(get("/api/appointments")).andExpect(status().isOk());
    mockMvc.perform(get("/api/appointments/1")).andExpect(status().isOk());
    mockMvc.perform(delete("/api/appointments/1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenClient_whenCallAdminOnlyEndpoints_thenReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/appointments")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/appointments/1")).andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/appointments/1")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenClient_whenCallSharedEndpoints_thenReturnsOk() throws Exception {
    AppointmentDTO appointmentDto =
        AppointmentDTO.builder()
            .id(1L)
            .terminId(10L)
            .pilatesId(20L)
            .status(AppointmentStatus.AVAILABLE)
            .build();

    when(service.getAvailableAppointments(null)).thenReturn(List.of(appointmentDto));
    when(service.getByUserId(5L)).thenReturn(List.of(appointmentDto));
    when(service.bookAppointment(any(BookAppointmentRequestDTO.class))).thenReturn(appointmentDto);
    when(service.updateAppointment(anyLong(), any(UpdateAppointmentRequestDTO.class)))
        .thenReturn(appointmentDto);

    mockMvc.perform(get("/api/appointments/available")).andExpect(status().isOk());
    mockMvc.perform(get("/api/appointments/user/5")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "appointmentId": 1
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc
        .perform(put("/api/appointments/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
  }

  @Test
  void givenUnauthenticated_whenCallSharedEndpoints_thenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/appointments/available")).andExpect(status().isUnauthorized());
  }
}
