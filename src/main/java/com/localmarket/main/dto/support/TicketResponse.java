package com.localmarket.main.dto.support;

import com.localmarket.main.entity.support.TicketStatus;
import com.localmarket.main.entity.support.TicketPriority;
import lombok.Data;
import java.time.LocalDateTime;
import com.localmarket.main.dto.user.GetAllUsersResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private Long ticketId;
    private String subject;
    private TicketStatus status;
    private TicketPriority priority;
    private GetAllUsersResponse createdBy;
    private GetAllUsersResponse assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
}