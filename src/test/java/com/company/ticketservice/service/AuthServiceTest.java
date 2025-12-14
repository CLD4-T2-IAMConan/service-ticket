package com.company.ticketservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("현재 사용자 ID 반환 - 임시 구현")
    void getCurrentUserId_ReturnsTemporaryId() {
        // when
        Long userId = authService.getCurrentUserId();

        // then
        assertThat(userId).isEqualTo(2L);
    }
}
