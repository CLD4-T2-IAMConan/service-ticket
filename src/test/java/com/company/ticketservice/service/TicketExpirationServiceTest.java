package com.company.ticketservice.service;

import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketExpirationServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketExpirationService ticketExpirationService;

    @Test
    @DisplayName("만료된 티켓 상태 업데이트 - 업데이트 발생")
    void expireTickets_UpdatesExpiredTickets() {
        // given
        when(ticketRepository.expireAvailableTickets(
                eq(TicketStatus.AVAILABLE),
                eq(TicketStatus.EXPIRED),
                any(LocalDateTime.class)
        )).thenReturn(5);

        // when
        ticketExpirationService.expireTickets();

        // then
        verify(ticketRepository, times(1)).expireAvailableTickets(
                eq(TicketStatus.AVAILABLE),
                eq(TicketStatus.EXPIRED),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("만료된 티켓 상태 업데이트 - 업데이트 없음")
    void expireTickets_NoUpdates() {
        // given
        when(ticketRepository.expireAvailableTickets(
                eq(TicketStatus.AVAILABLE),
                eq(TicketStatus.EXPIRED),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        ticketExpirationService.expireTickets();

        // then
        verify(ticketRepository, times(1)).expireAvailableTickets(
                eq(TicketStatus.AVAILABLE),
                eq(TicketStatus.EXPIRED),
                any(LocalDateTime.class)
        );
    }
}
