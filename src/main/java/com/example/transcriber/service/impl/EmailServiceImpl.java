package com.example.transcriber.service.impl;

import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendRequestSentEmail(TranscriptionRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            // Note: In production, get user email from user service
            // For now, using a placeholder
            message.setTo("user@example.com"); // TODO: Get actual user email
            message.setSubject("Audio Transcription Request Sent");
            message.setText(String.format(
                    "Your audio transcription request has been submitted successfully.\n\n" +
                    "File: %s\n" +
                    "Duration: %.1f seconds\n" +
                    "Status: Processing\n\n" +
                    "You will receive an email when the transcription is completed.",
                    request.getFileName(),
                    request.getDurationSecs() != null ? request.getDurationSecs().doubleValue() : 0.0
            ));
            mailSender.send(message);
            logger.info("Sent request sent email for transcription request {}", request.getId());
        } catch (Exception e) {
            logger.error("Failed to send request sent email for transcription request {}", request.getId(), e);
        }
    }

    @Override
    @Async
    public void sendTranscriptionCompletedEmail(TranscriptionRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            // Note: In production, get user email from user service
            message.setTo("user@example.com"); // TODO: Get actual user email
            message.setSubject("Audio Transcription Completed");
            message.setText(String.format(
                    "Your audio transcription has been completed successfully.\n\n" +
                    "File: %s\n" +
                    "Duration: %.1f seconds\n" +
                    "Status: Completed\n\n" +
                    "You can now view the transcription results.",
                    request.getFileName(),
                    request.getDurationSecs() != null ? request.getDurationSecs().doubleValue() : 0.0
            ));
            mailSender.send(message);
            logger.info("Sent completion email for transcription request {}", request.getId());
        } catch (Exception e) {
            logger.error("Failed to send completion email for transcription request {}", request.getId(), e);
        }
    }

    @Override
    @Async
    public void sendTranscriptionFailedEmail(TranscriptionRequest request, String errorMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            // Note: In production, get user email from user service
            message.setTo("user@example.com"); // TODO: Get actual user email
            message.setSubject("Audio Transcription Failed");
            message.setText(String.format(
                    "Unfortunately, your audio transcription request has failed.\n\n" +
                    "File: %s\n" +
                    "Duration: %.1f seconds\n" +
                    "Status: Failed\n" +
                    "Error: %s\n\n" +
                    "Please try again or contact support if the issue persists.",
                    request.getFileName(),
                    request.getDurationSecs() != null ? request.getDurationSecs().doubleValue() : 0.0,
                    errorMessage != null ? errorMessage : "Unknown error"
            ));
            mailSender.send(message);
            logger.info("Sent failure email for transcription request {}", request.getId());
        } catch (Exception e) {
            logger.error("Failed to send failure email for transcription request {}", request.getId(), e);
        }
    }
}
