package com.company.ticketservice.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    /**
     * 현재 인증된 사용자의 userId 반환
     * - JWT subject(sub)에 userId가 들어있다는 전제
     */
    public Long getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증된 사용자가 없습니다.");
        }

        // JwtAuthenticationFilter에서 authentication.getName() = userId
        return Long.parseLong(authentication.getName());
    }
}
