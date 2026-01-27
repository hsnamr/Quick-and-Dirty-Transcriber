package com.example.transcriber.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.transcriber.repository")
@EnableMongoAuditing
public class DatabaseConfig {
    // MongoDB configuration
    // Connection details are configured via application.properties
}
