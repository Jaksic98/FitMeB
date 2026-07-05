package com.consi.fitme.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class AuthControllerIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  void givenMissingPhoneNumber_whenRegister_thenValidationFails() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "itest.register.nophone.%s",
                      "fullName": "Integration Register No Phone",
                      "email": "itest.register.nophone.%s@fitme.com",
                      "password": "itest.register.fitme123!"
                    }
                    """
                        .formatted(seed, seed)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void givenBlankPhoneNumber_whenRegister_thenValidationFails() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "itest.register.blankphone.%s",
                      "fullName": "Integration Register Blank Phone",
                      "email": "itest.register.blankphone.%s@fitme.com",
                      "phoneNumber": "   ",
                      "password": "itest.register.fitme123!"
                    }
                    """
                        .formatted(seed, seed)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void givenInvalidPhoneFormat_whenRegister_thenValidationFails() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "itest.register.invalidphone.%s",
                      "fullName": "Integration Register Invalid Phone",
                      "email": "itest.register.invalidphone.%s@fitme.com",
                      "phoneNumber": "abc",
                      "password": "itest.register.fitme123!"
                    }
                    """
                        .formatted(seed, seed)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void givenValidRegistrationRequest_whenRegister_thenSucceeds() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "itest.register.valid.%s",
                      "fullName": "Integration Register Valid",
                      "email": "itest.register.valid.%s@fitme.com",
                      "phoneNumber": "+3816%s1",
                      "password": "itest.register.fitme123!"
                    }
                    """
                        .formatted(seed, seed, seed)))
        .andExpect(status().isOk());
  }

  @Test
  void givenMissingPhoneNumber_whenSendOtp_thenValidationFails() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/phone/send-otp").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void givenValidPhoneNumber_whenSendOtp_thenReturnsOk() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "phoneNumber": "+3816%s9"
                    }
                    """
                        .formatted(seed)))
        .andExpect(status().isOk());
  }

  @Test
  void givenNonSixDigitCode_whenVerifyOtp_thenValidationFails() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/phone/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "phoneNumber": "+3816%s9",
                      "code": "12345"
                    }
                    """
                        .formatted(seed)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void givenAlphabeticCode_whenVerifyOtp_thenValidationFails() throws Exception {
    String seed = String.valueOf(System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/auth/phone/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "phoneNumber": "+3816%s9",
                      "code": "abcdef"
                    }
                    """
                        .formatted(seed)))
        .andExpect(status().isBadRequest());
  }
}
