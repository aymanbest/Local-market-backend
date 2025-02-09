package com.localmarket.main.service.email;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;



@Service
public class EmailService {

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
            Map<String, Object> templateVariables) throws MessagingException {
        
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("subject", subject);
        
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
    }
    
}


