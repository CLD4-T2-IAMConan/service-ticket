package com.company.ticketservice.dto;

import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
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

    private Long categoryId;

    private String image1;
    private String image2;

    private String description;

    private TradeType tradeType;


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
                .categoryId(ticket.getCategoryId())
                .image1(ticket.getImage1())
                .image2(ticket.getImage2())
                .description(ticket.getDescription())
                .tradeType(ticket.getTradeType())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
