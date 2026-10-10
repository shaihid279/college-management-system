package com.college.cms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginSuccessHandler successHandler,
                                           LoginFailureHandler failureHandler) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/", "/login", "/login/**", "/register", "/register/**", "/contact",
                                "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").successHandler(successHandler)
                        .failureHandler(failureHandler).permitAll())
                .logout(l -> l.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}