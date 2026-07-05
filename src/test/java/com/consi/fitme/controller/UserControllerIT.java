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

import com.consi.fitme.dto.UserDTO;
import com.consi.fitme.dto.request.CreateUserRequestDTO;
import com.consi.fitme.dto.request.UpdateUserRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.dto.response.PagingResponseDTO;
import com.consi.fitme.model.Role;
import com.consi.fitme.model.Status;
import com.consi.fitme.service.UserService;
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
class UserControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private UserService service;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdmin_whenCallUserEndpoints_thenReturnsOk() throws Exception {
    UserDTO userDto =
        UserDTO.builder()
            .id(1L)
            .username("admin.user")
            .fullName("Admin User")
            .email("admin.user@fitme.com")
            .status(Status.ACTIVE)
            .roles(List.of(Role.ADMIN))
            .build();

    PagingResponseDTO<UserDTO> pagingResponse =
        new PagingResponseDTO<>(List.of(userDto), 1, 1, 10, 0, false);

    when(service.getUsers(any())).thenReturn(pagingResponse);
    when(service.getUser(1L)).thenReturn(userDto);
    when(service.createUser(any(CreateUserRequestDTO.class))).thenReturn(userDto);
    when(service.updateUser(anyLong(), any(UpdateUserRequestDTO.class))).thenReturn(userDto);
    when(service.deleteUser(1L)).thenReturn(new MessageResponseDTO("deleted"));

    mockMvc.perform(get("/api/users")).andExpect(status().isOk());
    mockMvc.perform(get("/api/users/1")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "new.user",
                      "fullName": "New User",
                      "email": "new.user@fitme.com",
                      "phoneNumber": "+381601234567",
                      "org_unit_id": 1,
                      "password": "Aa1@aaaa",
                      "roles": ["CLIENT"]
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "ACTIVE"
                    }
                    """))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/users/1")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void givenNonAdmin_whenCallUserEndpoints_thenReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/users/1")).andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "new.user",
                      "fullName": "New User",
                      "email": "new.user@fitme.com",
                      "phoneNumber": "+381601234567",
                      "org_unit_id": 1,
                      "password": "Aa1@aaaa",
                      "roles": ["CLIENT"]
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "ACTIVE"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());

    verify(service, never()).getUsers(any());
    verify(service, never()).getUser(anyLong());
    verify(service, never()).createUser(any(CreateUserRequestDTO.class));
    verify(service, never()).updateUser(anyLong(), any(UpdateUserRequestDTO.class));
    verify(service, never()).deleteUser(anyLong());
  }
}
