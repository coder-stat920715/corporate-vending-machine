package com.corporate.vending.domain.model;

import com.corporate.vending.domain.SlotStatus;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "slots", uniqueConstraints = @UniqueConstraint(columnNames = {"machine_id", "slot_no"}))
@Getter
@Setter
@NoArgsConstructor
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false, length = 6)
    private String slotNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SlotStatus status = SlotStatus.ACTIVE;

    @Column(nullable = false)
    private int faultCount;
}
