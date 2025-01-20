package com.localmarket.main.entity.order;

import lombok.Data;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.payment.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "`Order`")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    @JsonIgnoreProperties({"passwordHash", "role", "createdAt", "updatedAt"})
    private User customer;
    
    private String guestEmail;
    private String shippingAddress;
    private String phoneNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    
    private BigDecimal totalPrice;
    
    @JsonManagedReference
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @OneToOne
    @JoinColumn(name = "paymentId")
    private Payment payment;
    
    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
    }
} 