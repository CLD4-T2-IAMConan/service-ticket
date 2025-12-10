package com.company.ticketservice.service;

// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    /**
     * 로그인 기능 붙이기 전까지 임시로 유저 ID 반환
     */
    public Long getCurrentUserId() {
        return 1L; // 나중에 Security 붙이면 실제로 교체
    }

    /**
     * 현재 로그인한 사용자의 ID를 반환합니다.
     * - SecurityContext에 들어있는 Authentication에서 name(또는 principal)을 꺼내 사용
     * - 여기서는 Authentication.getName()이 "유저 ID"라고 가정하고 Long으로 변환
     */
    /**
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            // 로그인 안 된 상태에서 호출되면 예외 던지는 게 안전함
            throw new IllegalStateException("로그인된 사용자가 없습니다.");
        }

        // ⚠ 여기서는 Authentication.getName()이 "유저 ID 문자열"이라고 가정
        String userIdString = authentication.getName();

        try {
            return Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            // 만약 username(이메일 등)이 들어오면 여기서 깨짐 → 그땐 principal에서 꺼내야 함
            throw new IllegalStateException("현재 인증 정보에서 유저 ID를 읽어올 수 없습니다: " + userIdString);
        }
    }
     */
}
