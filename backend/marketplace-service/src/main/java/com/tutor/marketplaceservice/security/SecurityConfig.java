package com.tutor.marketplaceservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/reviews/eligibility").hasAuthority("ROLE_CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/subjects").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/tutors").hasAuthority("ROLE_TUTOR")
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/tutors/*/requests").hasAuthority("ROLE_CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/tutors/*/complaints").hasAnyAuthority("ROLE_CLIENT", "ROLE_TUTOR")
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/reviews").hasAuthority("ROLE_CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/reviews/*/complaints").hasAnyAuthority("ROLE_CLIENT", "ROLE_TUTOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
