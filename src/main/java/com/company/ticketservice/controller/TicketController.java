package com.company.ticketservice.controller;

import com.company.ticketservice.dto.*;
import com.company.ticketservice.service.TicketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class TicketController {

    private final TicketService ticketService;

    /**
     * [POST] 티켓 등록 (판매자)
     * - URL: /api/sellers/tickets
     * - 인증 필요
     * - multipart/form-data
     */
    @PostMapping(
            value = "/sellers/tickets",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<TicketResponse> createTicket(
            Authentication authentication,
            @Valid @ModelAttribute TicketCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        TicketResponse response = ticketService.createTicket(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * [GET] 티켓 리스트 조회
     * - URL: /api/tickets
     * - 인증 불필요
     */
    @GetMapping("/tickets")
    public ApiResponse<List<TicketResponse>> getTickets(
            TicketSearchCondition condition
    ) {
        List<TicketResponse> responses = ticketService.searchTickets(condition);
        return ApiResponse.success(responses);
    }

    /**
     * [GET] 티켓 상세 조회
     * - URL: /api/tickets/{ticketId}
     * - 인증 불필요
     */
    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<TicketResponse> getTicketDetail(
            @PathVariable Long ticketId
    ) {
        TicketResponse response = ticketService.getTicketDetail(ticketId);
        return ApiResponse.success(response);
    }

    /**
     * [GET] 판매자 본인 티켓 조회
     * - URL: /api/sellers/tickets
     * - 인증 필요
     */
    @GetMapping("/sellers/tickets")
    public ApiResponse<List<TicketResponse>> getMyTickets(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        List<TicketResponse> responses = ticketService.searchSellerTickets(userId);
        return ApiResponse.success(responses);
    }

    /**
     * [PUT] 티켓 수정
     * - URL: /api/sellers/tickets/{ticketId}
     * - 인증 필요
     * - multipart/form-data
     */
    @PutMapping(
            value = "/sellers/tickets/{ticketId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<TicketResponse> updateTicket(
            Authentication authentication,
            @PathVariable Long ticketId,
            @Valid @ModelAttribute TicketUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        TicketResponse response =
                ticketService.updateTicket(ticketId, userId, request);
        return ApiResponse.success(response);
    }

    /**
     * [DELETE] 티켓 삭제
     * - URL: /api/sellers/tickets/{ticketId}
     * - 인증 필요
     */
    @DeleteMapping("/sellers/tickets/{ticketId}")
    public ApiResponse<Void> deleteTicket(
            Authentication authentication,
            @PathVariable Long ticketId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        ticketService.deleteTicket(ticketId, userId);
        return ApiResponse.success(null);
    }

    /**
     * [PUT] 티켓 상태 변경
     * - URL: /api/tickets/{ticketId}/status/{newStatus}
     * - 인증 필요
     */
    @PutMapping("/tickets/{ticketId}/status/{newStatus}")
    public ResponseEntity<?> updateTicketStatus(
            Authentication authentication,
            @PathVariable Long ticketId,
            @PathVariable String newStatus
    ) {
        try {
            Long userId = (Long) authentication.getPrincipal();

            TicketResponse updatedTicket =
                    ticketService.updateTicketStatus(ticketId, userId, newStatus);

            return ResponseEntity.ok(ApiResponse.success(updatedTicket));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("유효하지 않은 티켓 상태: " + newStatus);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("티켓 상태 변경 중 서버 오류 발생");
        }
    }
}
