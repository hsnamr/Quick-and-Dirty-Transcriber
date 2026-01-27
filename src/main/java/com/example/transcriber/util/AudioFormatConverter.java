package com.example.audiototext.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for audio format conversion
 */
public class AudioFormatConverter {

    private static final Logger logger = LoggerFactory.getLogger(AudioFormatConverter.class);
    private static final int TARGET_SAMPLE_RATE = 16000; // Vosk requires 16kHz
    private static final int TARGET_BITS_PER_SAMPLE = 16;
    private static final int TARGET_CHANNELS = 1; // Mono

    /**
     * Convert audio file to WAV format with target specifications
     * @param inputFile Input audio file
     * @param outputFile Output WAV file
     * @return true if conversion successful
     */
    public static boolean convertToWav(File inputFile, File outputFile) {
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(inputFile);
            AudioFormat sourceFormat = inputStream.getFormat();

            // Create target format
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    TARGET_SAMPLE_RATE,
                    TARGET_BITS_PER_SAMPLE,
                    TARGET_CHANNELS,
                    TARGET_CHANNELS * (TARGET_BITS_PER_SAMPLE / 8),
                    TARGET_SAMPLE_RATE,
                    false
            );

            // Convert if needed
            if (!sourceFormat.matches(targetFormat)) {
                logger.info("Converting audio from {} to target format", sourceFormat);
                inputStream = AudioSystem.getAudioInputStream(targetFormat, inputStream);
            }

            // Write to output file
            AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, outputFile);
            inputStream.close();

            logger.info("Audio conversion completed: {} -> {}", inputFile.getName(), outputFile.getName());
            return true;

        } catch (UnsupportedAudioFileException | IOException e) {
            logger.error("Failed to convert audio file: {}", inputFile.getName(), e);
            return false;
        }
    }

    /**
     * Check if audio file needs conversion
     */
    public static boolean needsConversion(File audioFile) {
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = inputStream.getFormat();
            inputStream.close();

            return format.getSampleRate() != TARGET_SAMPLE_RATE ||
                   format.getSampleSizeInBits() != TARGET_BITS_PER_SAMPLE ||
                   format.getChannels() != TARGET_CHANNELS ||
                   !format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);

        } catch (Exception e) {
            logger.warn("Could not determine if conversion needed", e);
            return true; // Assume conversion needed if we can't determine
        }
    }
}
