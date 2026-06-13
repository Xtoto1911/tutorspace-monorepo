package com.tutor.subscriptionservice.yookassa;

public interface YooKassaPaymentClient {
    YooKassaPayment createPayment(YooKassaCreatePaymentRequest request, String idempotenceKey);

    YooKassaPayment getPayment(String providerPaymentId);
}
