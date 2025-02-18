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

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;

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

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(request.getMessage());
        message.setInternalNote(request.isInternalNote());
        messageRepository.save(message);

        // Update ticket status if needed
        if (ticket.getStatus() == TicketStatus.PENDING_PRODUCER && !sender.getRole().name().equals("ADMIN")) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getUnassignedTickets(Pageable pageable) {
        List<Ticket> tickets = ticketRepository.findByAssignedToIsNullOrderByCreatedAtDesc();
        
        // Sort tickets
        List<Ticket> sortedTickets = tickets.stream()
            .sorted((t1, t2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "createdAt" -> t1.getCreatedAt().compareTo(t2.getCreatedAt());
                    case "priority" -> t1.getPriority().compareTo(t2.getPriority());
                    case "status" -> t1.getStatus().compareTo(t2.getStatus());
                    case "subject" -> t1.getSubject().compareTo(t2.getSubject());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedTickets.size());
        
        if (start >= sortedTickets.size()) {
            return new PageImpl<>(List.of(), pageable, sortedTickets.size());
        }
        
        List<Ticket> paginatedTickets = sortedTickets.subList(start, end);
        
        return new PageImpl<>(
            paginatedTickets.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()),
            pageable,
            sortedTickets.size()
        );
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getAdminTickets(User admin, TicketStatus status, Pageable pageable) {
        List<Ticket> tickets = status != null ? 
            ticketRepository.findByAssignedToAndStatus(admin, status) :
            ticketRepository.findByAssignedToOrderByCreatedAtDesc(admin);
        
        // Sort tickets
        List<Ticket> sortedTickets = tickets.stream()
            .sorted((t1, t2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "createdAt" -> t1.getCreatedAt().compareTo(t2.getCreatedAt());
                    case "priority" -> t1.getPriority().compareTo(t2.getPriority());
                    case "status" -> t1.getStatus().compareTo(t2.getStatus());
                    case "subject" -> t1.getSubject().compareTo(t2.getSubject());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedTickets.size());
        
        if (start >= sortedTickets.size()) {
            return new PageImpl<>(List.of(), pageable, sortedTickets.size());
        }
        
        List<Ticket> paginatedTickets = sortedTickets.subList(start, end);
        
        return new PageImpl<>(
            paginatedTickets.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()),
            pageable,
            sortedTickets.size()
        );
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getProducerTickets(User producer, TicketStatus status, Pageable pageable) {
        List<Ticket> tickets = status != null ?
            ticketRepository.findByCreatedByAndStatus(producer, status) :
            ticketRepository.findByCreatedByOrderByCreatedAtDesc(producer);
        
        // Sort tickets
        List<Ticket> sortedTickets = tickets.stream()
            .sorted((t1, t2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "createdAt" -> t1.getCreatedAt().compareTo(t2.getCreatedAt());
                    case "priority" -> t1.getPriority().compareTo(t2.getPriority());
                    case "status" -> t1.getStatus().compareTo(t2.getStatus());
                    case "subject" -> t1.getSubject().compareTo(t2.getSubject());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedTickets.size());
        
        if (start >= sortedTickets.size()) {
            return new PageImpl<>(List.of(), pageable, sortedTickets.size());
        }
        
        List<Ticket> paginatedTickets = sortedTickets.subList(start, end);
        
        return new PageImpl<>(
            paginatedTickets.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()),
            pageable,
            sortedTickets.size()
        );
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