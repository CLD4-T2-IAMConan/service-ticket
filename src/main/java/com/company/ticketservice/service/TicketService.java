package com.company.ticketservice.service;

import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.dto.TicketResponse;
import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.repository.TicketRepository;
import com.company.ticketservice.repository.TicketSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        // ticketStatus가 안 들어오면 기본값 AVAILABLE 적용
        TicketStatus status = request.getTicketStatus() != null
                ? request.getTicketStatus()
                : TicketStatus.AVAILABLE;

        Ticket ticket = Ticket.builder()
                .eventName(request.getEventName())
                .eventDate(request.getEventDate())
                .eventLocation(request.getEventLocation())
                .ownerId(request.getOwnerId())
                .ticketStatus(status)
                .originalPrice(request.getOriginalPrice())
                .sellingPrice(request.getSellingPrice())
                .seatInfo(request.getSeatInfo())
                .ticketType(request.getTicketType())
                .build();

        Ticket saved = ticketRepository.save(ticket);

        return TicketResponse.fromEntity(saved);
    }

    /**
     *  티켓 검색
     */
    public List<TicketResponse> searchTickets(TicketSearchCondition condition) {

        List<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition)
        );

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

        return tickets.stream()
                .map(TicketResponse::fromEntity)
                .toList();
    }
}
