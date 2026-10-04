package com.corporate.vending.application.port.out;

import java.util.UUID;

public interface UpiGatewayPort {

    record QrResponse(String txnId, String qrPayload) { }

    QrResponse createDynamicQr(UUID orderId, long amountPaise);

    void refund(String txnId, long amountPaise);
}
