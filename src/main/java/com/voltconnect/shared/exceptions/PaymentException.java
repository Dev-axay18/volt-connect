package com.voltconnect.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a payment operation fails (e.g., Razorpay order creation failure,
 * HMAC signature mismatch, or refund processing error).
 * Maps to HTTP 402 Payment Required (or 400 Bad Request depending on context).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
