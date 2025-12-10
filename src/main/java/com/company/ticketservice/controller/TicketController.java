package com.company.ticketservice.controller;

import com.company.ticketservice.dto.*;
import com.company.ticketservice.service.AuthService;
import com.company.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class TicketController {

    private final TicketService ticketService;
    private final AuthService authService;

    /**
     *  [POST] 티켓 등록
     *   - URL: /sellers/tickets
     *   - 판매자가 티켓을 등록
     */
    @PostMapping("/sellers/tickets")
    public ApiResponse<TicketResponse> createTicket(@RequestBody TicketCreateRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ApiResponse.success(response);
    }

    /**
     *  [GET] 티켓 리스트 조회 및 필터링
     *   - URL: /tickets
     *   - 사용자 누구나 조회 가능
     */
    @GetMapping("/tickets")
    public ApiResponse<List<TicketResponse>> getTickets(TicketSearchCondition condition) {
        List<TicketResponse> responses = ticketService.searchTickets(condition);
        return ApiResponse.success(responses);
    }

    /**
     *  [GET] 등록 티켓 조회 (판매자 본인 티켓 조회)
     *   - URL: /sellers/tickets
     */
    @GetMapping("/sellers/tickets")
    public ApiResponse<List<TicketResponse>> getMyTickets() {
        Long currentUserId = authService.getCurrentUserId(); // 로그인한 사용자 ID
        List<TicketResponse> responses = ticketService.searchSellerTickets(currentUserId);
        return ApiResponse.success(responses);
    }



    /**
     * [PUT] 티켓 수정
     * - URL: /sellers/tickets/{ticketId}
     * - 판매자 본인이 등록한 티켓 수정
     */
    @PutMapping("/sellers/tickets/{ticketId}")
    public ApiResponse<TicketResponse> updateTicket(
            @PathVariable Long ticketId,
            @RequestBody TicketUpdateRequest request
    ) {
        TicketResponse response = ticketService.updateTicket(ticketId, request);
        return ApiResponse.success(response);
    }

    /**
     * [DELETE] 티켓 삭제
     * - URL: /sellers/tickets/{ticketId}
     * - 판매자 본인이 등록한 티켓 삭제
     */
    @DeleteMapping("/sellers/tickets/{ticketId}")
    public ApiResponse<Void> deleteTicket(
            @PathVariable Long ticketId
    ) {
        ticketService.deleteTicket(ticketId);
        return ApiResponse.success(null);
    }

}
