package com.company.ticketservice.controller;

import com.company.ticketservice.dto.*;
import com.company.ticketservice.service.AuthService;
import com.company.ticketservice.service.TicketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class TicketController {

    private final TicketService ticketService;
    private final AuthService authService;

    /**
     *  [POST] 티켓 등록
     *   - URL: /sellers/tickets
     *   - 판매자가 티켓을 등록
     *   - multipart/form-data (이미지 포함)
     */
    @PostMapping(
            value = "/sellers/tickets",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<TicketResponse> createTicket(@Valid @ModelAttribute TicketCreateRequest request) {

        // 로그인 붙일 때 여기에서 ownerId 넣어주는 방식이 가장 안전함
        request.setOwnerId(authService.getCurrentUserId());

        TicketResponse response = ticketService.createTicket(request);
        return ApiResponse.success(response);
    }

           /**
            *  [GET] 티켓 리스트 조회 및 필터링 (페이지네이션 지원)
            *   - URL: /tickets?page=0&size=20&eventName=검색어&ticketStatus=AVAILABLE&sortBy=eventDate&sortDirection=ASC
            *   - 사용자 누구나 조회 가능
            */
           @GetMapping("/tickets")
           public ApiResponse<PageResponse<TicketResponse>> getTickets(
                   TicketSearchCondition condition,
                   @RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "20") int size,
                   @RequestParam(required = false) String sortBy,
                   @RequestParam(required = false) String sortDirection
           ) {
               PageResponse<TicketResponse> responses = ticketService.searchTickets(condition, page, size, sortBy, sortDirection);
               return ApiResponse.success(responses);
           }

    /**
     * [GET] 티켓 상세 정보 조회
     * - URL: /tickets/{ticketId}
     * - 사용자 누구나 조회 가능
     * @param ticketId URL 경로 변수 (ticketId)
     * @return 200 OK와 함께 티켓 상세 정보 (JSON) 반환
     */
    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<TicketResponse> getTicketDetail(@PathVariable Long ticketId) {
        // 티켓 ID를 사용하여 서비스 레이어에서 상세 정보를 조회합니다.
        TicketResponse response = ticketService.getTicketDetail(ticketId);
        return ApiResponse.success(response);
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
     * - multipart/form-data (이미지 포함)
     */
    @PutMapping(
            value = "/sellers/tickets/{ticketId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<TicketResponse> updateTicket(
            @PathVariable Long ticketId,
            @Valid @ModelAttribute TicketUpdateRequest request
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

    /**
     * [PUT] 특정 티켓의 상태를 지정된 새 상태로 변경합니다.
     * URL: PUT /tickets/{ticketId}/status/{newStatus}
     *
     * @param ticketId 변경할 티켓의 ID
     * @param newStatus 변경할 목표 상태 (예: RESERVED, SOLD, AVAILABLE, CANCELLED)
     * @return 변경된 티켓의 응답 DTO (TicketResponse)
     */
    @PutMapping("/tickets/{ticketId}/status/{newStatus}")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable Long ticketId,
            @PathVariable String newStatus
    ) {
        try {
            // 1. 서비스에 상태 변경 요청을 위임
            // 🚨 TicketService에 해당 메서드를 정의해야 합니다.
            TicketResponse updatedTicket = ticketService.updateTicketStatus(ticketId, newStatus);

            // 2. 성공 시 200 OK와 함께 변경된 티켓 정보 반환
            return ResponseEntity.ok(ApiResponse.success(updatedTicket)); // 🚨 ApiResponse.success() 사용

        } catch (IllegalArgumentException e) {
            // newStatus가 유효하지 않은 TicketStatus Enum 값일 경우
            return ResponseEntity.badRequest().body("유효하지 않은 티켓 상태: " + newStatus);
        } catch (EntityNotFoundException e) {
            // 티켓 ID를 찾을 수 없는 경우
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // 현재 상태에서 목표 상태로 변경할 수 없는 경우 (예: 이미 SOLD인데 RESERVED로 변경 시도)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 기타 서버 내부 오류
            return ResponseEntity.internalServerError().body("티켓 상태 변경 중 서버 오류 발생.");
        }
    }

    /**
     * [POST] 관리자용 - 티켓 시드 데이터 추가
     * - URL: /api/admin/tickets/seed
     * - 개발용: 인증 없이 사용 가능
     */
    @PostMapping("/admin/tickets/seed")
    @CrossOrigin(origins = "*") // 개발용으로 모든 origin 허용
    public ApiResponse<String> seedTickets() {
        try {
            // DataInitializer의 로직을 직접 호출
            ticketService.seedTickets();
            long count = ticketService.countAvailableFutureTickets();
            return ApiResponse.success("티켓 시드 데이터가 성공적으로 추가되었습니다. 현재 미래 날짜 AVAILABLE 티켓: " + count + "개");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("티켓 시드 데이터 추가 중 오류 발생: " + e.getMessage());
        }
    }

}
