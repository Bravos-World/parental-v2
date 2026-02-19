package com.bravos.parentalv2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${app.cors.allowed-origin}")
  private String corsAllowedOrigin;

  private final SessionAuthFilter sessionAuthFilter;

  public SecurityConfig(SessionAuthFilter sessionAuthFilter) {
    this.sessionAuthFilter = sessionAuthFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").authenticated()
            .requestMatchers("/api/ws-secret/**").authenticated()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((_, response, _) -> {
              response.setStatus(401);
              response.setContentType("application/json");
              response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            }));
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    String[] allowedOrigins = corsAllowedOrigin.split(",");
    config.setAllowedOrigins(List.of(allowedOrigins));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

}
