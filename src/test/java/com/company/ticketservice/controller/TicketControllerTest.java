package com.company.ticketservice.controller;

import com.company.ticketservice.config.TestConfig;
import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import com.company.ticketservice.entity.TradeType;
import com.company.ticketservice.repository.TicketRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("TicketController 통합 테스트")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Long testUserId;
    private Ticket testTicket;

    /**
     * 테스트용 JWT 토큰 생성
     */
    private String createTestToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1시간 후

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", "USER")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @BeforeEach
    void setUp() {
        // 테스트 사용자 ID
        testUserId = 1L;
        
        // 테스트 티켓 생성
        testTicket = Ticket.builder()
                .eventName("테스트 콘서트")
                .eventDate(LocalDateTime.now().plusDays(30))
                .eventLocation("올림픽공원")
                .ownerId(testUserId)
                .ticketStatus(TicketStatus.AVAILABLE)
                .originalPrice(new BigDecimal("100000"))
                .sellingPrice(new BigDecimal("95000"))
                .seatInfo("VIP석 1번")
                .ticketType("일반")
                .categoryId(1L)
                .description("테스트 티켓입니다")
                .tradeType(TradeType.DELIVERY)
                .build();
        
        ticketRepository.save(testTicket);
    }

    @Test
    @DisplayName("티켓 리스트 조회 성공 (인증 불필요)")
    void getTickets_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/tickets")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("티켓 상세 조회 성공 (인증 불필요)")
    void getTicketDetail_Success() throws Exception {
        // given
        Long ticketId = testTicket.getTicketId();

        // when & then
        mockMvc.perform(get("/api/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticketId").value(ticketId))
                .andExpect(jsonPath("$.data.eventName").value("테스트 콘서트"));
    }

    @Test
    @DisplayName("티켓 상세 조회 실패 - 존재하지 않는 티켓")
    void getTicketDetail_Fail_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/tickets/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("티켓 검색 - 이벤트명 필터링")
    void getTickets_WithEventNameFilter() throws Exception {
        // when & then
        mockMvc.perform(get("/api/tickets")
                        .param("eventName", "테스트")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].eventName").value("테스트 콘서트"));
    }

    @Test
    @DisplayName("티켓 검색 - 상태 필터링")
    void getTickets_WithStatusFilter() throws Exception {
        // when & then
        mockMvc.perform(get("/api/tickets")
                        .param("ticketStatus", "AVAILABLE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("티켓 검색 - 정렬")
    void getTickets_WithSorting() throws Exception {
        // when & then
        mockMvc.perform(get("/api/tickets")
                        .param("sortBy", "eventDate")
                        .param("sortDirection", "ASC")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("판매자 본인 티켓 조회 성공 (인증 필요)")
    void getMyTickets_Success() throws Exception {
        // given
        String token = createTestToken(testUserId);

        // when & then
        mockMvc.perform(get("/api/sellers/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("판매자 본인 티켓 조회 실패 - 인증 없음")
    void getMyTickets_Fail_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/sellers/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("티켓 상태 변경 성공 (인증 필요)")
    void updateTicketStatus_Success() throws Exception {
        // given
        String token = createTestToken(testUserId);
        Long ticketId = testTicket.getTicketId();

        // when & then
        mockMvc.perform(put("/api/tickets/{ticketId}/status/{newStatus}", ticketId, "RESERVED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("티켓 상태 변경 실패 - 인증 없음")
    void updateTicketStatus_Fail_Unauthorized() throws Exception {
        // given
        Long ticketId = testTicket.getTicketId();

        // when & then
        mockMvc.perform(put("/api/tickets/{ticketId}/status/{newStatus}", ticketId, "RESERVED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("시드 데이터 생성 성공 (인증 불필요)")
    void seedTickets_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/admin/tickets/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

