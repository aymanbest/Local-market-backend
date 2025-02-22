package com.localmarket.main.service.email;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendHtmlEmail(
            String to, 
            String subject, 
            String name, 
            String templateName,
            MultipartFile attachment,
            Map<String, Object> templateVariables) {
        
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("subject", subject);
            context.setVariable("frontendUrl", frontendUrl);
            
            if (templateVariables != null) {
                templateVariables.forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process(templateName, context);
            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            if (attachment != null) {
                helper.addAttachment(attachment.getOriginalFilename(), attachment);
            }
            
            emailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}: {}", to, e.getMessage());
        }
    }
    
}


