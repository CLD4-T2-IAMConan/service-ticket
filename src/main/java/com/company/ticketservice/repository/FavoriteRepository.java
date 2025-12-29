package com.company.ticketservice.repository;

import com.company.ticketservice.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndTicketId(Long userId, Long ticketId);
    boolean existsByUserIdAndTicketId(Long userId, Long ticketId);
    List<Favorite> findByUserId(Long userId);
    void deleteByUserIdAndTicketId(Long userId, Long ticketId);
}

