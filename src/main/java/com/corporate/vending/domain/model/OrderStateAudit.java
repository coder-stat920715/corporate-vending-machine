package com.corporate.vending.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "order_state_audit")
@Getter
@Setter
@NoArgsConstructor
public class OrderStateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(length = 20)
    private String fromState;

    @Column(nullable = false, length = 20)
    private String toState;

    @Column(nullable = false, length = 30)
    private String event;

    @CreationTimestamp
    @Column(name = "at", nullable = false, updatable = false)
    private Instant occurredAt;

    public OrderStateAudit(UUID orderId, String fromState, String toState, String event) {
        this.orderId = orderId;
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
    }
}
