package com.techconsulting.lending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class LendingDashboardApplication {
  public static void main(String[] args) { SpringApplication.run(LendingDashboardApplication.class, args); }
}
