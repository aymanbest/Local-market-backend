package com.localmarket.main.repository.support;

import com.localmarket.main.entity.support.Ticket;
import com.localmarket.main.entity.support.TicketStatus;
import com.localmarket.main.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCreatedByOrderByCreatedAtDesc(User user);
    List<Ticket> findByAssignedToOrderByCreatedAtDesc(User admin);
    List<Ticket> findByAssignedToIsNullOrderByCreatedAtDesc();
    
    boolean existsByCreatedByAndStatusNotIn(User user, List<TicketStatus> statuses);

    List<Ticket> findByCreatedByAndStatus(User user, TicketStatus status);
    List<Ticket> findByAssignedToAndStatus(User admin, TicketStatus status);
} 