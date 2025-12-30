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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    private static final String UPLOAD_DIR = "uploads/"; // 로컬 이미지 저장 경로

    /**
     * 티켓 생성 (판매자)
     * - 인증된 userId는 Controller에서 전달받음
     * - ownerId는 request에서 받지 않고 userId로 강제 설정
     */
    public TicketResponse createTicket(Long userId, TicketCreateRequest request) {
        validateCreateRequest(request);

        String savedImage1 = saveImageFile(request.getImage1());
        String savedImage2 = saveImageFile(request.getImage2());

        Ticket ticket = Ticket.builder()
                .eventName(request.getEventName())
                .eventDate(request.getEventDate())
                .eventLocation(request.getEventLocation())
                .ownerId(userId) // JWT로 인증된 사용자로 강제
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
     * 티켓 생성 시 검증 로직
     */
    private void validateCreateRequest(TicketCreateRequest request) {

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

        if (request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("공연 날짜는 현재 시점 이후여야 합니다.");
        }

        BigDecimal originalPrice = request.getOriginalPrice();
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("원래 가격은 0보다 큰 값이어야 합니다.");
        }

        if (request.getSellingPrice() != null) {
            BigDecimal sellingPrice = request.getSellingPrice();

            if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("판매 가격은 0보다 큰 값이어야 합니다.");
            }

            if (sellingPrice.compareTo(originalPrice) > 0) {
                throw new BadRequestException("판매 가격은 원래 가격을 초과할 수 없습니다.");
            }
        }
    }

    /**
     * 티켓 검색 (공개)
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
     * 티켓 상세 조회 (공개)
     */
    public TicketResponse getTicketDetail(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("티켓 ID: " + ticketId + "에 해당하는 티켓을 찾을 수 없습니다."));
        return TicketResponse.fromEntity(ticket);
    }

    /**
     * 판매자 본인 티켓 조회 (인증 필요)
     * - Controller에서 userId 전달
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
     * 티켓 수정 (인증 + 인가 필요)
     * - 본인(ownerId) 티켓만 수정 가능
     */
    public TicketResponse updateTicket(Long ticketId, Long userId, TicketUpdateRequest request) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("해당 티켓을 찾을 수 없습니다."));

        // 인가: 본인 티켓만
        if (!Objects.equals(ticket.getOwnerId(), userId)) {
            throw new BadRequestException("본인이 등록한 티켓만 수정할 수 있습니다.");
        }

        // 거래 중 상태 확인
        if (!ticket.getTicketStatus().isUpdatable()) {
            throw new BadRequestException("현재 상태에서는 티켓을 수정할 수 없습니다.");
        }


        validateUpdateRequest(request, ticket);

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

        Ticket updatedTicket = ticketRepository.save(ticket);
        return TicketResponse.fromEntity(updatedTicket);
    }

    private void validateUpdateRequest(TicketUpdateRequest request, Ticket ticket) {

        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("공연 날짜는 현재 시점 이후여야 합니다.");
        }

        if (request.getOriginalPrice() != null &&
                request.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("원래 가격은 0보다 커야 합니다.");
        }

        if (request.getSellingPrice() != null) {

            if (request.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("판매 가격은 0보다 커야 합니다.");
            }

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
     * 티켓 삭제 (인증 + 인가 필요)
     * - 본인(ownerId) 티켓만 삭제 가능
     */
    public void deleteTicket(Long ticketId, Long userId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("해당 티켓을 찾을 수 없습니다."));

        // 인가: 본인 티켓만
        if (!Objects.equals(ticket.getOwnerId(), userId)) {
            throw new BadRequestException("본인이 등록한 티켓만 삭제할 수 있습니다.");
        }

        if (!ticket.getTicketStatus().isDeletable()) {
            throw new BadRequestException("현재 상태에서는 티켓을 삭제할 수 없습니다.");
        }


        ticketRepository.delete(ticket);
    }

    /**
     * 티켓 상태 변경 (인증 필요)
     * - 여기서 '누가 변경할 수 있는지' 정책을 정해야 함
     *   (예: 판매자만? 구매자도? 관리자만?)
     * - 우선 안전하게: '판매자(소유자)만 변경 가능'으로 구현
     */
    @Transactional
    public TicketResponse updateTicketStatus(Long ticketId, Long userId, String newStatusString) {

        TicketStatus newStatus;
        try {
            newStatus = TicketStatus.valueOf(newStatusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("존재하지 않는 티켓 상태 값입니다: " + newStatusString);
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + ticketId + "인 티켓을 찾을 수 없습니다."));

        // 인가(안전): 소유자만 상태 변경 가능
        if (!Objects.equals(ticket.getOwnerId(), userId)) {
            throw new BadRequestException("본인 티켓만 상태를 변경할 수 있습니다.");
        }

        if (!ticket.getTicketStatus().canChangeTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("현재 상태 (%s)에서는 %s 상태로 변경할 수 없습니다.",
                            ticket.getTicketStatus(), newStatus)
            );
        }


        ticket.setTicketStatus(newStatus);
        return TicketResponse.fromEntity(ticket);
    }


}
