package com.company.ticketservice.repository;

import com.company.ticketservice.entity.Ticket;
import com.company.ticketservice.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Ticket t
       SET t.ticketStatus = :expired
     WHERE t.ticketStatus = :available
       AND t.eventDate < :now
""")
    int expireAvailableTickets(
            @Param("available") TicketStatus available,
            @Param("expired") TicketStatus expired,
            @Param("now") LocalDateTime now
    );

}