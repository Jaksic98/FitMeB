package com.consi.fitme.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class AppClock {

  public static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");

  private AppClock() {}

  public static LocalDateTime now() {
    return LocalDateTime.now(ZONE);
  }

  public static LocalDate today() {
    return LocalDate.now(ZONE);
  }
}
