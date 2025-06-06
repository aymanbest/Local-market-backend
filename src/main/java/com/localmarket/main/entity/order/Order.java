package com.localmarket.main.entity.order;

import lombok.Data;
import lombok.ToString;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import com.localmarket.main.entity.payment.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "`Order`")
@Data
@EqualsAndHashCode(exclude = {"customer", "items"})
@ToString(exclude = {"customer", "items"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    @JsonBackReference
    private User customer;
    
    @Column(name = "guestEmail")
    private String guestEmail;
    
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    @Column(name = "shippingAddress")
    private String shippingAddress;
    
    @NotBlank(message = "Phone number is required")
    @Column(name = "phoneNumber")
    private String phoneNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "orderDate")
    private LocalDateTime orderDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;
    

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Total price must have at most 10 digits and 2 decimal places")
    private BigDecimal totalPrice;
    
    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();
    
    @OneToOne
    @JoinColumn(name = "paymentId")
    private Payment payment;
 

    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "paymentMethod")
    private PaymentMethod paymentMethod;
    

    @Column(name = "accessToken")
    private String accessToken;
    
    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
} 