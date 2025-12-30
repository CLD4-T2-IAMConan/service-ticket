package com.company.ticketservice.entity;

public enum TicketStatus {

    AVAILABLE(true, true),   // 판매중 (수정/삭제 가능)
    RESERVED(false, false),  // 거래중
    SOLD(false, false),      // 거래완료 (아직 사용 전)
    USED(false, false),      // 사용완료
    EXPIRED(true, false);    // 판매 안 됨 + 기간 만료 (삭제만 가능)

    private final boolean deletable;
    private final boolean updatable;

    TicketStatus(boolean deletable, boolean updatable) {
        this.deletable = deletable;
        this.updatable = updatable;
    }

    /** 삭제 가능 여부 */
    public boolean isDeletable() {
        return deletable;
    }

    /** 수정 가능 여부 */
    public boolean isUpdatable() {
        return updatable;
    }

    /** 상태 전이 가능 여부 */
    public boolean canChangeTo(TicketStatus target) {
        if (this == target) return true;

        return switch (this) {
            case AVAILABLE ->
                    target == RESERVED ||
                            target == EXPIRED;

            case RESERVED ->
                    target == SOLD;   // 거래 취소는 별도 API에서만 허용

            case SOLD ->
                    target == USED;

            case USED, EXPIRED ->
                    false;
        };
    }
}
