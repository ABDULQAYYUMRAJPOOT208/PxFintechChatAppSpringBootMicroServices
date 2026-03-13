package com.pxfintech.authentication_service.config;
import com.pxfintech.authentication_service.controller.LoginController;
import com.pxfintech.authentication_service.security.RateLimitingFilter;
import com.pxfintech.authentication_service.security.SecurityHeadersFilter;
import com.pxfintech.authentication_service.security.ServiceAuthenticationFilter;
import com.pxfintech.authentication_service.service.RateLimitingService;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SecurityConfig {
    // Add to existing SecurityConfig.java
    RateLimitingService rateLimitingService;
    RateLimitingFilter rateLimitingFilter;
    SecurityHeadersFilter securityHeadersFilter;
    ObjectMapper objectMapper;
    // Add this bean
    @Bean
    public SecurityFilterChain serviceSecurityFilterChain(HttpSecurity http,
                                                          ServiceAuthenticationFilter serviceAuthFilter) throws Exception {
        http
                .securityMatcher("/service/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/service/token").permitAll()
                        .requestMatchers("/service/validate").permitAll()
                        .requestMatchers("/service/**").authenticated()
                )
                .addFilterBefore(serviceAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Add to existing SecurityConfig.java

    @Bean
    @Order(3)
    public SecurityFilterChain loginPageFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/login", "/oauth2/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/oauth2/success", true)
                );

        return http.build();
    }

    @Bean
    public LoginController loginController() {
        return new LoginController();
    }

    // Add these beans to existing SecurityConfig

    @Bean
    @Order(4)
    public SecurityFilterChain securityHeadersFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .addFilterBefore(securityHeadersFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, SecurityHeadersFilter.class);

        return http.build();
    }

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter(rateLimitingService, objectMapper);
    }
}
