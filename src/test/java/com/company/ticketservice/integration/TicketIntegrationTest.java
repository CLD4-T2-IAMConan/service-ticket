package com.company.ticketservice.integration;

import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
import com.company.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("integration")
class TicketIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("통합 테스트 - 티켓 생성 및 조회")
    void createAndFindTicket() {
        // given
        Ticket ticket = Ticket.builder()
                .eventName("통합 테스트 콘서트")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation("서울")
                .ownerId(1L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(50000))
                .sellingPrice(BigDecimal.valueOf(40000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();

        // when
        Ticket saved = ticketRepository.save(ticket);
        Ticket found = ticketRepository.findById(saved.getTicketId()).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getEventName()).isEqualTo("통합 테스트 콘서트");
        assertThat(found.getTicketStatus()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    @DisplayName("통합 테스트 - 티켓 상태 업데이트")
    void updateTicketStatus() {
        // given
        Ticket ticket = Ticket.builder()
                .eventName("콘서트")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation("서울")
                .ownerId(1L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(50000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();
        Ticket saved = ticketRepository.save(ticket);

        // when
        saved.setTicketStatus(TicketStatus.RESERVED);
        Ticket updated = ticketRepository.save(saved);

        // then
        assertThat(updated.getTicketStatus()).isEqualTo(TicketStatus.RESERVED);
    }

    @Test
    @DisplayName("통합 테스트 - 티켓 삭제")
    void deleteTicket() {
        // given
        Ticket ticket = Ticket.builder()
                .eventName("콘서트")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation("서울")
                .ownerId(1L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(50000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();
        Ticket saved = ticketRepository.save(ticket);

        // when
        ticketRepository.delete(saved);
        Ticket found = ticketRepository.findById(saved.getTicketId()).orElse(null);

        // then
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("통합 테스트 - 만료된 티켓 자동 업데이트")
    void expireTicketsAutomatically() {
        // given
        Ticket pastTicket = Ticket.builder()
                .eventName("지난 콘서트")
                .eventDate(LocalDateTime.now().minusDays(1))
                .eventLocation("서울")
                .ownerId(1L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(50000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();
        ticketRepository.save(pastTicket);

        // when
        int updatedCount = ticketRepository.expireAvailableTickets(
                TicketStatus.AVAILABLE,
                TicketStatus.EXPIRED,
                LocalDateTime.now()
        );

        // then
        assertThat(updatedCount).isGreaterThan(0);

        List<Ticket> expiredTickets = ticketRepository.findAll();
        assertThat(expiredTickets).isNotEmpty();
        assertThat(expiredTickets.get(0).getTicketStatus()).isEqualTo(TicketStatus.EXPIRED);
    }

    @Test
    @DisplayName("통합 테스트 - 여러 티켓 조회")
    void findMultipleTickets() {
        // given
        Ticket ticket1 = Ticket.builder()
                .eventName("콘서트 1")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation("서울")
                .ownerId(1L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(50000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();

        Ticket ticket2 = Ticket.builder()
                .eventName("콘서트 2")
                .eventDate(LocalDateTime.now().plusDays(20))
                .eventLocation("부산")
                .ownerId(2L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(60000))
                .categoryId(1L)
                .tradeType(TradeType.ONSITE)
                .build();

        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);

        // when
        List<Ticket> tickets = ticketRepository.findAll();

        // then
        assertThat(tickets).hasSize(2);
        assertThat(tickets).extracting("eventName")
                .containsExactlyInAnyOrder("콘서트 1", "콘서트 2");
    }
}
