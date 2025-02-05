package com.localmarket.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.localmarket.main.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.authorization.AuthorizationDecision;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context
                .requireExplicitSave(false))
            .authorizeHttpRequests(auth -> auth
                // Swagger UI endpoints
                .requestMatchers("/v3/api-docs/**", 
                               "/v3/api-docs.yaml",
                               "/swagger-ui/**",
                               "/swagger-ui.html",
                               "/webjars/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/checkout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/pay").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/orders/**").permitAll()
                .requestMatchers("/api/orders/bundle/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/my-products").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/products/my-pending").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/products/pending").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/{id}/approve").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/{id}/decline").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users").authenticated() //It Can get users by role name
                .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/{id}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/users").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/send-email").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/analytics/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/producer-applications/status").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/producer-orders/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/producer-orders/status/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/change-password").authenticated()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/eligibility/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews/*/approve").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews/*/decline").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews/pending").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders").access((authentication, context) -> {
                    String showall = context.getRequest().getParameter("showall");
                    return new AuthorizationDecision(showall != null ? 
                        authentication.get().isAuthenticated() : 
                        true);
                })
                .requestMatchers(HttpMethod.POST, "/api/coupons/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/coupons/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/coupons/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/coupons/validate/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));  // For testing only
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 