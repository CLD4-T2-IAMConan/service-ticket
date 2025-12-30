package com.company.ticketservice.controller;

import com.company.ticketservice.dto.*;
import com.company.ticketservice.service.TicketService;
import com.company.ticketservice.service.FavoriteService;
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
    private final FavoriteService favoriteService;

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
     * [GET] 티켓 리스트 조회 및 필터링 (페이지네이션 지원)
     * - URL: /tickets?page=0&size=20&eventName=검색어&ticketStatus=AVAILABLE&sortBy=eventDate&sortDirection=ASC
     * - 사용자 누구나 조회 가능
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

    /**
     * [POST] 찜하기 추가/제거 (토글)
     * - URL: /api/tickets/{ticketId}/favorite
     * - 로그인 필요
     */
    @PostMapping("/tickets/{ticketId}/favorite")
    public ApiResponse<Boolean> toggleFavorite(
            Authentication authentication,
            @PathVariable Long ticketId
    ) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            boolean isFavorite = favoriteService.toggleFavorite(userId, ticketId);
            return ApiResponse.success(isFavorite);
        } catch (IllegalStateException e) {
            // 인증 실패 시 401 반환
            return ApiResponse.error("인증이 필요합니다: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("찜하기 처리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * [GET] 찜하기 여부 확인
     * - URL: /api/tickets/{ticketId}/favorite
     * - 로그인 필요
     */
    @GetMapping("/tickets/{ticketId}/favorite")
    public ApiResponse<Boolean> checkFavorite(
            Authentication authentication,
            @PathVariable Long ticketId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isFavorite = favoriteService.isFavorite(userId, ticketId);
        return ApiResponse.success(isFavorite);
    }
}
