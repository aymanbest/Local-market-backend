package com.localmarket.main.repository.support;

import com.localmarket.main.entity.support.TicketMessage;
import com.localmarket.main.entity.support.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicketOrderBySentAtAsc(Ticket ticket);
} 