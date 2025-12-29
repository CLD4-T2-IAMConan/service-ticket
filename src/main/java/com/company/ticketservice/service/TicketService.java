package com.company.ticketservice.service;

import com.company.ticketservice.dto.PageResponse;
import com.company.ticketservice.dto.TicketCreateRequest;
import com.company.ticketservice.dto.TicketResponse;
import com.company.ticketservice.dto.TicketSearchCondition;
import com.company.ticketservice.dto.TicketUpdateRequest;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
import com.company.ticketservice.exception.BadRequestException;
import com.company.ticketservice.exception.NotFoundException;
import com.company.ticketservice.repository.TicketRepository;
import com.company.ticketservice.repository.TicketSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * 로컬 개발 환경에서는 이미지 저장을 건너뛰고 파일명만 반환
     */
    private String saveImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        
        // 로컬 개발 환경: 실제 파일 저장 없이 파일명만 반환
        // 운영 환경에서는 S3 등으로 업로드하도록 변경 필요
        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        // 파일명만 생성하여 반환 (실제 파일 저장은 건너뜀)
        String fileName = UUID.randomUUID() + "_" + originalFilename;
        
        // 로컬 파일 저장 시도 (선택적, 실패해도 계속 진행)
        try {
            File dir = new File(UPLOAD_DIR);
            if (dir.exists() || dir.mkdirs()) {
                File file = new File(dir, fileName);
                imageFile.transferTo(file);
            }
        } catch (IOException | SecurityException e) {
            // 파일 저장 실패해도 파일명은 반환 (로컬 개발 환경 대응)
            // 로그만 남기고 계속 진행
        }
        
        return fileName; // DB에는 파일명만 저장
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
     *  티켓 검색 (페이지네이션 지원)
     */
    public PageResponse<TicketResponse> searchTickets(TicketSearchCondition condition, int page, int size, String sortBy, String sortDirection) {
        // 정렬 설정
        Sort sort;
        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            
            // 필드명 매핑
            String fieldName;
            String lowerSortBy = sortBy.toLowerCase();
            if (lowerSortBy.equals("createdat") || lowerSortBy.equals("created_at") || lowerSortBy.equals("date")) {
                fieldName = "createdAt";
            } else if (lowerSortBy.equals("eventdate") || lowerSortBy.equals("event_date")) {
                fieldName = "eventDate";
            } else if (lowerSortBy.equals("price") || lowerSortBy.equals("sellingprice") || lowerSortBy.equals("selling_price")) {
                fieldName = "sellingPrice";
            } else if (lowerSortBy.equals("originalprice") || lowerSortBy.equals("original_price")) {
                fieldName = "originalPrice";
            } else {
                fieldName = "eventDate"; // 기본값
            }
            
            sort = Sort.by(direction, fieldName);
            // 이벤트 날짜 정렬이 아닌 경우, 보조 정렬로 이벤트 날짜 추가
            if (!"eventDate".equals(fieldName) && !"createdAt".equals(fieldName)) {
                // createdAt 정렬의 경우 보조 정렬로 이벤트 날짜 추가하지 않음 (null 값 처리)
                sort = sort.and(Sort.by(Sort.Direction.ASC, "eventDate"));
            }
            // createdAt이 null인 경우를 대비해 nullsLast 추가
            if ("createdAt".equals(fieldName)) {
                sort = sort.and(Sort.by(Sort.Direction.ASC, "eventDate")); // 보조 정렬
            }
        } else {
            // 기본 정렬: 이벤트 날짜 오름차순 (가까운 날짜부터), 그 다음 생성일 내림차순 (최신순)
            sort = Sort.by(
                    Sort.Order.asc("eventDate"),  // 이벤트 날짜가 가까운 순
                    Sort.Order.desc("createdAt")  // 같은 날짜면 최신순
            );
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Ticket> ticketPage = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition),
                pageable
        );

        List<TicketResponse> content = ticketPage.getContent().stream()
                .map(TicketResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                content,
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isFirst(),
                ticketPage.isLast()
        );
    }

    /**
     *  티켓 검색 (페이지네이션 없이 - 하위 호환성)
     */
    @Deprecated
    public List<TicketResponse> searchTickets(TicketSearchCondition condition) {
        List<Ticket> tickets = ticketRepository.findAll(
                TicketSpecification.fromCondition(condition)
        );

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
    /**
     * 관리자용 - 티켓 시드 데이터 생성
     */
    public void seedTickets() {
        long availableFutureTickets = countAvailableFutureTickets();

        if (availableFutureTickets < 100) {
            initializeFutureTickets();
        }
    }

    /**
     * 미래 날짜의 AVAILABLE 티켓 개수 조회
     */
    public long countAvailableFutureTickets() {
        LocalDateTime now = LocalDateTime.now();
        return ticketRepository.findAll().stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.AVAILABLE)
                .filter(t -> t.getEventDate().isAfter(now))
                .count();
    }

    /**
     * 미래 날짜의 티켓 생성 (DataInitializer와 동일한 로직)
     */
    private void initializeFutureTickets() {
        // 미래 날짜의 티켓 100개 생성 (현재 날짜 기준으로 1~6개월 후)
        List<Ticket> futureTickets = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 티켓 템플릿 데이터
        String[][] concertTemplates = {
            {"NewJeans 콘서트", "올림픽공원 체조경기장", "150000", "145000", "VIP석"},
            {"에스파 월드투어", "잠실실내체육관", "180000", "175000", "R석"},
            {"세븐틴 팬미팅", "고척스카이돔", "120000", "115000", "A석"},
            {"아이브 콘서트", "잠실실내체육관", "160000", "155000", "S석"},
            {"르세라핌 쇼케이스", "올림픽공원 올림픽홀", "140000", "135000", "스탠딩"},
            {"블랙핑크 콘서트", "잠실종합운동장", "200000", "195000", "VIP석"},
            {"트와이스 월드투어", "고척스카이돔", "170000", "165000", "R석"},
            {"레드벨벳 팬미팅", "올림픽공원 체조경기장", "130000", "125000", "A석"},
            {"NCT 콘서트", "잠실실내체육관", "190000", "185000", "VIP석"},
            {"스트레이키즈 쇼케이스", "올림픽공원 올림픽홀", "150000", "145000", "스탠딩"}
        };

        String[][] musicalTemplates = {
            {"뮤지컬 위키드", "샤롯데씨어터", "150000", "145000", "VIP석"},
            {"뮤지컬 맘마미아", "블루스퀘어", "130000", "128000", "R석"},
            {"뮤지컬 레미제라블", "샤롯데씨어터", "140000", "135000", "VIP석"},
            {"뮤지컬 오페라의 유령", "블루스퀘어", "160000", "155000", "R석"},
            {"뮤지컬 캣츠", "예술의전당 오페라극장", "120000", "118000", "S석"},
            {"뮤지컬 시카고", "샤롯데씨어터", "135000", "130000", "VIP석"},
            {"뮤지컬 지킬앤하이드", "블루스퀘어", "145000", "140000", "R석"},
            {"뮤지컬 드라큘라", "예술의전당 오페라극장", "125000", "120000", "S석"}
        };

        String[][] sportsTemplates = {
            {"K리그 올스타전", "서울월드컵경기장", "40000", "38000", "중앙석"},
            {"한화 이글스 홈경기", "대전한화생명이글스파크", "28000", "25000", "1루석"},
            {"FC서울 홈경기", "서울월드컵경기장", "35000", "32000", "북측 응원석"},
            {"두산 베어스 홈경기", "잠실야구장", "30000", "28000", "1루 테이블석"},
            {"롯데 자이언츠 홈경기", "사직야구장", "30000", "28000", "중앙 블루석"},
            {"LG 트윈스 홈경기", "잠실야구장", "32000", "30000", "3루석"},
            {"KT 위즈 홈경기", "수원KT위즈파크", "25000", "23000", "중앙석"},
            {"SSG 랜더스 홈경기", "인천SSG랜더스필드", "28000", "26000", "1루석"}
        };

        String[][] exhibitionTemplates = {
            {"반 고흐와 고갱 특별전", "국립중앙박물관", "20000", "19000", "성인 입장권"},
            {"피카소 특별전", "예술의전당 한가람미술관", "22000", "20000", "성인 1매"},
            {"모네 인상주의 특별전", "국립중앙박물관", "18000", "17000", "일반 입장권"},
            {"클림트 특별전", "예술의전당 한가람미술관", "25000", "23000", "성인 입장권"},
            {"뭉크 특별전", "국립중앙박물관", "20000", "19000", "성인 1매"},
            {"르누아르 특별전", "예술의전당 한가람미술관", "21000", "20000", "성인 입장권"}
        };

        String[][] classicTemplates = {
            {"베를린 필하모닉 오케스트라", "롯데콘서트홀", "100000", "95000", "VIP석"},
            {"서울시향 정기연주회", "예술의전당 콘서트홀", "60000", "58000", "R석"},
            {"조성진 피아노 독주회", "예술의전당 콘서트홀", "80000", "75000", "VIP석"},
            {"빈 필하모닉 오케스트라", "롯데콘서트홀", "120000", "115000", "VIP석"},
            {"런던 심포니 오케스트라", "예술의전당 콘서트홀", "110000", "105000", "VIP석"},
            {"서울시향 봄 정기연주회", "예술의전당 콘서트홀", "60000", "58000", "R석"}
        };

        // 티켓 생성 (총 100개)
        int ticketCount = 0;
        int monthOffset = 1;

        // 콘서트 티켓 30개
        for (int i = 0; i < 30 && ticketCount < 100; i++) {
            String[] template = concertTemplates[i % concertTemplates.length];
            LocalDateTime eventDate = now.plusMonths(monthOffset).plusDays(i % 30)
                    .withHour(18 + (i % 4)).withMinute((i % 2) * 30);
            futureTickets.add(createSeedTicket(
                    template[0] + " " + (i + 1),
                    eventDate,
                    template[1],
                    1L,
                    1L, // 콘서트
                    new BigDecimal(template[2]),
                    new BigDecimal(template[3]),
                    template[4] + " " + (i % 20 + 1) + "번",
                    template[0] + " 티켓입니다.",
                    i % 2 == 0 ? TradeType.DELIVERY : TradeType.ONSITE
            ));
            ticketCount++;
            if (i % 10 == 9) monthOffset++;
        }

        // 뮤지컬 티켓 25개
        monthOffset = 1;
        for (int i = 0; i < 25 && ticketCount < 100; i++) {
            String[] template = musicalTemplates[i % musicalTemplates.length];
            LocalDateTime eventDate = now.plusMonths(monthOffset).plusDays(i % 30)
                    .withHour(14 + (i % 2) * 5).withMinute((i % 2) * 30);
            futureTickets.add(createSeedTicket(
                    template[0] + " " + (i + 1),
                    eventDate,
                    template[1],
                    1L,
                    2L, // 뮤지컬
                    new BigDecimal(template[2]),
                    new BigDecimal(template[3]),
                    template[4] + " " + (i % 15 + 1) + "번",
                    template[0] + " 공연 티켓입니다.",
                    i % 2 == 0 ? TradeType.DELIVERY : TradeType.ONSITE
            ));
            ticketCount++;
            if (i % 10 == 9) monthOffset++;
        }

        // 스포츠 티켓 25개
        monthOffset = 1;
        for (int i = 0; i < 25 && ticketCount < 100; i++) {
            String[] template = sportsTemplates[i % sportsTemplates.length];
            LocalDateTime eventDate = now.plusMonths(monthOffset).plusDays(i % 30)
                    .withHour(15 + (i % 4)).withMinute(0);
            futureTickets.add(createSeedTicket(
                    template[0] + " " + (i + 1),
                    eventDate,
                    template[1],
                    1L,
                    3L, // 스포츠
                    new BigDecimal(template[2]),
                    new BigDecimal(template[3]),
                    template[4] + " " + (i % 20 + 1) + "번",
                    template[0] + " 티켓입니다.",
                    i % 2 == 0 ? TradeType.ONSITE : TradeType.DELIVERY
            ));
            ticketCount++;
            if (i % 10 == 9) monthOffset++;
        }

        // 전시 티켓 10개
        monthOffset = 1;
        for (int i = 0; i < 10 && ticketCount < 100; i++) {
            String[] template = exhibitionTemplates[i % exhibitionTemplates.length];
            LocalDateTime eventDate = now.plusMonths(monthOffset).plusDays(i % 30)
                    .withHour(10 + (i % 4)).withMinute(0);
            futureTickets.add(createSeedTicket(
                    template[0] + " " + (i + 1),
                    eventDate,
                    template[1],
                    1L,
                    4L, // 전시
                    new BigDecimal(template[2]),
                    new BigDecimal(template[3]),
                    template[4],
                    template[0] + " 티켓입니다.",
                    TradeType.DELIVERY
            ));
            ticketCount++;
            if (i % 5 == 4) monthOffset++;
        }

        // 클래식 티켓 10개
        monthOffset = 1;
        for (int i = 0; i < 10 && ticketCount < 100; i++) {
            String[] template = classicTemplates[i % classicTemplates.length];
            LocalDateTime eventDate = now.plusMonths(monthOffset).plusDays(i % 30)
                    .withHour(19 + (i % 2)).withMinute((i % 2) * 30);
            futureTickets.add(createSeedTicket(
                    template[0] + " " + (i + 1),
                    eventDate,
                    template[1],
                    1L,
                    5L, // 클래식
                    new BigDecimal(template[2]),
                    new BigDecimal(template[3]),
                    template[4],
                    template[0] + " 공연 티켓입니다.",
                    i % 2 == 0 ? TradeType.ONSITE : TradeType.DELIVERY
            ));
            ticketCount++;
            if (i % 5 == 4) monthOffset++;
        }

        ticketRepository.saveAll(futureTickets);
    }

    /**
     * 시드 티켓 생성 헬퍼 메서드
     */
    private Ticket createSeedTicket(String eventName, LocalDateTime eventDate, String eventLocation,
                                    Long ownerId, Long categoryId, BigDecimal originalPrice,
                                    BigDecimal sellingPrice, String seatInfo, String description,
                                    TradeType tradeType) {
        return Ticket.builder()
                .eventName(eventName)
                .eventDate(eventDate)
                .eventLocation(eventLocation)
                .ownerId(ownerId)
                .categoryId(categoryId)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(originalPrice)
                .sellingPrice(sellingPrice)
                .seatInfo(seatInfo)
                .ticketType("일반")
                .description(description)
                .tradeType(tradeType)
                .build();
    }

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
