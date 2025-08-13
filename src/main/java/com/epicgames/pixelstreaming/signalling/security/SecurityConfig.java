// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.security;

import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the signalling server.
 * Handles CORS, authentication, and authorization settings.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SignallingConfig signallingConfig;

    public SecurityConfig(SignallingConfig signallingConfig) {
        this.signallingConfig = signallingConfig;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API endpoints
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization
            .authorizeHttpRequests(authz -> {
                // Allow all health and monitoring endpoints
                authz.requestMatchers("/actuator/**").permitAll();
                authz.requestMatchers("/api/health").permitAll();
                authz.requestMatchers("/api/ping").permitAll();
                
                // Configure API endpoint access based on configuration
                if (signallingConfig.getSecurity().isEnableAuth()) {
                    authz.requestMatchers("/api/**").authenticated();
                } else {
                    authz.requestMatchers("/api/**").permitAll();
                }
                
                // Allow all other requests
                authz.anyRequest().permitAll();
            });

        // Disable form login and basic auth if not needed
        if (!signallingConfig.getSecurity().isEnableAuth()) {
            http.httpBasic(AbstractHttpConfigurer::disable);
            http.formLogin(AbstractHttpConfigurer::disable);
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        if (signallingConfig.getSecurity().isEnableCors()) {
            // Configure allowed origins
            String[] allowedOrigins = signallingConfig.getSecurity().getAllowedOrigins();
            configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
            
            // Configure allowed methods
            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            
            // Configure allowed headers
            configuration.setAllowedHeaders(List.of("*"));
            
            // Allow credentials
            configuration.setAllowCredentials(true);
            
            // Configure max age for preflight requests
            configuration.setMaxAge(3600L);
        } else {
            // Disable CORS
            configuration.setAllowedOrigins(List.of());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}