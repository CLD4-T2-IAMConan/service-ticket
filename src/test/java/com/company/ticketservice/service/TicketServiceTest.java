package com.company.ticketservice.service;

import com.company.sns.SnsEventPublisher;
import com.company.ticketservice.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService 테스트")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SnsEventPublisher eventPublisher;

    @InjectMocks
    private TicketService ticketService;

    private Long testUserId;
    private Ticket testTicket;
    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        futureDate = LocalDateTime.now().plusDays(30);

        testTicket = Ticket.builder()
                .ticketId(1L)
                .eventName("테스트 콘서트")
                .eventDate(futureDate)
                .eventLocation("올림픽공원")
                .ownerId(testUserId)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(new BigDecimal("100000"))
                .sellingPrice(new BigDecimal("95000"))
                .seatInfo("VIP석 1번")
                .ticketType("일반")
                .categoryId(1L)
                .description("테스트 티켓입니다")
                .tradeType(TradeType.DELIVERY)
                .build();
    }

    @Test
    @DisplayName("티켓 생성 성공")
    void createTicket_Success() {
        // given
        TicketCreateRequest request = TicketCreateRequest.builder()
                .eventName("새로운 콘서트")
                .eventDate(futureDate)
                .eventLocation("잠실실내체육관")
                .originalPrice(new BigDecimal("150000"))
                .sellingPrice(new BigDecimal("145000"))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();

        Ticket savedTicket = Ticket.builder()
                .ticketId(2L)
                .eventName(request.getEventName())
                .eventDate(request.getEventDate())
                .eventLocation(request.getEventLocation())
                .ownerId(testUserId)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(request.getOriginalPrice())
                .sellingPrice(request.getSellingPrice())
                .categoryId(request.getCategoryId())
                .tradeType(request.getTradeType())
                .build();

        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        // when
        TicketResponse response = ticketService.createTicket(testUserId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEventName()).isEqualTo("새로운 콘서트");
        assertThat(response.getOwnerId()).isEqualTo(testUserId);
        assertThat(response.getTicketStatus()).isEqualTo(TicketStatus.AVAILABLE);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 공연명 누락")
    void createTicket_Fail_EventNameMissing() {
        // given
        TicketCreateRequest request = TicketCreateRequest.builder()
                .eventName("")
                .eventDate(futureDate)
                .eventLocation("잠실실내체육관")
                .originalPrice(new BigDecimal("150000"))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(testUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("공연/이벤트 이름은 필수 입력값입니다");
    }

    @Test
    @DisplayName("티켓 생성 실패 - 과거 날짜")
    void createTicket_Fail_PastDate() {
        // given
        TicketCreateRequest request = TicketCreateRequest.builder()
                .eventName("과거 콘서트")
                .eventDate(LocalDateTime.now().minusDays(1))
                .eventLocation("잠실실내체육관")
                .originalPrice(new BigDecimal("150000"))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(testUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("공연 날짜는 현재 시점 이후여야 합니다");
    }

    @Test
    @DisplayName("티켓 생성 실패 - 판매 가격이 원가보다 높음")
    void createTicket_Fail_SellingPriceExceedsOriginal() {
        // given
        TicketCreateRequest request = TicketCreateRequest.builder()
                .eventName("콘서트")
                .eventDate(futureDate)
                .eventLocation("잠실실내체육관")
                .originalPrice(new BigDecimal("150000"))
                .sellingPrice(new BigDecimal("160000"))
                .categoryId(1L)
                .tradeType(TradeType.DELIVERY)
                .build();

        // when & then
        assertThatThrownBy(() -> ticketService.createTicket(testUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("판매 가격은 원래 가격을 초과할 수 없습니다");
    }

    @Test
    @DisplayName("티켓 상세 조회 성공")
    void getTicketDetail_Success() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when
        TicketResponse response = ticketService.getTicketDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(1L);
        assertThat(response.getEventName()).isEqualTo("테스트 콘서트");
        verify(ticketRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("티켓 상세 조회 실패 - 존재하지 않는 티켓")
    void getTicketDetail_Fail_NotFound() {
        // given
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ticketService.getTicketDetail(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("티켓 ID: 999");
    }

    @Test
    @DisplayName("티켓 검색 성공")
    void searchTickets_Success() {
        // given
        TicketSearchCondition condition = new TicketSearchCondition();
        condition.setEventName("테스트");
        
        Pageable pageable = PageRequest.of(0, 20);
        Page<Ticket> ticketPage = new PageImpl<>(List.of(testTicket), pageable, 1);

        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(ticketPage);

        // when
        PageResponse<TicketResponse> response = ticketService.searchTickets(condition, 0, 20, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(ticketRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("판매자 본인 티켓 조회 성공")
    void searchSellerTickets_Success() {
        // given
        when(ticketRepository.findAll(any(Specification.class))).thenReturn(List.of(testTicket));

        // when
        List<TicketResponse> responses = ticketService.searchSellerTickets(testUserId);

        // then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOwnerId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("판매자 본인 티켓 조회 실패 - 티켓 없음")
    void searchSellerTickets_Fail_NoTickets() {
        // given
        when(ticketRepository.findAll(any(Specification.class))).thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> ticketService.searchSellerTickets(testUserId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("등록한 티켓이 없습니다");
    }

    @Test
    @DisplayName("티켓 수정 성공")
    void updateTicket_Success() {
        // given
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setEventName("수정된 콘서트");
        request.setSellingPrice(new BigDecimal("90000"));

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // when
        TicketResponse response = ticketService.updateTicket(1L, testUserId, request);

        // then
        assertThat(response).isNotNull();
        verify(ticketRepository, times(1)).findById(1L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 본인 티켓 아님")
    void updateTicket_Fail_NotOwner() {
        // given
        Long otherUserId = 2L;
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setEventName("수정된 콘서트");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicket(1L, otherUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("본인이 등록한 티켓만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("티켓 수정 실패 - 수정 불가능한 상태")
    void updateTicket_Fail_NotUpdatableStatus() {
        // given
        testTicket.setTicketStatus(TicketStatus.RESERVED);
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setEventName("수정된 콘서트");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicket(1L, testUserId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("현재 상태에서는 티켓을 수정할 수 없습니다");
    }

    @Test
    @DisplayName("티켓 삭제 성공")
    void deleteTicket_Success() {
        // given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        doNothing().when(ticketRepository).delete(any(Ticket.class));

        // when
        ticketService.deleteTicket(1L, testUserId);

        // then
        verify(ticketRepository, times(1)).findById(1L);
        verify(ticketRepository, times(1)).delete(testTicket);
    }

    @Test
    @DisplayName("티켓 삭제 실패 - 본인 티켓 아님")
    void deleteTicket_Fail_NotOwner() {
        // given
        Long otherUserId = 2L;
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when & then
        assertThatThrownBy(() -> ticketService.deleteTicket(1L, otherUserId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("본인이 등록한 티켓만 삭제할 수 있습니다");
    }

    @Test
    @DisplayName("티켓 삭제 실패 - 삭제 불가능한 상태")
    void deleteTicket_Fail_NotDeletableStatus() {
        // given
        testTicket.setTicketStatus(TicketStatus.RESERVED);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when & then
        assertThatThrownBy(() -> ticketService.deleteTicket(1L, testUserId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("현재 상태에서는 티켓을 삭제할 수 없습니다");
    }

    @Test
    @DisplayName("티켓 상태 변경 성공 - AVAILABLE -> RESERVED")
    void updateTicketStatus_Success_AvailableToReserved() {
        // given
        Ticket updatedTicket = Ticket.builder()
                .ticketId(1L)
                .eventName(testTicket.getEventName())
                .eventDate(testTicket.getEventDate())
                .eventLocation(testTicket.getEventLocation())
                .ownerId(testTicket.getOwnerId())
                .ticketStatus(TicketStatus.RESERVED) // 상태 변경됨
                .originalPrice(testTicket.getOriginalPrice())
                .sellingPrice(testTicket.getSellingPrice())
                .seatInfo(testTicket.getSeatInfo())
                .ticketType(testTicket.getTicketType())
                .categoryId(testTicket.getCategoryId())
                .description(testTicket.getDescription())
                .tradeType(testTicket.getTradeType())
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(updatedTicket);

        // when
        TicketResponse response = ticketService.updateTicketStatus(1L, testUserId, "RESERVED");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTicketStatus()).isEqualTo(TicketStatus.RESERVED);
        verify(ticketRepository, times(1)).findById(1L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("티켓 상태 변경 실패 - 잘못된 상태값")
    void updateTicketStatus_Fail_InvalidStatus() {
        // when & then
        // IllegalArgumentException은 TicketStatus.valueOf()에서 발생하므로
        // repository 호출 전에 예외가 발생함
        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, testUserId, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 티켓 상태 값");
        
        // repository는 호출되지 않음
        verify(ticketRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("티켓 상태 변경 실패 - 불가능한 상태 전이")
    void updateTicketStatus_Fail_InvalidTransition() {
        // given
        testTicket.setTicketStatus(TicketStatus.AVAILABLE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, testUserId, "USED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("변경할 수 없습니다");
    }

    @Test
    @DisplayName("티켓 상태 변경 실패 - 본인 티켓 아님")
    void updateTicketStatus_Fail_NotOwner() {
        // given
        Long otherUserId = 2L;
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        // when & then
        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, otherUserId, "RESERVED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("본인 티켓만 상태를 변경할 수 있습니다");
    }
}

