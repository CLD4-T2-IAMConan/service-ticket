package com.company.ticketservice.dto;

import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreateRequest {

    // 필수값

    @NotBlank(message = "공연명은 필수 입력입니다.")
    private String eventName;

    @NotNull(message = "공연 날짜는 필수 입력입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventDate;

    @NotBlank(message = "공연 장소는 필수 입력입니다.")
    private String eventLocation;

    @NotNull(message = "원래 가격은 필수 입력입니다.")
    private BigDecimal originalPrice;

    @NotNull(message = "카테고리 ID는 필수 입력입니다.")
    private Long categoryId;

    @NotNull(message = "거래 방식은 필수 입력입니다.")
    private TradeType tradeType;


    // 선택값

    private BigDecimal sellingPrice;     // NULL 허용
    private String seatInfo;             // NULL 허용
    private String ticketType;           // NULL 허용
    private String description;          // NULL 허용

    private MultipartFile image1;        // NULL 허용
    private MultipartFile image2;        // NULL 허용


    // 서버에서 자동 설정되는 값

    // 사용자가 절대 입력 X → Controller에서 자동 설정됨
    private Long ownerId;

    // Optional: Null이면 Service에서 AVAILABLE로 기본값 설정 가능
    private TicketStatus ticketStatus;
}
