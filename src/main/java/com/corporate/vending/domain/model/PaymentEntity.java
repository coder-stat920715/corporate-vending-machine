package com.corporate.vending.domain.model;

import com.corporate.vending.domain.PaymentMethod;
import com.corporate.vending.domain.PaymentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PaymentStatus status;

    @Column(length = 64, unique = true)
    private String gatewayTxnId;

    @Column(nullable = false)
    private long amountPaise;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
