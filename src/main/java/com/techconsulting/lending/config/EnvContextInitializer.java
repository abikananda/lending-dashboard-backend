package com.techconsulting.lending.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Early environment loader that runs BEFORE Spring initializes beans.
 * Loads .env file during ApplicationContext initialization phase.
 * This ensures database and other service configurations can access .env variables.
 */
public class EnvContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(EnvContextInitializer.class);

    /**
     * Initialize the application context by loading .env file.
     * This runs very early in Spring startup - before bean creation and database connection.
     *
     * @param context the application context
     */
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        try {
            logger.info("Loading .env file at application initialization phase...");
            Dotenv dotenv = Dotenv.load();

            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                System.setProperty(key, value);
                
                boolean isPassword = key.toLowerCase().contains("password");
                logger.debug("Loaded env: {} = {}", key, isPassword ? "****" : value);
            });

            logger.info("✓ Successfully loaded {} environment variables from .env file", 
                dotenv.entries().size());
        } catch (Exception e) {
            logger.warn("⚠ .env file not found or not readable (using application.yml defaults): {}", 
                e.getMessage());
        }
    }
}
