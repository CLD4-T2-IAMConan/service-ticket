package com.company.ticketservice.config;

import com.company.ticketservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 비활성화 (JWT 기반)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 미사용
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증 / 인가 정책
                .authorizeHttpRequests(auth -> auth

                        // CORS Preflight 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 관리자용 시드 데이터 API (개발용) - 가장 먼저 체크
                        .requestMatchers("/api/admin/tickets/seed").permitAll()

                        // 찜하기 API는 인증 필요 (더 구체적인 경로를 먼저 체크)
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/*/favorite").authenticated()

                        // 티켓 조회는 누구나 가능 (GET 요청만)
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").permitAll()

                        // 티켓 상태 변경 (로그인 필요)
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/status/*").authenticated()

                        // 판매자 전용 API (전부 로그인 필요)
                        .requestMatchers("/api/sellers/**").authenticated()

                        // 그 외 요청은 차단
                        .anyRequest().denyAll()
                )

                // JWT 필터 적용
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
