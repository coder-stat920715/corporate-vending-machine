package com.corporate.vending.domain.model;

import com.corporate.vending.domain.WalletTxType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WalletTxType type;

    @Column(nullable = false)
    private long amountPaise;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
