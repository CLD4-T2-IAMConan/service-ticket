package com.company.ticketservice.dto;

import com.company.ticketservice.entity.TicketStatus;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCreateRequest {

    // 티켓 기본 정보
    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;

    // 소유자 정보
    // 나중에 로그인 붙이면 Security에서 꺼내 쓰고, 지금은 그냥 요청으로 받거나 서비스에서 주입해도 됨
    private Long ownerId;

    // 티켓 상태 (생략 시 서버에서 기본 AVAILABLE로 처리해도 됨)
    private TicketStatus ticketStatus;

    // 가격 정보
    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;

    // 상세 정보
    private String seatInfo;
    private String ticketType;

    // 이미지 파일 (선택사항)
    // POST /sellers/tickets 에서 multipart/form-data로 받을 예정
    private MultipartFile imageFile;
}
