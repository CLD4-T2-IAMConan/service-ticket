package com.company.ticketservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** JWT 유효성 검증 + 로그 */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            // 만료 토큰
            log.warn("[JWT] Expired token - exp: {}", e.getClaims().getExpiration());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] Unsupported token");
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Malformed token");
        } catch (SecurityException e) {
            log.warn("[JWT] Invalid signature");
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] Empty or null token");
        }

        return false;
    }

    /** JWT → Authentication 변환 */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        Long userId = Long.parseLong(claims.getSubject()); // Account Service와 일치
        String role = claims.get("role", String.class);

        List<SimpleGrantedAuthority> authorities =
                role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
