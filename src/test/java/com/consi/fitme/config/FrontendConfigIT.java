package com.consi.fitme.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class FrontendConfigIT {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @Test
  void givenSingleSegmentRoute_whenGet_thenForwardsToIndexHtml() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenNestedAdminRoute_whenGet_thenForwardsToIndexHtml() throws Exception {
    mockMvc
        .perform(get("/admin/raspored"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void givenDeeplyNestedRoute_whenGet_thenForwardsToIndexHtml() throws Exception {
    mockMvc
        .perform(get("/admin/rezervacije/detalji"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }
}
