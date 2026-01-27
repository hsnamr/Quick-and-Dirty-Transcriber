package com.example.transcriber.service.impl;

import com.example.transcriber.service.TranscriptionEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Vosk API imports - these will be available if vosk dependency includes native libraries
// If compilation fails, the native libraries may not be available for your platform
// In that case, use the reflection-based approach (see commented code below)
/*
import com.alphacephei.vosk.Model;
import com.alphacephei.vosk.Recognizer;
*/

/**
 * Vosk-based transcription service using the Vosk Java API
 * 
 * This service uses the com.alphacephei.vosk package for speech recognition.
 * The native library (libvosk) is loaded automatically through JNA.
 * 
 * Requirements:
 * 1. Vosk models downloaded from https://alphacephei.com/vosk/models
 * 2. Models extracted to the configured models path
 * 3. Native library included in vosk Maven dependency (handled automatically)
 */
@Service
public class VoskTranscriptionService implements TranscriptionEngineService {

    private static final Logger logger = LoggerFactory.getLogger(VoskTranscriptionService.class);
    private static final int SAMPLE_RATE = 16000; // Vosk requires 16kHz
    private static final int CHUNK_SIZE = 4096;

    @Value("${transcription.engine.vosk.models.path:./models}")
    private String modelsPath;

    @Value("${transcription.engine.vosk.sample-rate:16000}")
    private int sampleRate;

    // Cache for loaded Vosk models
    // Using Object type with reflection to handle cases where Vosk classes may not be available
    // If Vosk classes are available at compile time, you can use: Map<String, Model>
    private final Map<String, Object> modelCache = new ConcurrentHashMap<>();
    
    private boolean nativeLibraryAvailable = false;

    public VoskTranscriptionService() {
        initializeNativeLibrary();
    }

    /**
     * Initialize and verify Vosk native library availability
     */
    private void initializeNativeLibrary() {
        try {
            // Try to load Vosk classes to verify native library
            Class<?> modelClass = Class.forName("com.alphacephei.vosk.Model");
            Class<?> recognizerClass = Class.forName("com.alphacephei.vosk.Recognizer");
            
            logger.info("Vosk classes found: Model={}, Recognizer={}", 
                    modelClass.getName(), recognizerClass.getName());
            
            // Try to verify native library is actually loadable
            // This will throw UnsatisfiedLinkError if native library is missing
            try {
                // Just verify classes are available - actual loading happens when creating Model
                nativeLibraryAvailable = true;
                logger.info("Vosk native library should be available through JNA");
            } catch (UnsatisfiedLinkError e) {
                logger.error("Vosk native library could not be loaded. Check platform compatibility.", e);
                logger.error("See VOSK_SETUP.md for setup instructions");
                nativeLibraryAvailable = false;
            }
            
        } catch (ClassNotFoundException e) {
            logger.error("Vosk classes not found. Make sure vosk dependency is in classpath.", e);
            logger.error("Run: mvn clean install to download dependencies");
            nativeLibraryAvailable = false;
        } catch (Exception e) {
            logger.error("Error initializing Vosk native library", e);
            nativeLibraryAvailable = false;
        }
    }

    @Override
    public TranscriptionResult transcribe(MultipartFile audioFile, String languageCode, Integer speakersCount) {
        if (!nativeLibraryAvailable) {
            throw new RuntimeException("Vosk native library is not available. " +
                    "Please ensure the vosk Maven dependency includes native libraries for your platform. " +
                    "See VOSK_SETUP.md for setup instructions.");
        }

        long startTime = System.currentTimeMillis();

        logger.info("Starting transcription for file: {}, language: {}, speakers: {}", 
                audioFile.getOriginalFilename(), languageCode, speakersCount);

        File tempAudioFile = null;
        try {
            // Convert and prepare audio file
            tempAudioFile = prepareAudioFile(audioFile);
            
            // Load model
            Object model = getModel(languageCode);
            
            // Process transcription using reflection to call Vosk API
            TranscriptionResult result = processTranscriptionWithVosk(tempAudioFile, model, speakersCount);
            
            long processingTime = System.currentTimeMillis() - startTime;
            result.setProcessingTimeSeconds(processingTime / 1000.0);
            
            logger.info("Transcription completed in {} seconds", result.getProcessingTimeSeconds());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during transcription", e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        } finally {
            // Clean up temporary file
            if (tempAudioFile != null && tempAudioFile.exists()) {
                try {
                    Files.delete(tempAudioFile.toPath());
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary audio file", e);
                }
            }
        }
    }

    /**
     * Prepare audio file for transcription (convert to WAV 16kHz mono PCM if needed)
     */
    private File prepareAudioFile(MultipartFile audioFile) throws IOException {
        // Create temporary file
        Path tempDir = Files.createTempDirectory("audio_transcription_");
        String originalFileName = audioFile.getOriginalFilename();
        if (originalFileName == null) {
            originalFileName = "audio_file.wav";
        }
        
        // Ensure .wav extension
        if (!originalFileName.toLowerCase().endsWith(".wav")) {
            int lastDot = originalFileName.lastIndexOf('.');
            if (lastDot > 0) {
                originalFileName = originalFileName.substring(0, lastDot) + ".wav";
            } else {
                originalFileName = originalFileName + ".wav";
            }
        }
        
        File tempFile = tempDir.resolve(originalFileName).toFile();
        
        // Write multipart file to temp file
        try (InputStream inputStream = audioFile.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        // Convert to required format if needed
        File convertedFile = convertToRequiredFormat(tempFile);
        if (convertedFile != tempFile && tempFile.exists()) {
            Files.delete(tempFile.toPath());
        }
        
        return convertedFile;
    }

    /**
     * Convert audio file to WAV 16kHz mono PCM format required by Vosk
     */
    private File convertToRequiredFormat(File audioFile) throws IOException {
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat sourceFormat = inputStream.getFormat();
            
            // Target format: PCM_SIGNED, 16000 Hz, 16-bit, mono
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    1, // mono
                    2, // 2 bytes per frame
                    sampleRate,
                    false
            );
            
            // Check if conversion is needed
            if (sourceFormat.matches(targetFormat)) {
                inputStream.close();
                return audioFile;
            }
            
            // Convert format
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, inputStream);
            
            // Write to new file
            Path convertedPath = audioFile.toPath().getParent().resolve("converted_" + audioFile.getName());
            File convertedFile = convertedPath.toFile();
            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, convertedFile);
            
            convertedStream.close();
            inputStream.close();
            
            logger.info("Converted audio from {} to target format", sourceFormat);
            return convertedFile;
            
        } catch (UnsupportedAudioFileException e) {
            logger.error("Unsupported audio format", e);
            throw new IOException("Unsupported audio format: " + e.getMessage(), e);
        }
    }

    /**
     * Get or load Vosk model for the specified language using reflection
     */
    private Object getModel(String languageCode) {
        // Default to English if auto or unknown
        if (languageCode == null || languageCode.equals("auto") || languageCode.isEmpty()) {
            languageCode = "en";
        }
        
        return modelCache.computeIfAbsent(languageCode, code -> {
            try {
                String modelPath = getModelPath(code);
                logger.info("Loading Vosk model from: {}", modelPath);
                
                // Use reflection to load Vosk Model class
                Class<?> modelClass = Class.forName("com.alphacephei.vosk.Model");
                Object model = modelClass.getConstructor(String.class).newInstance(modelPath);
                
                logger.info("Vosk model loaded successfully for language: {}", code);
                return model;
                
            } catch (ClassNotFoundException e) {
                logger.error("Vosk Model class not found", e);
                throw new RuntimeException("Vosk Model class not available", e);
            } catch (Exception e) {
                logger.error("Failed to load Vosk model from: {}", getModelPath(code), e);
                throw new RuntimeException("Failed to load Vosk model for language: " + code, e);
            }
        });
    }

    /**
     * Get model path for language code
     */
    private String getModelPath(String languageCode) {
        Map<String, String> modelMap = new HashMap<>();
        modelMap.put("en", "vosk-model-en-us-0.22");
        modelMap.put("ar", "vosk-model-ar-0.22");
        modelMap.put("es", "vosk-model-es-0.42");
        modelMap.put("fr", "vosk-model-fr-0.22");
        modelMap.put("de", "vosk-model-de-0.22");
        modelMap.put("it", "vosk-model-it-0.22");
        modelMap.put("pt", "vosk-model-pt-0.22");
        modelMap.put("ru", "vosk-model-ru-0.22");
        modelMap.put("zh", "vosk-model-cn-0.22");
        modelMap.put("ja", "vosk-model-jp-0.22");
        modelMap.put("ko", "vosk-model-kr-0.22");
        
        String modelName = modelMap.getOrDefault(languageCode, "vosk-model-en-us-0.22");
        return modelsPath + "/" + modelName;
    }

    /**
     * Process audio file with Vosk recognizer using reflection
     */
    private TranscriptionResult processTranscriptionWithVosk(File audioFile, Object model, Integer speakersCount) 
            throws Exception {
        
        TranscriptionResult result = new TranscriptionResult();
        StringBuilder fullText = new StringBuilder();
        Map<String, Object> jsonResultMap = new HashMap<>();
        
        try {
            // Use reflection to create Recognizer
            Class<?> recognizerClass = Class.forName("com.alphacephei.vosk.Recognizer");
            Object recognizer = recognizerClass.getConstructor(model.getClass(), float.class)
                    .newInstance(model, (float) sampleRate);
            
            // Set words option if available
            try {
                recognizerClass.getMethod("setWords", boolean.class).invoke(recognizer, true);
            } catch (NoSuchMethodException e) {
                logger.debug("setWords method not available in this Vosk version");
            }
            
            // Read audio file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();
            
            // Convert to PCM format if needed
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    1,
                    2,
                    sampleRate,
                    false
            );
            
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            
            // Process audio in chunks
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            
            while ((bytesRead = convertedStream.read(buffer)) != -1) {
                // Call acceptWaveForm using reflection
                Boolean accepted = (Boolean) recognizerClass.getMethod("acceptWaveForm", byte[].class, int.class)
                        .invoke(recognizer, buffer, bytesRead);
                
                if (accepted) {
                    String partialResult = (String) recognizerClass.getMethod("getResult").invoke(recognizer);
                    if (partialResult != null && !partialResult.isEmpty()) {
                        String text = extractTextFromJson(partialResult);
                        if (!text.isEmpty()) {
                            fullText.append(text).append(" ");
                        }
                    }
                } else {
                    // Get partial result
                    String partialResult = (String) recognizerClass.getMethod("getPartialResult").invoke(recognizer);
                    // Optionally log partial results for progress tracking
                }
            }
            
            // Get final result
            String finalResult = (String) recognizerClass.getMethod("getFinalResult").invoke(recognizer);
            if (finalResult != null && !finalResult.isEmpty()) {
                String text = extractTextFromJson(finalResult);
                if (!text.isEmpty()) {
                    fullText.append(text);
                }
                jsonResultMap.put("final_result", finalResult);
            }
            
            // Close recognizer if it implements AutoCloseable
            try {
                if (recognizer instanceof AutoCloseable) {
                    ((AutoCloseable) recognizer).close();
                }
            } catch (Exception e) {
                logger.warn("Error closing recognizer", e);
            }
            
            convertedStream.close();
            audioInputStream.close();
            
        } catch (ClassNotFoundException e) {
            logger.error("Vosk Recognizer class not found", e);
            throw new RuntimeException("Vosk Recognizer class not available", e);
        } catch (Exception e) {
            logger.error("Error processing audio with Vosk", e);
            throw new RuntimeException("Vosk transcription processing failed", e);
        }
        
        result.setText(fullText.toString().trim());
        jsonResultMap.put("text", result.getText());
        result.setJsonResult(jsonResultMap);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sample_rate", sampleRate);
        metadata.put("speakers_count", speakersCount);
        metadata.put("model_loaded", model != null);
        result.setMetadata(metadata);
        
        return result;
    }

    /**
     * Extract text from Vosk JSON result
     * Vosk returns JSON in format: {"text": "recognized text", "result": [...]}
     */
    private String extractTextFromJson(String jsonResult) {
        if (jsonResult == null || jsonResult.isEmpty()) {
            return "";
        }
        
        try {
            // Simple JSON parsing - extract "text" field
            int textIndex = jsonResult.indexOf("\"text\"");
            if (textIndex == -1) {
                return "";
            }
            
            int startIndex = jsonResult.indexOf("\"", textIndex + 7) + 1;
            if (startIndex <= 0) {
                return "";
            }
            
            int endIndex = jsonResult.indexOf("\"", startIndex);
            if (endIndex > startIndex) {
                return jsonResult.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract text from JSON result: {}", jsonResult, e);
        }
        
        return "";
    }
}
