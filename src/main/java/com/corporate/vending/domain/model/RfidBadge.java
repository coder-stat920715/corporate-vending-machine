package com.corporate.vending.domain.model;

import com.corporate.vending.domain.BadgeStatus;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rfid_badges")
@Getter
@Setter
@NoArgsConstructor
public class RfidBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String badgeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BadgeStatus status = BadgeStatus.ACTIVE;

    @Column(nullable = false)
    private long offlineCapPaise = 20000;
}
