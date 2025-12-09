package com.company.ticketservice.dto;

import com.company.ticketservice.entity.TicketStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class TicketSearchCondition {

    // 공연명 검색 (부분 검색)
    private String eventName;

    // 티켓 상태 필터 (AVAILABLE, SOLD 등)
    private TicketStatus ticketStatus;

    // 특정 소유자(판매자)의 티켓만 보고 싶을 때 사용
    private Long ownerId;

    // 이벤트 날짜 범위 검색
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;   // 이 날짜 이후

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;     // 이 날짜 이전
}
