package com.corporate.vending.application.payment;

public record PaymentResult(Status status, String gatewayTxnId, String qrPayload, String message) {

    public enum Status { SUCCESS, PENDING, FAILED }

    public static PaymentResult success(String txnId) {
        return new PaymentResult(Status.SUCCESS, txnId, null, "OK");
    }

    public static PaymentResult pending(String txnId, String qrPayload) {
        return new PaymentResult(Status.PENDING, txnId, qrPayload, "Awaiting payment");
    }

    public static PaymentResult failed(String message) {
        return new PaymentResult(Status.FAILED, null, null, message);
    }
}
