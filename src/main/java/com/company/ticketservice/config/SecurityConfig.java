package com.company.ticketservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (API 서버이므로)
                .csrf(csrf -> csrf.disable())

                // 인증/인가 설정
                .authorizeHttpRequests(auth -> auth

                        // 티켓 조회/검색/상세는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").permitAll()

                        // 판매자 전용 API (등록/수정/삭제/내 티켓)
                        .requestMatchers("/api/sellers/**").authenticated()

                        // 그 외 API는 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
