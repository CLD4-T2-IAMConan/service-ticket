package com.company.ticketservice.service;

import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service // 이 클래스가 Service 레이어임을 스프링에 알림
@RequiredArgsConstructor // final 필드 자동 생성자 주입 (DI)
@Transactional(readOnly = true) // 기본은 읽기-only 트랜잭션
public class TicketService {

    private final TicketRepository ticketRepository;

    /**
     * 티켓 생성 (등록)
     * - 트랜잭션 안에서 DB에 INSERT 수행
     */
    @Transactional // 쓰기 작업이므로 readOnly 끔
    public Ticket createTicket(Ticket ticket) {
        // 여기서 나중에 검증 로직(가격 0원 이상인지 등)도 추가할 수 있음
        return ticketRepository.save(ticket);
    }

    /**
     * 티켓 단건 조회
     * - id로 티켓을 찾고, 없으면 예외 발생
     */
    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found. id=" + ticketId));
    }

    /**
     * 전체 티켓 목록 조회
     */
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    /**
     * 티켓 삭제
     */
    @Transactional
    public void deleteTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new IllegalArgumentException("Ticket not found. id=" + ticketId);
        }
        ticketRepository.deleteById(ticketId);
    }
}
