package com.company.ticketservice.service;

import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.dto.TicketResponse;
import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.exception.BadRequestException;
import com.company.ticketservice.exception.NotFoundException;
import com.company.ticketservice.repository.TicketRepository;
import com.company.ticketservice.repository.TicketSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    /**
     *  티켓 생성
     * - DTO → 엔티티 변환
     * - 기본 상태값 처리
     * - DB 저장
     * - Response DTO 반환
     */
    public TicketResponse createTicket(TicketCreateRequest request) {
        validateCreateRequest(request);


        Ticket ticket = Ticket.builder()
                .eventName(request.getEventName())
                .eventDate(request.getEventDate())
                .eventLocation(request.getEventLocation())
                .ownerId(request.getOwnerId())
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(request.getOriginalPrice())
                .sellingPrice(request.getSellingPrice())
                .seatInfo(request.getSeatInfo())
                .ticketType(request.getTicketType())
                .build();

        Ticket saved = ticketRepository.save(ticket);

        return TicketResponse.fromEntity(saved);
    }

    /**
     *  티켓 생성 시 검증 로직
     */
    private void validateCreateRequest(TicketCreateRequest request) {

        // 1) 필수 필드 체크
        if (request.getEventName() == null || request.getEventName().isBlank()) {
            throw new BadRequestException("공연/이벤트 이름은 필수 입력값입니다.");
        }

        if (request.getEventDate() == null) {
            throw new BadRequestException("공연 날짜는 필수 입력값입니다.");
        }

        if (request.getEventLocation() == null || request.getEventLocation().isBlank()) {
            throw new BadRequestException("공연 장소는 필수 입력값입니다.");
        }

        if (request.getOriginalPrice() == null) {
            throw new BadRequestException("원래 가격은 필수 입력값입니다.");
        }

        // 2) 공연 날짜가 현재 시점보다 이전인지 체크
        if (request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("공연 날짜는 현재 시점 이후여야 합니다.");
        }

        // 3) 가격 유효성 체크
        BigDecimal originalPrice = request.getOriginalPrice();
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("원래 가격은 0보다 큰 값이어야 합니다.");
        }

        if (request.getSellingPrice() != null) {
            BigDecimal sellingPrice = request.getSellingPrice();

            if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("판매 가격은 0보다 큰 값이어야 합니다.");
            }

            // 정책: 판매 가격이 원래 가격보다 클 수 없다 (선택)
            if (sellingPrice.compareTo(originalPrice) > 0) {
                throw new BadRequestException("판매 가격은 원래 가격을 초과할 수 없습니다.");
            }
        }
    }



    /**
     *  티켓 검색
     */
    public List<TicketResponse> searchTickets(TicketSearchCondition condition) {

        List<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition)
        );

        if (tickets.isEmpty()) {
            throw new NotFoundException("검색 조건에 해당하는 티켓이 없습니다.");
        }

        return tickets.stream()
                .map(TicketResponse::fromEntity)
                .toList();
    }

    /**
     *  판매자 본인 티켓만 조회
     * - Controller에서 ownerId만 받았을 때 사용 가능
     */
    public List<TicketResponse> searchSellerTickets(Long ownerId) {
        TicketSearchCondition condition = new TicketSearchCondition();
        condition.setOwnerId(ownerId);

        List<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition)
        );

        if (tickets.isEmpty()) {
            throw new NotFoundException("등록한 티켓이 없습니다.");
        }

        return tickets.stream()
                .map(TicketResponse::fromEntity)
                .toList();
    }
}
