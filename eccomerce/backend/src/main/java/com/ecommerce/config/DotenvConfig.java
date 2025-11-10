package com.ecommerce.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DotenvConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    @Bean
    public MapPropertySource dotenvPropertySource(Dotenv dotenv, ConfigurableEnvironment environment) {
        Map<String, Object> envMap = new HashMap<>();

        // Cargar todas las variables del .env
        dotenv.entries().forEach(entry -> {
            envMap.put(entry.getKey(), entry.getValue());
            // System.out.println("Loaded env var: " + entry.getKey() + " = " +
            // entry.getValue());
        });

        MapPropertySource propertySource = new MapPropertySource("dotenv", envMap);
        environment.getPropertySources().addFirst(propertySource);

        // System.out.println("Dotenv configuration loaded with " + envMap.size() + "
        // variables");

        return propertySource;
    }
}