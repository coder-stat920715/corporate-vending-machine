package com.corporate.vending.domain.model;

import com.corporate.vending.domain.MachineStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "machines")
@Getter
@Setter
@NoArgsConstructor
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MachineStatus status = MachineStatus.ONLINE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
