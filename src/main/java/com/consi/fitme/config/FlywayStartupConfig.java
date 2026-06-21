package com.consi.fitme.config;

import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class FlywayStartupConfig {

  private static final Logger logger = LoggerFactory.getLogger(FlywayStartupConfig.class);

  @Bean
  public ApplicationRunner flywayRunner(DataSource dataSource) {
    return args -> {
      Flyway flyway =
          Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
      MigrationInfo[] pending = flyway.info().pending();
      logger.info("Flyway pending migrations: {}", pending.length);
      if (pending.length > 0) {
        logger.info(
            "Pending versions: {}",
            Arrays.stream(pending)
                .map(info -> info.getVersion() + "__" + info.getDescription())
                .toList());
      }
      flyway.migrate();
    };
  }
}
