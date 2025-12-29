package com.company.ticketservice.service;

import com.company.ticketservice.entity.Favorite;
import com.company.ticketservice.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    /**
     * 찜하기 추가/제거 (토글)
     */
    @Transactional
    public boolean toggleFavorite(Long userId, Long ticketId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndTicketId(userId, ticketId);
        
        if (existing.isPresent()) {
            // 이미 찜하기가 있으면 제거
            favoriteRepository.delete(existing.get());
            log.info("찜하기 제거: userId={}, ticketId={}", userId, ticketId);
            return false;
        } else {
            // 찜하기 추가
            Favorite favorite = Favorite.builder()
                    .userId(userId)
                    .ticketId(ticketId)
                    .build();
            favoriteRepository.save(favorite);
            log.info("찜하기 추가: userId={}, ticketId={}", userId, ticketId);
            return true;
        }
    }

    /**
     * 찜하기 여부 확인
     */
    public boolean isFavorite(Long userId, Long ticketId) {
        return favoriteRepository.existsByUserIdAndTicketId(userId, ticketId);
    }

    /**
     * 사용자의 찜하기 목록 조회
     */
    public List<Favorite> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserId(userId);
    }

    /**
     * 찜하기 삭제
     */
    @Transactional
    public void removeFavorite(Long userId, Long ticketId) {
        favoriteRepository.deleteByUserIdAndTicketId(userId, ticketId);
        log.info("찜하기 삭제: userId={}, ticketId={}", userId, ticketId);
    }
}

