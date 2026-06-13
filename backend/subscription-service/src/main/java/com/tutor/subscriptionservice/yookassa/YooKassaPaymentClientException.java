package com.tutor.subscriptionservice.yookassa;

public class YooKassaPaymentClientException extends RuntimeException {
    public YooKassaPaymentClientException(String message) {
        super(message);
    }

    public YooKassaPaymentClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
