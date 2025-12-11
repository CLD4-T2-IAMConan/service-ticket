package com.company.ticketservice.service;

import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketExpirationService {

    private final TicketRepository ticketRepository;

    /**
     * 매 시 정각마다 실행
     * EXPIRED 상태 자동 업데이트
     */
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void expireTickets() {
        LocalDateTime now = LocalDateTime.now();

        int updated = ticketRepository.expireAvailableTickets(
                TicketStatus.AVAILABLE,
                TicketStatus.EXPIRED,
                now
        );

        if (updated > 0) {
            log.info("Expired tickets updated: {} at {}", updated, now);
        }
    }
}
