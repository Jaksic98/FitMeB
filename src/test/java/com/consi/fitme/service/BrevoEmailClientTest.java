package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoEmailClientTest {

  @Test
  void send_postsToBrevoSmtpEndpointWithApiKeyAndPayload() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server
        .expect(requestTo("https://api.brevo.com/v3/smtp/email"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("api-key", "test-api-key"))
        .andExpect(jsonPath("$.sender.email").value("noreply@pilates.fitme.rs"))
        .andExpect(jsonPath("$.sender.name").value("FitMe Pilates"))
        .andExpect(jsonPath("$.to[0].email").value("client@example.com"))
        .andExpect(jsonPath("$.subject").value("Vaš verifikacioni kod — FitMe"))
        .andExpect(jsonPath("$.htmlContent").value("<p>123456</p>"))
        .andRespond(withSuccess());

    BrevoEmailClient client =
        new BrevoEmailClient("test-api-key", "noreply@pilates.fitme.rs", "FitMe Pilates", builder);

    client.send("client@example.com", "Vaš verifikacioni kod — FitMe", "<p>123456</p>");

    server.verify();
  }

  @Test
  void send_throwsEmailSendExceptionOnHttpError() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withServerError());

    BrevoEmailClient client =
        new BrevoEmailClient("test-api-key", "noreply@pilates.fitme.rs", "FitMe Pilates", builder);

    assertThatThrownBy(() -> client.send("client@example.com", "Subject", "<p>Body</p>"))
        .isInstanceOf(EmailSendException.class);

    server.verify();
  }
}
