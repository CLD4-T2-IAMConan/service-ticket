package com.company.ticketservice.service;

import com.company.ticketservice.entity.Favorite;
import com.company.ticketservice.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService 테스트")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private Long testUserId;
    private Long testTicketId;
    private Favorite testFavorite;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testTicketId = 1L;

        testFavorite = Favorite.builder()
                .favoriteId(1L)
                .userId(testUserId)
                .ticketId(testTicketId)
                .build();
    }

    @Test
    @DisplayName("찜하기 추가 성공")
    void toggleFavorite_Success_Add() {
        // given
        when(favoriteRepository.findByUserIdAndTicketId(testUserId, testTicketId))
                .thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(testFavorite);

        // when
        boolean result = favoriteService.toggleFavorite(testUserId, testTicketId);

        // then
        assertThat(result).isTrue();
        verify(favoriteRepository, times(1)).findByUserIdAndTicketId(testUserId, testTicketId);
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    @DisplayName("찜하기 제거 성공")
    void toggleFavorite_Success_Remove() {
        // given
        when(favoriteRepository.findByUserIdAndTicketId(testUserId, testTicketId))
                .thenReturn(Optional.of(testFavorite));
        doNothing().when(favoriteRepository).delete(any(Favorite.class));

        // when
        boolean result = favoriteService.toggleFavorite(testUserId, testTicketId);

        // then
        assertThat(result).isFalse();
        verify(favoriteRepository, times(1)).findByUserIdAndTicketId(testUserId, testTicketId);
        verify(favoriteRepository, times(1)).delete(testFavorite);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("찜하기 여부 확인 - 찜함")
    void isFavorite_True() {
        // given
        when(favoriteRepository.existsByUserIdAndTicketId(testUserId, testTicketId))
                .thenReturn(true);

        // when
        boolean result = favoriteService.isFavorite(testUserId, testTicketId);

        // then
        assertThat(result).isTrue();
        verify(favoriteRepository, times(1)).existsByUserIdAndTicketId(testUserId, testTicketId);
    }

    @Test
    @DisplayName("찜하기 여부 확인 - 찜하지 않음")
    void isFavorite_False() {
        // given
        when(favoriteRepository.existsByUserIdAndTicketId(testUserId, testTicketId))
                .thenReturn(false);

        // when
        boolean result = favoriteService.isFavorite(testUserId, testTicketId);

        // then
        assertThat(result).isFalse();
        verify(favoriteRepository, times(1)).existsByUserIdAndTicketId(testUserId, testTicketId);
    }

    @Test
    @DisplayName("사용자 찜하기 목록 조회")
    void getUserFavorites_Success() {
        // given
        Favorite favorite1 = Favorite.builder()
                .favoriteId(1L)
                .userId(testUserId)
                .ticketId(1L)
                .build();
        Favorite favorite2 = Favorite.builder()
                .favoriteId(2L)
                .userId(testUserId)
                .ticketId(2L)
                .build();

        when(favoriteRepository.findByUserId(testUserId))
                .thenReturn(List.of(favorite1, favorite2));

        // when
        List<Favorite> favorites = favoriteService.getUserFavorites(testUserId);

        // then
        assertThat(favorites).isNotNull();
        assertThat(favorites).hasSize(2);
        verify(favoriteRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("찜하기 삭제")
    void removeFavorite_Success() {
        // given
        doNothing().when(favoriteRepository).deleteByUserIdAndTicketId(testUserId, testTicketId);

        // when
        favoriteService.removeFavorite(testUserId, testTicketId);

        // then
        verify(favoriteRepository, times(1)).deleteByUserIdAndTicketId(testUserId, testTicketId);
    }
}

