package com.company.ticketservice.dto;

import com.company.ticketservice.entity.TradeType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

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

    private Long categoryId;
    private String description;
    private TradeType tradeType;

    private MultipartFile image1;
    private MultipartFile image2;
}
