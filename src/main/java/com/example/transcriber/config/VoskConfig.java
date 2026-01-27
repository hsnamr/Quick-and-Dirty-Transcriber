package com.example.transcriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Configuration for Vosk native library loading
 * 
 * Vosk uses JNA (Java Native Access) to load native libraries automatically.
 * This configuration ensures the native library path is set correctly.
 */
@Configuration
public class VoskConfig {

    private static final Logger logger = LoggerFactory.getLogger(VoskConfig.class);

    @Value("${transcription.engine.vosk.models.path:./models}")
    private String modelsPath;

    @PostConstruct
    public void initialize() {
        // Ensure models directory exists
        File modelsDir = new File(modelsPath);
        if (!modelsDir.exists()) {
            logger.warn("Vosk models directory does not exist: {}. Creating directory.", modelsPath);
            boolean created = modelsDir.mkdirs();
            if (!created) {
                logger.error("Failed to create Vosk models directory: {}", modelsPath);
            }
        } else {
            logger.info("Vosk models directory found: {}", modelsPath);
        }

        // Log native library information
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        logger.info("Vosk native library will be loaded for OS: {}, Architecture: {}", osName, osArch);
        
        // Vosk uses JNA which automatically loads the native library
        // The library should be included in the vosk Maven dependency
        try {
            // Verify native library availability using VoskNativeLibraryLoader
            boolean available = com.example.transcriber.util.VoskNativeLibraryLoader.isLibraryAvailable();
            if (available) {
                logger.info("Vosk native library is available and ready to use");
            } else {
                logger.warn("Vosk native library may not be available. Check VOSK_SETUP.md for setup instructions.");
            }
        } catch (Exception e) {
            logger.warn("Could not verify Vosk native library: {}", e.getMessage());
        }
    }
}
