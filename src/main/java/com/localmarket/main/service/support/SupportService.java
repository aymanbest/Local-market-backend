package com.localmarket.main.service.support;

import com.localmarket.main.entity.support.*;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.repository.support.TicketRepository;
import com.localmarket.main.repository.support.TicketMessageRepository;
import com.localmarket.main.dto.support.*;
import com.localmarket.main.dto.user.GetAllUsersResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.Map;
import com.localmarket.main.service.notification.WebSocketService;
import com.localmarket.main.dto.notification.NotificationResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final WebSocketService webSocketService;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, User producer) {
        if (producer == null) {
            log.error("Producer cannot be null when creating a ticket");
            throw new IllegalArgumentException("Producer cannot be null");
        }

        log.info("Creating ticket for producer: {} (ID: {})", producer.getUsername(), producer.getUserId());

        // Check if producer already has an active ticket
        boolean hasActiveTicket = ticketRepository.existsByCreatedByAndStatusNotIn(
            producer,
            Arrays.asList(TicketStatus.CLOSED)
        );

        if (hasActiveTicket) {
            log.warn("Producer {} already has an active ticket", producer.getUsername());
            throw new RuntimeException("You already have an active ticket. Please wait for it to be resolved before creating a new one.");
        }

        Ticket ticket = new Ticket();
        ticket.setSubject(request.getSubject());
        ticket.setCreatedBy(producer);
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        
        log.debug("Saving ticket with subject: {}", ticket.getSubject());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created with ID: {}", savedTicket.getTicketId());

        // Create initial message
        TicketMessage message = new TicketMessage();
        message.setTicket(savedTicket);
        message.setSender(producer);
        message.setContent(request.getMessage());
        message.setInternalNote(false);
        messageRepository.save(message);
        log.debug("Initial message saved for ticket ID: {}", savedTicket.getTicketId());

        return mapToDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assignTicket(Long ticketId, User admin) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
            
        ticket.setAssignedTo(admin);
        ticket.setStatus(TicketStatus.ASSIGNED);
        return mapToDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse forwardTicket(Long ticketId, User newAdmin) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
            
        ticket.setAssignedTo(newAdmin);
        return mapToDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public void addMessage(Long ticketId, TicketMessageRequest request, User sender) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Only allow admins to set internal notes, force false for producers
        boolean isInternalNote = sender.getRole() == Role.ADMIN ? request.isInternalNote() : false;

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(request.getMessage());
        message.setInternalNote(isInternalNote);
        messageRepository.save(message);

        // Send notification if admin replies and it's not an internal note
        if (sender.getRole() == Role.ADMIN && !isInternalNote) {
            NotificationResponse notification = NotificationResponse.builder()
                .type("TICKET_REPLY")
                .message("Admin replied to your ticket: " + ticket.getSubject())
                .data(Map.of(
                    "ticketId", ticket.getTicketId(),
                    "subject", ticket.getSubject(),
                    "message", request.getMessage(),
                    "adminName", sender.getUsername()
                ))
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

            webSocketService.sendToUser(ticket.getCreatedBy().getEmail(), notification);
        }

        // Update ticket status if needed
        if (ticket.getStatus() == TicketStatus.PENDING_PRODUCER && !sender.getRole().name().equals("ADMIN")) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getUnassignedTickets(Pageable pageable) {
        Page<Ticket> ticketPage = ticketRepository.findByAssignedToIsNull(pageable);
        return ticketPage.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getAdminTickets(User admin, TicketStatus status, Pageable pageable) {
        Page<Ticket> ticketPage = status != null ?
            ticketRepository.findByAssignedToAndStatus(admin, status, pageable) :
            ticketRepository.findByAssignedTo(admin, pageable);
        
        return ticketPage.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getProducerTickets(User producer, TicketStatus status, Pageable pageable) {
        Page<Ticket> ticketPage = status != null ?
            ticketRepository.findByCreatedByAndStatus(producer, status, pageable) :
            ticketRepository.findByCreatedBy(producer, pageable);
        
        return ticketPage.map(this::mapToDTO);
    }

    public List<TicketMessageResponse> getTicketMessages(Long ticketId, User user) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Verify that the user has access to this ticket
        if (user.getRole() == Role.PRODUCER && !ticket.getCreatedBy().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("You don't have access to this ticket");
        }

        List<TicketMessage> messages = messageRepository.findByTicketOrderBySentAtAsc(ticket);
        
        // Filter out internal notes for producers
        if (user.getRole() == Role.PRODUCER) {
            messages = messages.stream()
                .filter(message -> !message.isInternalNote())
                .collect(Collectors.toList());
        }

        return messages.stream()
            .map(this::mapToMessageDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public TicketResponse closeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(java.time.LocalDateTime.now());
        return mapToDTO(ticketRepository.save(ticket));
    }

    private TicketResponse mapToDTO(Ticket ticket) {
        return new TicketResponse(
            ticket.getTicketId(),
            ticket.getSubject(),
            ticket.getStatus(),
            ticket.getPriority(),
            new GetAllUsersResponse(
                ticket.getCreatedBy().getUserId(),
                ticket.getCreatedBy().getUsername(),
                ticket.getCreatedBy().getEmail(),
                ticket.getCreatedBy().getFirstname(),
                ticket.getCreatedBy().getLastname(),
                ticket.getCreatedBy().getRole(),
                ticket.getCreatedBy().getCreatedAt(),
                ticket.getCreatedBy().getLastLogin()
            ),
            ticket.getAssignedTo() != null ? new GetAllUsersResponse(
                ticket.getAssignedTo().getUserId(),
                ticket.getAssignedTo().getUsername(),
                ticket.getAssignedTo().getEmail(),
                ticket.getAssignedTo().getFirstname(),
                ticket.getAssignedTo().getLastname(),
                ticket.getAssignedTo().getRole(),
                ticket.getAssignedTo().getCreatedAt(),
                ticket.getAssignedTo().getLastLogin()
            ) : null,
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getClosedAt()
        );
    }

    private TicketMessageResponse mapToMessageDTO(TicketMessage message) {
        return new TicketMessageResponse(
            message.getMessageId(),
            mapToDTO(message.getTicket()),
            new GetAllUsersResponse(
                message.getSender().getUserId(),
                message.getSender().getUsername(),
                message.getSender().getEmail(),
                message.getSender().getFirstname(),
                message.getSender().getLastname(),
                message.getSender().getRole(),
                message.getSender().getCreatedAt(),
                message.getSender().getLastLogin()
            ),
            message.getContent(),
            message.getSentAt(),
            message.isInternalNote()
        );
    }
} 