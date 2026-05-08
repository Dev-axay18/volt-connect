package com.voltconnect.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for confirming a Razorpay payment after the client completes checkout.
 *
 * <p>Satisfies Requirements 7.2, 7.3.
 */
public record ConfirmPaymentRequest(

    @NotNull(message = "bookingId is required")
    UUID bookingId,

    @NotBlank(message = "razorpayOrderId is required")
    String razorpayOrderId,

    @NotBlank(message = "razorpayPaymentId is required")
    String razorpayPaymentId,

    @NotBlank(message = "razorpaySignature is required")
    String razorpaySignature
) {}
