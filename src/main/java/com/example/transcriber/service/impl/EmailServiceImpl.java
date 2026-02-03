package com.example.transcriber.service.impl;

import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.service.EmailService;
import com.example.transcriber.service.UserContact;
import com.example.transcriber.service.UserEmailResolver;
import com.example.transcriber.util.DurationFormatter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Sends email notifications using templates audio_transcription_loading_{lang},
 * audio_transcription_complete_{lang}, audio_transcription_error_{lang} (en/ar).
 * Recipient = user who created the request (resolved via UserEmailResolver).
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final String TEMPLATE_LOADING = "email/audio_transcription_loading_";
    private static final String TEMPLATE_COMPLETE = "email/audio_transcription_complete_";
    private static final String TEMPLATE_ERROR = "email/audio_transcription_error_";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserEmailResolver userEmailResolver;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            UserEmailResolver userEmailResolver) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.userEmailResolver = userEmailResolver;
    }

    @Override
    @Async
    public void sendRequestSentEmail(TranscriptionRequest request) {
        Optional<UserContact> contact = userEmailResolver.resolve(request.getUserId());
        if (contact.isEmpty()) {
            logger.debug("No recipient resolved for user {}; skipping request-sent email for request {}",
                    request.getUserId(), request.getId());
            return;
        }
        UserContact recipient = contact.get();
        String lang = recipient.getTemplateLang();
        String templateName = TEMPLATE_LOADING + lang;
        Map<String, Object> model = buildModel(request, recipient, null);
        String subject = "Audio Transcription Request Sent";
        sendHtmlEmail(recipient.email(), subject, templateName, model, request.getId(), "request-sent");
    }

    @Override
    @Async
    public void sendTranscriptionCompletedEmail(TranscriptionRequest request) {
        Optional<UserContact> contact = userEmailResolver.resolve(request.getUserId());
        if (contact.isEmpty()) {
            logger.debug("No recipient resolved for user {}; skipping completion email for request {}",
                    request.getUserId(), request.getId());
            return;
        }
        UserContact recipient = contact.get();
        String lang = recipient.getTemplateLang();
        String templateName = TEMPLATE_COMPLETE + lang;
        Map<String, Object> model = buildModel(request, recipient, null);
        String subject = "Audio Transcription Completed";
        sendHtmlEmail(recipient.email(), subject, templateName, model, request.getId(), "completion");
    }

    @Override
    @Async
    public void sendTranscriptionFailedEmail(TranscriptionRequest request, String errorMessage) {
        Optional<UserContact> contact = userEmailResolver.resolve(request.getUserId());
        if (contact.isEmpty()) {
            logger.debug("No recipient resolved for user {}; skipping failure email for request {}",
                    request.getUserId(), request.getId());
            return;
        }
        UserContact recipient = contact.get();
        String lang = recipient.getTemplateLang();
        String templateName = TEMPLATE_ERROR + lang;
        Map<String, Object> model = buildModel(request, recipient, errorMessage);
        String subject = "Audio Transcription Failed";
        sendHtmlEmail(recipient.email(), subject, templateName, model, request.getId(), "failure");
    }

    private Map<String, Object> buildModel(TranscriptionRequest request, UserContact recipient, String errorMessage) {
        Map<String, Object> model = new HashMap<>();
        model.put("userName", recipient.name());
        model.put("userEmail", recipient.email());
        model.put("fileName", request.getFileName() != null ? request.getFileName() : "");
        model.put("durationSecs", request.getDurationSecs() != null ? request.getDurationSecs().doubleValue() : 0.0);
        model.put("durationFormatted", DurationFormatter.formatDuration(request.getDurationSecs()));
        model.put("category", request.getCategory() != null ? request.getCategory().getDisplayName() : "");
        model.put("languageName", request.getLanguage() != null ? request.getLanguage().getName() : "");
        model.put("createdAtFormatted", formatDateTime(request.getCreatedAt()));
        if (errorMessage != null) {
            model.put("errorMessage", errorMessage);
        } else {
            model.put("errorMessage", "");
        }
        return model;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> model,
                              String requestId, String emailType) {
        try {
            Context context = new Context();
            model.forEach(context::setVariable);

            String html = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            logger.info("Sent {} email for transcription request {} to {}", emailType, requestId, to);
        } catch (MessagingException e) {
            logger.error("Failed to send {} email for transcription request {} to {}", emailType, requestId, to, e);
        }
    }
}
