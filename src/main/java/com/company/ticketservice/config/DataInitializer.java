package com.company.ticketservice.config;

import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
import com.company.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TicketRepository ticketRepository;

    @Override
    public void run(String... args) {
        long existingCount = ticketRepository.count();
        log.info("Existing ticket count: {}", existingCount);
        
        // 미래 날짜의 AVAILABLE 티켓 개수 확인
        LocalDateTime now = LocalDateTime.now();
        long availableFutureTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.AVAILABLE)
                .filter(t -> t.getEventDate() != null && t.getEventDate().isAfter(now))
                .count();
        
        log.info("Available future tickets: {} (target: 100)", availableFutureTickets);
        
        // 미래 날짜의 AVAILABLE 티켓이 100개 미만이면 추가 생성
        if (availableFutureTickets < 100) {
            log.info("🚀 Initializing additional ticket seed data (future dates only)... Current: {}, Target: 100", availableFutureTickets);
            try {
                initializeFutureTickets();
                long newCount = ticketRepository.findAll().stream()
                        .filter(t -> t.getTicketStatus() == TicketStatus.AVAILABLE)
                        .filter(t -> t.getEventDate() != null && t.getEventDate().isAfter(now))
                        .count();
                log.info("✅ Ticket seed data initialized successfully. Available future tickets: {} (added: {})", newCount, newCount - availableFutureTickets);
            } catch (Exception e) {
                log.error("❌ Failed to initialize ticket seed data", e);
                e.printStackTrace();
            }
        } else {
            log.info("✅ Sufficient available future tickets exist ({} tickets), skipping initialization", availableFutureTickets);
        }
    }

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
            futureTickets.add(createTicket(
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
            futureTickets.add(createTicket(
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
            futureTickets.add(createTicket(
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
            futureTickets.add(createTicket(
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
            futureTickets.add(createTicket(
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
        log.info("Generated {} future tickets", futureTickets.size());
    }

    private Ticket createTicket(String eventName, LocalDateTime eventDate, String eventLocation,
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
}
