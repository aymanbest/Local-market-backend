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



@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendHtmlEmail(  String to, 
                                String subject, 
                                String name, 
                                String body, 
                                MultipartFile attachment) throws MessagingException {

        // Prepare Thymeleaf context
        Context context = new Context();
        context.setVariable("name", name);  
        context.setVariable("subject", subject);
        context.setVariable("body", body); 
    
        // Load and process the HTML template
        String htmlContent = templateEngine.process("email-template", context);
    
        
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
    
        helper.setTo(to);
        helper.setFrom("localMarket@trial-x2p03477y0k4zdrn.mlsender.net");
        helper.setSubject(subject);
        helper.setText(htmlContent, true); 
        
        helper.addInline("logoImage", new ClassPathResource("static/logo.png"));

        // Add attachment if provided
        if (attachment != null && !attachment.isEmpty()) {
            helper.addAttachment(attachment.getOriginalFilename(), attachment);
        }
        
        emailSender.send(message);
    }
    
}


