package com.techconsulting.lending.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Environment post processor to load environment variables from .env file.
 * This loads .env file into Spring's environment before application context is created.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
  private static final Logger logger = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
  private static final String ENV_FILE = ".env";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    try {
      File envFile = new File(ENV_FILE);
      if (!envFile.exists()) {
        logger.debug(".env file not found at {}, using system environment variables", ENV_FILE);
        return;
      }

      Properties props = new Properties();
      try (FileInputStream fis = new FileInputStream(envFile)) {
        props.load(fis);
      }

      Map<String, Object> dotenvMap = new HashMap<>();
      for (String key : props.stringPropertyNames()) {
        String value = props.getProperty(key);
        dotenvMap.put(key, value);
        logger.debug("Loaded environment variable: {}", key);
      }

      if (!dotenvMap.isEmpty()) {
        environment.getPropertySources()
            .addFirst(new MapPropertySource("dotenv", dotenvMap));
        logger.info("Successfully loaded {} environment variables from .env file", dotenvMap.size());
      }
    } catch (IOException e) {
      logger.warn("Failed to load .env file. Using system environment variables instead.", e);
    }
  }
}

