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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import com.localmarket.main.security.JwtAuthenticationFilter;
import com.localmarket.main.entity.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final String PRODUCER = Role.PRODUCER.name();
    private final String ADMIN = Role.ADMIN.name();
    private final String CUSTOMER = Role.CUSTOMER.name();

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);
        
        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF protection
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                    "/api/auth/**",
                    "/api/orders/checkout",
                    "/api/orders/pay",
                    "/api/orders/bundle/**",
                    "/ws/**"
                ))
            
            // Session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Security context
            .securityContext(context -> context
                .requireExplicitSave(false))
            
            // Security headers to prevent attacks xss
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "img-src 'self' data: https:; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "connect-src 'self' ws: wss:;"
                ))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frame -> frame.deny())) // prevent clickjacking attacks 
                
            
            // Request authorization
            .authorizeHttpRequests(auth -> auth
                // WebSocket endpoint
                .requestMatchers("/ws/**").permitAll()
                // Swagger UI endpoints
                .requestMatchers("/v3/api-docs/**", 
                               "/v3/api-docs.yaml",
                               "/swagger-ui/**",
                               "/swagger-ui.html",
                               "/webjars/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Allow guest checkout but require authentication for authenticated users
                .requestMatchers(HttpMethod.POST, "/api/orders/checkout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/pay").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/orders/**").permitAll()
                .requestMatchers("/api/orders/bundle/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                // Products
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority(PRODUCER)
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority(PRODUCER)
                .requestMatchers(HttpMethod.DELETE, "/api/products/admin/**").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority(PRODUCER)
                .requestMatchers(HttpMethod.GET, "/api/products/my-products").hasAuthority(PRODUCER)
                .requestMatchers(HttpMethod.GET, "/api/products/my-pending").hasAuthority(PRODUCER)
                .requestMatchers(HttpMethod.GET, "/api/products/pending").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/products/{id}/approve").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/products/{id}/decline").hasAuthority(ADMIN)
                
                // Reviews
                .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/eligibility/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews/pending").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/reviews/{reviewId}/approve").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/reviews/{reviewId}/decline").hasAuthority(ADMIN)
                
                // Support System
                .requestMatchers("/api/support/tickets/producer/**").hasAuthority(PRODUCER)
                .requestMatchers("/api/support/tickets/admin/**").hasAuthority(ADMIN)
                .requestMatchers("/api/support/tickets/unassigned").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/support/tickets").hasAuthority(PRODUCER)
                .requestMatchers(HttpMethod.POST, "/api/support/tickets/{ticketId}/assign").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/support/tickets/{ticketId}/forward").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/support/tickets/{ticketId}/close").hasAuthority(ADMIN)
                .requestMatchers("/api/support/tickets/{ticketId}/messages/**").hasAnyAuthority(ADMIN, PRODUCER)
                
                // Regions
                .requestMatchers("/api/regions/**").permitAll()
                // Producer Applications
                .requestMatchers(HttpMethod.POST, "/api/producer-applications").hasAuthority(CUSTOMER)
                .requestMatchers(HttpMethod.GET, "/api/producer-applications/my-application").hasAuthority(CUSTOMER)
                .requestMatchers(HttpMethod.GET, "/api/producer-applications/status").hasAuthority(CUSTOMER)
                .requestMatchers("/api/producer-applications/**").hasAuthority(ADMIN)
                // Coupons
                .requestMatchers(HttpMethod.GET, "/api/coupons/check-welcome").hasAnyAuthority(CUSTOMER , PRODUCER , ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/coupons/validate/**").permitAll()
                .requestMatchers("/api/coupons/**").hasAuthority(ADMIN)
                // Analytics - Admin endpoints
                .requestMatchers("/api/analytics/users").hasAuthority(ADMIN)
                .requestMatchers("/api/analytics/transactions").hasAuthority(ADMIN)
                .requestMatchers("/api/analytics/business-metrics").hasAuthority(ADMIN)
                .requestMatchers("/api/analytics/export").hasAuthority(ADMIN)
                // Analytics - Producer endpoints
                .requestMatchers("/api/analytics/overview").hasAuthority(PRODUCER)
                .requestMatchers("/api/analytics/total-orders").hasAuthority(PRODUCER)
                .requestMatchers("/api/analytics/total-pending-orders").hasAuthority(PRODUCER)
                .requestMatchers("/api/analytics/total-delivered-orders").hasAuthority(PRODUCER)
                .requestMatchers("/api/analytics/total-processing-orders").hasAuthority(PRODUCER)
                .anyRequest().authenticated()
            )
            
            // Authentication
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-XSRF-TOKEN", "Cookie" , "x-auth-check"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 