package com.company.ticketservice.repository;

import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TicketSpecification {

    // 외부에서 이거 하나만 쓰면 됨
    public static Specification<Ticket> fromCondition(TicketSearchCondition cond) {
        return Specification
                .where(eventNameContains(cond.getEventName()))
                .and(hasStatus(cond.getTicketStatus()))
                .and(hasOwnerId(cond.getOwnerId()))
                .and(eventDateAfterOrEqual(cond.getStartDate()))
                .and(eventDateBeforeOrEqual(cond.getEndDate()))
                .and(hasCategoryId(cond.getCategoryId()));
    }

    // ========== 개별 조건들 ==========

    private static Specification<Ticket> eventNameContains(String eventName) {
        return (root, query, cb) -> {
            if (eventName == null || eventName.isBlank()) {
                return null; // 조건 적용 안 함
            }
            return cb.like(root.get("eventName"), "%" + eventName + "%");
        };
    }

    private static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("ticketStatus"), status);
        };
    }

    private static Specification<Ticket> hasOwnerId(Long ownerId) {
        return (root, query, cb) -> {
            if (ownerId == null) {
                return null;
            }
            return cb.equal(root.get("ownerId"), ownerId);
        };
    }

    private static Specification<Ticket> eventDateAfterOrEqual(LocalDateTime start) {
        return (root, query, cb) -> {
            if (start == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("eventDate"), start);
        };
    }

    private static Specification<Ticket> eventDateBeforeOrEqual(LocalDateTime end) {
        return (root, query, cb) -> {
            if (end == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("eventDate"), end);
        };
    }
    private static Specification<Ticket> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return null;
            }
            return cb.equal(root.get("categoryId"), categoryId);
        };
    }

}
