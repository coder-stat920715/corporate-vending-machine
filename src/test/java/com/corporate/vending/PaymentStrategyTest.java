package com.corporate.vending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corporate.vending.application.payment.CardPaymentStrategy;
import com.corporate.vending.application.payment.PaymentRequest;
import com.corporate.vending.application.payment.PaymentResult;
import com.corporate.vending.application.payment.RfidPaymentStrategy;
import com.corporate.vending.application.payment.UpiPaymentStrategy;
import com.corporate.vending.application.port.out.CardGatewayPort;
import com.corporate.vending.application.port.out.UpiGatewayPort;
import com.corporate.vending.application.port.out.WalletPort;
import com.corporate.vending.domain.PaymentMethod;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Disabled
class PaymentStrategyTest {

    private static final UUID ORDER = UUID.randomUUID();

    @Mock WalletPort walletPort;
    @Mock UpiGatewayPort upiGateway;
    @Mock CardGatewayPort cardGateway;

    @InjectMocks RfidPaymentStrategy rfid;
    @InjectMocks UpiPaymentStrategy upi;
    @InjectMocks CardPaymentStrategy card;

    private PaymentRequest request(PaymentMethod method, String badge, String cardToken, String txnId) {
        return new PaymentRequest(ORDER, method, 6000, badge, cardToken, null, txnId);
    }

    @Test
    void rfid_success_returnsTransactionId() {
        when(walletPort.debit("hash", 6000, ORDER)).thenReturn(Optional.of("tx-1"));

        PaymentResult result = rfid.pay(request(PaymentMethod.RFID, "hash", null, null));

        assertThat(result.status()).isEqualTo(PaymentResult.Status.SUCCESS);
        assertThat(result.gatewayTxnId()).isEqualTo("tx-1");
    }

    @Test
    void rfid_insufficientFunds_fails() {
        when(walletPort.debit(anyString(), anyLong(), eq(ORDER))).thenReturn(Optional.empty());

        PaymentResult result = rfid.pay(request(PaymentMethod.RFID, "hash", null, null));

        assertThat(result.status()).isEqualTo(PaymentResult.Status.FAILED);
    }

    @Test
    void rfid_missingBadge_failsWithoutTouchingWallet() {
        PaymentResult result = rfid.pay(request(PaymentMethod.RFID, " ", null, null));

        assertThat(result.status()).isEqualTo(PaymentResult.Status.FAILED);
        assertThat(result.message()).contains("Badge");
    }

    @Test
    void rfid_refund_creditsWallet() {
        rfid.refund(request(PaymentMethod.RFID, "hash", null, "tx-1"));

        verify(walletPort).credit("hash", 6000, ORDER);
    }

    @Test
    void upi_pay_returnsPendingWithQr() {
        when(upiGateway.createDynamicQr(ORDER, 6000)).thenReturn(new UpiGatewayPort.QrResponse("upi-1", "upi://pay?x=1"));

        PaymentResult result = upi.pay(request(PaymentMethod.UPI, null, null, null));

        assertThat(result.status()).isEqualTo(PaymentResult.Status.PENDING);
        assertThat(result.qrPayload()).isEqualTo("upi://pay?x=1");
        assertThat(result.gatewayTxnId()).isEqualTo("upi-1");
    }

    @Test
    void upi_gatewayDown_fails() {
        when(upiGateway.createDynamicQr(ORDER, 6000)).thenThrow(new IllegalStateException("down"));

        PaymentResult result = upi.pay(request(PaymentMethod.UPI, null, null, null));

        assertThat(result.status()).isEqualTo(PaymentResult.Status.FAILED);
    }

    @Test
    void upi_refund_usesGatewayTransactionId() {
        upi.refund(request(PaymentMethod.UPI, null, null, "upi-1"));

        verify(upiGateway).refund("upi-1", 6000);
    }

    @Test
    void card_approved_returnsSuccess() {
        when(cardGateway.charge("tok", 6000, ORDER)).thenReturn(Optional.of("card-1"));

        PaymentResult result = card.pay(request(PaymentMethod.CARD, null, "tok", null));

        assertThat(result.status()).isEqualTo(PaymentResult.Status.SUCCESS);
        assertThat(result.gatewayTxnId()).isEqualTo("card-1");
    }

    @Test
    void card_declined_fails() {
        when(cardGateway.charge("tok", 6000, ORDER)).thenReturn(Optional.empty());

        assertThat(card.pay(request(PaymentMethod.CARD, null, "tok", null)).status())
                .isEqualTo(PaymentResult.Status.FAILED);
    }

    @Test
    void card_missingToken_fails() {
        assertThat(card.pay(request(PaymentMethod.CARD, null, null, null)).status())
                .isEqualTo(PaymentResult.Status.FAILED);
    }
}
