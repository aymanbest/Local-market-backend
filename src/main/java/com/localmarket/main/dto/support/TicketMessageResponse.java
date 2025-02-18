package com.localmarket.main.dto.support;

import com.localmarket.main.dto.user.GetAllUsersResponse;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketMessageResponse {
    private Long messageId;
    private TicketResponse ticket;
    private GetAllUsersResponse sender;
    private String content;
    private LocalDateTime sentAt;
    private boolean internalNote;
}