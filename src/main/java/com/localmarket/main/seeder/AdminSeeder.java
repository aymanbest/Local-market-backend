package com.localmarket.main.seeder;

import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;
    
    @Value("${app.admin.firstname}")
    private String adminFirstname;
    
    @Value("${app.admin.lastname}")
    private String adminLastname;
    
    @Value("${app.admin.email}")
    private String adminEmail;
    
    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.findByEmail(adminEmail).isPresent()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setFirstname(adminFirstname);
            admin.setLastname(adminLastname);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setLastLogin(LocalDateTime.now());
            admin.setRole(Role.ADMIN);
            
            userRepository.save(admin);
        }
    }
} 