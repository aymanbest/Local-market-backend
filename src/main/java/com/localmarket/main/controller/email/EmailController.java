package com.localmarket.main.controller.email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.service.email.EmailService;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
@RestController
@Tag(name = "Email", description = "Email APIs")
@RequiredArgsConstructor
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService; 

    
    @GetMapping("/api/send-email")
    public ResponseEntity<String> sendEmail(
            HttpServletRequest request, 
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body,
            @RequestPart(required = false) MultipartFile attachment) {

        try {

            String[]  userName = to.split("@");
            emailService.sendHtmlEmail(to, subject, userName[0], body, attachment);

            return ResponseEntity.ok("Email Sent!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send email: " + e.getMessage());
        }
    }
}
