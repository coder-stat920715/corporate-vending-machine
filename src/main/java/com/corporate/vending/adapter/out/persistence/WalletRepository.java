package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.model.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.employee.id = "
            + "(select b.employee.id from RfidBadge b where b.badgeHash = :hash "
            + "and b.status = com.corporate.vending.domain.BadgeStatus.ACTIVE)")
    Optional<Wallet> lockByActiveBadge(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.employee.id = "
            + "(select b.employee.id from RfidBadge b where b.badgeHash = :hash)")
    Optional<Wallet> lockByBadge(@Param("hash") String hash);
}
