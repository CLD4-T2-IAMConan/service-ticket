package com.company.ticketservice.controller;

import com.company.ticketservice.dto.ApiResponse;
import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.dto.TicketResponse;
import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class TicketController {

    private final TicketService ticketService;

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
     *   - URL: /sellers/tickets?ownerId=1
     */
    @GetMapping("/sellers/tickets")
    public ApiResponse<List<TicketResponse>> getSellerTickets(@RequestParam Long ownerId) {
        List<TicketResponse> responses = ticketService.searchSellerTickets(ownerId);
        return ApiResponse.success(responses);
    }
}
