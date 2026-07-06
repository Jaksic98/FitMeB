package com.consi.fitme.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MetaCloudApiWhatsAppSenderTest {

  @Test
  void sendTemplate_postsToGraphApiWithBearerAuthAndTemplatePayload() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server
        .expect(requestTo("https://graph.facebook.com/v21.0/1234567890/messages"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-token"))
        .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
        .andExpect(jsonPath("$.to").value("381601234567"))
        .andExpect(jsonPath("$.type").value("template"))
        .andExpect(jsonPath("$.template.name").value("fitme_otp"))
        .andExpect(jsonPath("$.template.language.code").value("sr"))
        .andExpect(jsonPath("$.template.components[0].type").value("body"))
        .andExpect(jsonPath("$.template.components[0].parameters[0].type").value("text"))
        .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("123456"))
        .andRespond(withSuccess());

    MetaCloudApiWhatsAppSender sender =
        new MetaCloudApiWhatsAppSender(
            "test-token", "1234567890", "https://graph.facebook.com", "v21.0", builder);

    sender.sendTemplate("381601234567", "fitme_otp", List.of("123456"));

    server.verify();
  }

  @Test
  void sendTemplate_swallowsExceptionOnHttpError() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server
        .expect(requestTo("https://graph.facebook.com/v21.0/1234567890/messages"))
        .andRespond(withServerError());

    MetaCloudApiWhatsAppSender sender =
        new MetaCloudApiWhatsAppSender(
            "test-token", "1234567890", "https://graph.facebook.com", "v21.0", builder);

    sender.sendTemplate("381601234567", "fitme_otp", List.of("123456"));

    server.verify();
  }
}
