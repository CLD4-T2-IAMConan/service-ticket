package com.company.ticketservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    // 티켓 기본 정보
    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "event_location", nullable = false)
    private String eventLocation;

    // 소유자 정보 (외래키: users.user_id)
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // 티켓 상태 ENUM 매핑
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false)
    private TicketStatus ticketStatus;

    // 가격 정보
    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    // 티켓 상세 정보
    @Column(name = "seat_info")
    private String seatInfo;

    @Column(name = "ticket_type")
    private String ticketType;

    // 타임스탬프
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 자동 날짜 기록
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.ticketStatus == null) {
            this.ticketStatus = TicketStatus.AVAILABLE; // DB Default와 맞춤
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
