package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.application.port.out.WalletPort;
import com.corporate.vending.domain.WalletTxType;
import com.corporate.vending.domain.model.Wallet;
import com.corporate.vending.domain.model.WalletTransaction;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WalletAdapter implements WalletPort {

    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;

    @Override
    @Transactional
    public Optional<String> debit(String badgeHash, long amountPaise, UUID orderId) {
        Optional<WalletTransaction> already = transactions.findByOrderIdAndType(orderId, WalletTxType.DEBIT);
        if (already.isPresent()) {
            return already.map(t -> t.getId().toString());
        }
        return wallets.lockByActiveBadge(badgeHash)
                .filter(w -> w.getBalancePaise() + w.getCreditLimitPaise() >= amountPaise)
                .map(w -> {
                    w.setBalancePaise(w.getBalancePaise() - amountPaise);
                    wallets.save(w);
                    return transactions.save(newTx(w, orderId, WalletTxType.DEBIT, amountPaise)).getId().toString();
                });
    }

    @Override
    @Transactional
    public void credit(String badgeHash, long amountPaise, UUID orderId) {
        if (badgeHash == null || transactions.findByOrderIdAndType(orderId, WalletTxType.REFUND).isPresent()) {
            return;
        }
        wallets.lockByBadge(badgeHash).ifPresent(w -> {
            w.setBalancePaise(w.getBalancePaise() + amountPaise);
            wallets.save(w);
            transactions.save(newTx(w, orderId, WalletTxType.REFUND, amountPaise));
        });
    }

    private WalletTransaction newTx(Wallet wallet, UUID orderId, WalletTxType type, long amount) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setOrderId(orderId);
        tx.setType(type);
        tx.setAmountPaise(amount);
        return tx;
    }
}
