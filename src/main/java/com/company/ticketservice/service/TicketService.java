package com.company.ticketservice.service;

import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.dto.TicketResponse;
import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.dto.TicketUpdateRequest;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.exception.BadRequestException;
import com.company.ticketservice.exception.NotFoundException;
import com.company.ticketservice.repository.TicketRepository;
import com.company.ticketservice.repository.TicketSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AuthService authService;

    private static final String UPLOAD_DIR = "uploads/"; // 로컬 이미지 저장 경로

    /**
     *  티켓 생성
     * - DTO → 엔티티 변환
     * - 기본 상태값 처리
     * - DB 저장
     * - Response DTO 반환
     */
    public TicketResponse createTicket(TicketCreateRequest request) {
        validateCreateRequest(request);

        // 이미지 저장 처리
        String savedImage1 = saveImageFile(request.getImage1());
        String savedImage2 = saveImageFile(request.getImage2());


        Ticket ticket = Ticket.builder()
                .eventName(request.getEventName())
                .eventDate(request.getEventDate())
                .eventLocation(request.getEventLocation())
                .ownerId(request.getOwnerId())
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(request.getOriginalPrice())
                .sellingPrice(request.getSellingPrice())
                .seatInfo(request.getSeatInfo())
                .ticketType(request.getTicketType())
                .categoryId(request.getCategoryId())
                .image1(savedImage1)
                .image2(savedImage2)
                .description(request.getDescription())
                .tradeType(request.getTradeType())
                .build();

        Ticket saved = ticketRepository.save(ticket);

        return TicketResponse.fromEntity(saved);
    }

    /**
     * 이미지 파일 저장 (로컬)
     */
    private String saveImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            File file = new File(dir, fileName);
            imageFile.transferTo(file);

            return fileName; // DB에는 파일명만 저장
        } catch (IOException e) {
            throw new BadRequestException("이미지 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     *  티켓 생성 시 검증 로직
     */
    private void validateCreateRequest(TicketCreateRequest request) {

        // 1) 필수 필드 체크
        if (request.getEventName() == null || request.getEventName().isBlank()) {
            throw new BadRequestException("공연/이벤트 이름은 필수 입력값입니다.");
        }

        if (request.getEventDate() == null) {
            throw new BadRequestException("공연 날짜는 필수 입력값입니다.");
        }

        if (request.getEventLocation() == null || request.getEventLocation().isBlank()) {
            throw new BadRequestException("공연 장소는 필수 입력값입니다.");
        }

        if (request.getOriginalPrice() == null) {
            throw new BadRequestException("원래 가격은 필수 입력값입니다.");
        }

        if (request.getCategoryId() == null) {
            throw new BadRequestException("카테고리는 필수 입력값입니다.");
        }

        if (request.getTradeType() == null) {
            throw new BadRequestException("거래 방식은 필수 입력값입니다.");
        }


        // 2) 공연 날짜가 현재 시점보다 이전인지 체크
        if (request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("공연 날짜는 현재 시점 이후여야 합니다.");
        }

        // 3) 가격 유효성 체크
        BigDecimal originalPrice = request.getOriginalPrice();
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("원래 가격은 0보다 큰 값이어야 합니다.");
        }

        if (request.getSellingPrice() != null) {
            BigDecimal sellingPrice = request.getSellingPrice();

            if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("판매 가격은 0보다 큰 값이어야 합니다.");
            }

            // 정책: 판매 가격이 원래 가격보다 클 수 없다 (선택)
            if (sellingPrice.compareTo(originalPrice) > 0) {
                throw new BadRequestException("판매 가격은 원래 가격을 초과할 수 없습니다.");
            }
        }
    }



    /**
     *  티켓 검색
     */
    public List<TicketResponse> searchTickets(TicketSearchCondition condition) {

        List<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition)
        );

        if (tickets.isEmpty()) {
            throw new NotFoundException("검색 조건에 해당하는 티켓이 없습니다.");
        }

        return tickets.stream()
                .map(TicketResponse::fromEntity)
                .toList();
    }

    /**
     * 티켓 상세 정보를 조회
     * @param ticketId 조회할 티켓의 고유 ID
     * @return 티켓 상세 응답 DTO
     */
    public TicketResponse getTicketDetail(Long ticketId) {
        // 1. ID로 티켓 엔티티를 조회
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("티켓 ID: " + ticketId + "에 해당하는 티켓을 찾을 수 없습니다."));

        // 2. 엔티티를 Response DTO로 변환하여 반환
        return TicketResponse.fromEntity(ticket);
    }

    /**
     *  판매자 본인 티켓만 조회
     * - Controller에서 ownerId만 받았을 때 사용 가능
     */
    public List<TicketResponse> searchSellerTickets(Long ownerId) {
        TicketSearchCondition condition = new TicketSearchCondition();
        condition.setOwnerId(ownerId);

        List<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition)
        );

        if (tickets.isEmpty()) {
            throw new NotFoundException("등록한 티켓이 없습니다.");
        }

        return tickets.stream()
                .map(TicketResponse::fromEntity)
                .toList();
    }

    /**
     *  티켓 수정
     */
    public TicketResponse updateTicket(Long ticketId, TicketUpdateRequest request) {

        // 1. 티켓 존재 여부 확인
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("해당 티켓을 찾을 수 없습니다."));

        // 2. 로그인 사용자 ID 가져오기
        Long currentUserId = authService.getCurrentUserId();

        // 3. 소유자 검증
        if (!Objects.equals(ticket.getOwnerId(), currentUserId)) {
            throw new BadRequestException("본인이 등록한 티켓만 수정할 수 있습니다.");
        }

        // 4. 거래 중 상태 확인 (RESERVED or SOLD이면 수정 불가)
        if (ticket.getTicketStatus() == TicketStatus.RESERVED ||
                ticket.getTicketStatus() == TicketStatus.SOLD) {
            throw new BadRequestException("거래 중인 티켓은 수정할 수 없습니다.");
        }

        // 수정 시 값 유효성 검증
        validateUpdateRequest(request, ticket);

        // 5. null 또는 빈 값이 아닌 경우만 업데이트
        if (request.getEventName() != null && !request.getEventName().isBlank()) {
            ticket.setEventName(request.getEventName());
        }

        if (request.getEventDate() != null) {
            ticket.setEventDate(request.getEventDate());
        }

        if (request.getEventLocation() != null) {
            ticket.setEventLocation(request.getEventLocation());
        }

        if (request.getOriginalPrice() != null) {
            ticket.setOriginalPrice(request.getOriginalPrice());
        }

        if (request.getSellingPrice() != null) {
            ticket.setSellingPrice(request.getSellingPrice());
        }

        if (request.getSeatInfo() != null) {
            ticket.setSeatInfo(request.getSeatInfo());
        }

        if (request.getTicketType() != null) {
            ticket.setTicketType(request.getTicketType());
        }

        if (request.getCategoryId() != null) ticket.setCategoryId(request.getCategoryId());

        if (request.getDescription() != null) ticket.setDescription(request.getDescription());

        if (request.getTradeType() != null) ticket.setTradeType(request.getTradeType());

        if (request.getImage1() != null && !request.getImage1().isEmpty()) {
            ticket.setImage1(saveImageFile(request.getImage1()));
        }
        if (request.getImage2() != null && !request.getImage2().isEmpty()) {
            ticket.setImage2(saveImageFile(request.getImage2()));
        }



        // 6. 업데이트된 내용 저장
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 7. Response DTO로 변환하여 반환
        return TicketResponse.fromEntity(updatedTicket);
    }

    // 티켓 수정 시 검증 로직
    private void validateUpdateRequest(TicketUpdateRequest request, Ticket ticket) {

        // 날짜 검증
        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("공연 날짜는 현재 시점 이후여야 합니다.");
        }

        // 가격 검증
        if (request.getOriginalPrice() != null &&
                request.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("원래 가격은 0보다 커야 합니다.");
        }

        if (request.getSellingPrice() != null) {

            if (request.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("판매 가격은 0보다 커야 합니다.");
            }

            // originalPrice는 수정 중일 수도 있고 아닐 수도 있음
            BigDecimal baseOriginalPrice = request.getOriginalPrice() != null
                    ? request.getOriginalPrice()
                    : ticket.getOriginalPrice();

            if (request.getSellingPrice().compareTo(baseOriginalPrice) > 0) {
                throw new BadRequestException("판매 가격은 원래 가격을 초과할 수 없습니다.");
            }
        }

        if (request.getCategoryId() != null && request.getCategoryId() <= 0) {
            throw new BadRequestException("카테고리 ID는 0보다 큰 값이어야 합니다.");
        }

    }

    /**
     *  티켓 삭제
     */
    public void deleteTicket(Long ticketId) {

        // 1. 티켓 존재 여부 확인
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("해당 티켓을 찾을 수 없습니다."));

        // 2. 로그인 사용자 ID 가져오기
        Long currentUserId = authService.getCurrentUserId();

        // 3. 소유자 검증
        if (!Objects.equals(ticket.getOwnerId(), currentUserId)) {
            throw new BadRequestException("본인이 등록한 티켓만 삭제할 수 있습니다.");
        }

        // 4. 거래 중이면 삭제 불가
        if (ticket.getTicketStatus() == TicketStatus.RESERVED ||
                ticket.getTicketStatus() == TicketStatus.SOLD) {
            throw new BadRequestException("거래 중인 티켓은 삭제할 수 없습니다.");
        }

        // 5. 삭제 수행
        ticketRepository.delete(ticket);
    }


    @Transactional
    public TicketResponse updateTicketStatus(Long ticketId, String newStatusString) {

        // 1. Enum 파싱 및 유효성 검증
        TicketStatus newStatus;
        try {
            // 입력받은 문자열을 Enum으로 변환
            newStatus = TicketStatus.valueOf(newStatusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 유효하지 않은 Enum 값일 경우 예외 발생 (Controller에서 400 처리)
            throw new IllegalArgumentException("존재하지 않는 티켓 상태 값입니다: " + newStatusString);
        }

        // 2. 티켓 조회 (EntityNotFoundException 처리)
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + ticketId + "인 티켓을 찾을 수 없습니다."));

        // 3. 비즈니스 상태 전이 규칙 검증 (핵심)
        if (!canChangeStatus(ticket.getTicketStatus(), newStatus)) {
            throw new IllegalStateException(
                    String.format("현재 상태 (%s)에서는 %s 상태로 변경할 수 없습니다.",
                            ticket.getTicketStatus(), newStatus)
            );
        }

        // 4. 상태 변경 및 저장 (Dirty Checking)
        ticket.setTicketStatus(newStatus);

        // 5. 응답 DTO 반환
        return TicketResponse.fromEntity(ticket); // 🚨 DTO 변환 메서드는 TicketResponse에 정의되어 있어야 합니다.
    }

    /**
     * 상태 전이 규칙을 검증하는 내부 메서드
     */
    private boolean canChangeStatus(TicketStatus current, TicketStatus target) {
        if (current == target) {
            // 상태가 이미 목표 상태라면 변경할 필요 없음 (성공으로 간주)
            return true;
        }
        // Case 1: AVAILABLE -> RESERVED (거래 요청 시작)
        if (current == TicketStatus.AVAILABLE && target == TicketStatus.RESERVED) {
            return true;
        }
        // Case 2: RESERVED -> AVAILABLE (거래 요청 취소/실패)
        if (current == TicketStatus.RESERVED && target == TicketStatus.AVAILABLE) {
            return true;
        }
        // Case 3: RESERVED -> SOLD (거래 최종 확정)
        if (current == TicketStatus.RESERVED && target == TicketStatus.SOLD) {
            return true;
        }
        // Case 4: AVAILABLE/RESERVED -> EXPIRED (관리자 취소 등)
        if ((current == TicketStatus.AVAILABLE || current == TicketStatus.RESERVED) && target == TicketStatus.EXPIRED) {
            return true;
        }

        // 이미 SOLD나 EXPIRED 상태는 변경 불가능하다고 가정
        if (current == TicketStatus.EXPIRED) {
            return false;
        }

        // 그 외 모든 전이는 불가능
        return false;
    }




}
