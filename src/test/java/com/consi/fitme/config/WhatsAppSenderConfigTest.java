package com.consi.fitme.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.consi.fitme.service.InfobipWhatsAppSender;
import com.consi.fitme.service.MetaCloudApiWhatsAppSender;
import com.consi.fitme.service.NoopWhatsAppSender;
import com.consi.fitme.service.WhatsAppSender;
import org.junit.jupiter.api.Test;

class WhatsAppSenderConfigTest {

  private final WhatsAppSenderConfig config = new WhatsAppSenderConfig();

  @Test
  void whatsAppSender_providerInfobipWithCredentials_returnsInfobipSender() {
    WhatsAppSender sender =
        config.whatsAppSender("infobip", "api-key", "base-url", "sender", "", "", "v21.0");

    assertThat(sender).isInstanceOf(InfobipWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_providerInfobipWithBlankApiKey_fallsBackToNoop() {
    WhatsAppSender sender =
        config.whatsAppSender("infobip", "", "base-url", "sender", "", "", "v21.0");

    assertThat(sender).isInstanceOf(NoopWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_providerMetaWithCredentials_returnsMetaSender() {
    WhatsAppSender sender =
        config.whatsAppSender("meta", "", "", "", "access-token", "phone-number-id", "v21.0");

    assertThat(sender).isInstanceOf(MetaCloudApiWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_providerMetaWithBlankAccessToken_fallsBackToNoop() {
    WhatsAppSender sender =
        config.whatsAppSender("meta", "", "", "", "", "phone-number-id", "v21.0");

    assertThat(sender).isInstanceOf(NoopWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_providerMetaWithBlankPhoneNumberId_fallsBackToNoop() {
    WhatsAppSender sender = config.whatsAppSender("meta", "", "", "", "access-token", "", "v21.0");

    assertThat(sender).isInstanceOf(NoopWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_providerNoop_returnsNoopSender() {
    WhatsAppSender sender = config.whatsAppSender("noop", "", "", "", "", "", "v21.0");

    assertThat(sender).isInstanceOf(NoopWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_unknownProvider_fallsBackToNoop() {
    WhatsAppSender sender = config.whatsAppSender("unknown", "", "", "", "", "", "v21.0");

    assertThat(sender).isInstanceOf(NoopWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_blankProvider_fallsBackToNoop() {
    WhatsAppSender sender = config.whatsAppSender("", "", "", "", "", "", "v21.0");

    assertThat(sender).isInstanceOf(NoopWhatsAppSender.class);
  }

  @Test
  void whatsAppSender_providerCaseInsensitiveAndTrimmed_stillResolves() {
    WhatsAppSender sender =
        config.whatsAppSender("  Meta  ", "", "", "", "access-token", "phone-number-id", "v21.0");

    assertThat(sender).isInstanceOf(MetaCloudApiWhatsAppSender.class);
  }
}
