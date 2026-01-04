package com.company.ticketservice.listener;

import com.company.sns.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DealEventListener {

    private final ObjectMapper objectMapper;
    // TODO: TicketService를 주입받아 실제 티켓 상태 업데이트 로직 구현

    @SqsListener("${SQS_TICKET_DEAL_EVENTS_QUEUE_URL:${aws.sqs.queues.ticket-deal-events:https://sqs.ap-northeast-2.amazonaws.com/061051233779/passit-dev-ticket-deal-events}}")
    public void handleDealEvent(String messageJson) {
        try {
            // SNS wraps SQS messages, so we need to extract the Message field
            com.fasterxml.jackson.databind.JsonNode snsMessage = objectMapper.readTree(messageJson);
            String actualMessage = snsMessage.has("Message")
                ? snsMessage.get("Message").asText()
                : messageJson;

            EventMessage event = objectMapper.readValue(actualMessage, EventMessage.class);

            log.info("[SQS-EVENT] Received deal event: {}", event.getEventType());

            switch (event.getEventType()) {
                case "deal.confirmed":
                    handleDealConfirmed(event);
                    break;
                default:
                    log.warn("[SQS-EVENT] Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("[SQS-ERROR] Failed to process deal event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process deal event", e);
        }
    }

    private void handleDealConfirmed(EventMessage event) {
        Long ticketId = getLongValue(event.getData().get("ticketId"));
        Long dealId = getLongValue(event.getData().get("dealId"));

        log.info("[DEAL-CONFIRMED] Deal ID: {}, Ticket ID: {}", dealId, ticketId);

        // TODO: 티켓 상태를 USED로 변경하는 로직 구현
        // ticketService.updateTicketStatusToUsed(ticketId);
        log.info("[TODO] 티켓 상태 업데이트 로직이 필요합니다. Ticket ID: {}", ticketId);
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }
}
