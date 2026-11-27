package com.techconsulting.lending;

import com.techconsulting.lending.config.EnvContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;


import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class LendingDashboardApplication {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(LendingDashboardApplication.class);
    app.addInitializers(new EnvContextInitializer());
    app.run(args);
  }
}
