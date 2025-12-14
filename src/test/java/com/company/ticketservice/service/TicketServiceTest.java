package com.company.ticketservice.service;

import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.dto.TicketResponse;
import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.dto.TicketUpdateRequest;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
import com.company.ticketservice.exception.BadRequestException;
import com.company.ticketservice.exception.NotFoundException;
import com.company.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private TicketService ticketService;

    private TicketCreateRequest validCreateRequest;
    private Ticket validTicket;

    @BeforeEach
    void setUp() {
        validCreateRequest = TicketCreateRequest.builder()
                .eventName("콘서트")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation("서울")
                .ownerId(1L)
                .originalPrice(BigDecimal.valueOf(50000))
                .sellingPrice(BigDecimal.valueOf(40000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .seatInfo("A구역 10열 5번")
                .ticketType("일반")
                .description("깨끗한 티켓입니다")
                .build();

        validTicket = Ticket.builder()
                .ticketId(1L)
                .eventName("콘서트")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation("서울")
                .ownerId(1L)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(BigDecimal.valueOf(50000))
                .sellingPrice(BigDecimal.valueOf(40000))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .seatInfo("A구역 10열 5번")
                .ticketType("일반")
                .description("깨끗한 티켓입니다")
                .build();
    }

    @Test
    @DisplayName("티켓 생성 성공")
    void createTicket_Success() {
        // given
        when(ticketRepository.save(any(Ticket.class))).thenReturn(validTicket);

        // when
        TicketResponse response = ticketService.createTicket(validCreateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEventName()).isEqualTo("콘서트");
        assertThat(response.getTicketStatus()).isEqualTo(TicketStatus.AVAILABLE);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 공연명 누락")
    void createTicket_Fail_NoEventName() {
        // given
        validCreateRequest.setEventName(null);

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(validCreateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("공연/이벤트 이름은 필수 입력값입니다");
    }

    @Test
    @DisplayName("티켓 생성 실패 - 공연 날짜 과거")
    void createTicket_Fail_PastEventDate() {
        // given
        validCreateRequest.setEventDate(LocalDateTime.now().minusDays(1));

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(validCreateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("공연 날짜는 현재 시점 이후여야 합니다");
    }

    @Test
    @DisplayName("티켓 생성 실패 - 원래 가격이 0 이하")
    void createTicket_Fail_InvalidOriginalPrice() {
        // given
        validCreateRequest.setOriginalPrice(BigDecimal.ZERO);

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(validCreateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("원래 가격은 0보다 큰 값이어야 합니다");
    }

    @Test
    @DisplayName("티켓 생성 실패 - 판매 가격이 원래 가격보다 큼")
    void createTicket_Fail_SellingPriceGreaterThanOriginal() {
        // given
        validCreateRequest.setSellingPrice(BigDecimal.valueOf(60000));

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(validCreateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("판매 가격은 원래 가격을 초과할 수 없습니다");
    }

    @Test
    @DisplayName("티켓 검색 성공")
    void searchTickets_Success() {
        // given
        List<Ticket> tickets = Arrays.asList(validTicket);
        when(ticketRepository.findAll(any(Specification.class))).thenReturn(tickets);

        TicketSearchCondition condition = new TicketSearchCondition();
        condition.setEventName("콘서트");

        // when
        List<TicketResponse> responses = ticketService.searchTickets(condition);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEventName()).isEqualTo("콘서트");
        verify(ticketRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("티켓 검색 실패 - 결과 없음")
    void searchTickets_Fail_NoResults() {
        // given
        when(ticketRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList());

        TicketSearchCondition condition = new TicketSearchCondition();

        // when & then
        assertThatThrownBy(() -> ticketService.searchTickets(condition))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("검색 조건에 해당하는 티켓이 없습니다");
    }

    @Test
    @DisplayName("판매자 티켓 조회 성공")
    void searchSellerTickets_Success() {
        // given
        List<Ticket> tickets = Arrays.asList(validTicket);
        when(ticketRepository.findAll(any(Specification.class))).thenReturn(tickets);

        // when
        List<TicketResponse> responses = ticketService.searchSellerTickets(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOwnerId()).isEqualTo(1L);
        verify(ticketRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("티켓 수정 성공")
    void updateTicket_Success() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(validTicket));
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(validTicket);

        TicketUpdateRequest updateRequest = new TicketUpdateRequest();
        updateRequest.setEventName("수정된 콘서트");

        // when
        TicketResponse response = ticketService.updateTicket(1L, updateRequest);

        // then
        assertThat(response).isNotNull();
        verify(ticketRepository, times(1)).findById(1L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 티켓 없음")
    void updateTicket_Fail_NotFound() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        TicketUpdateRequest updateRequest = new TicketUpdateRequest();

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicket(1L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("해당 티켓을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("티켓 수정 실패 - 소유자 불일치")
    void updateTicket_Fail_NotOwner() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(validTicket));
        when(authService.getCurrentUserId()).thenReturn(999L); // 다른 사용자

        TicketUpdateRequest updateRequest = new TicketUpdateRequest();

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicket(1L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("본인이 등록한 티켓만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("티켓 수정 실패 - 거래 중인 티켓")
    void updateTicket_Fail_ReservedTicket() {
        // given
        validTicket.setTicketStatus(TicketStatus.RESERVED);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(validTicket));
        when(authService.getCurrentUserId()).thenReturn(1L);

        TicketUpdateRequest updateRequest = new TicketUpdateRequest();

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicket(1L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("거래 중인 티켓은 수정할 수 없습니다");
    }

    @Test
    @DisplayName("티켓 삭제 성공")
    void deleteTicket_Success() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(validTicket));
        when(authService.getCurrentUserId()).thenReturn(1L);

        // when
        ticketService.deleteTicket(1L);

        // then
        verify(ticketRepository, times(1)).findById(1L);
        verify(ticketRepository, times(1)).delete(validTicket);
    }

    @Test
    @DisplayName("티켓 삭제 실패 - 티켓 없음")
    void deleteTicket_Fail_NotFound() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ticketService.deleteTicket(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("해당 티켓을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("티켓 삭제 실패 - 소유자 불일치")
    void deleteTicket_Fail_NotOwner() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(validTicket));
        when(authService.getCurrentUserId()).thenReturn(999L);

        // when & then
        assertThatThrownBy(() -> ticketService.deleteTicket(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("본인이 등록한 티켓만 삭제할 수 있습니다");
    }

    @Test
    @DisplayName("티켓 삭제 실패 - 거래 중인 티켓")
    void deleteTicket_Fail_SoldTicket() {
        // given
        validTicket.setTicketStatus(TicketStatus.SOLD);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(validTicket));
        when(authService.getCurrentUserId()).thenReturn(1L);

        // when & then
        assertThatThrownBy(() -> ticketService.deleteTicket(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("거래 중인 티켓은 삭제할 수 없습니다");
    }
}
