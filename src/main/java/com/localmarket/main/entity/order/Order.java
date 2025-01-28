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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import com.localmarket.main.entity.payment.PaymentMethod;

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
    
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    
    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Total price must have at most 10 digits and 2 decimal places")
    private BigDecimal totalPrice;
    
    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    @JsonManagedReference
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @OneToOne
    @JoinColumn(name = "paymentId")
    private Payment payment;
    
    @Column(name = "guest_token")
    private String guestToken;
    
    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
} 