package com.company.ticketservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TicketUpdateRequest {

    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;

    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;

    private String seatInfo;
    private String ticketType;
}
