package com.company.ticketservice.dto;

import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long ticketId;

    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;

    private Long ownerId;
    private TicketStatus ticketStatus;

    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;

    private String seatInfo;
    private String ticketType;

    // 이미지 URL (DB에 컬럼 추가되면 연결)
    private String imageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 엔티티 -> DTO 변환용 정적 메서드
    public static TicketResponse fromEntity(Ticket ticket) {
        return TicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .eventName(ticket.getEventName())
                .eventDate(ticket.getEventDate())
                .eventLocation(ticket.getEventLocation())
                .ownerId(ticket.getOwnerId())
                .ticketStatus(ticket.getTicketStatus())
                .originalPrice(ticket.getOriginalPrice())
                .sellingPrice(ticket.getSellingPrice())
                .seatInfo(ticket.getSeatInfo())
                .ticketType(ticket.getTicketType())
                // imageUrl은 아직 엔티티에 없으니 일단 null 처리 or 나중에 추가
                .imageUrl(null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
