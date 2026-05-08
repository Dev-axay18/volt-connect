package com.voltconnect.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after successfully creating a booking.
 * Contains the Razorpay order details needed by the client to initiate payment.
 *
 * <p>Satisfies Requirements 6.2, 7.1.
 */
public record CreateBookingResponse(

    UUID bookingId,

    String razorpayOrderId,

    /** Razorpay publishable key — sent to client for SDK initialisation. */
    String razorpayKeyId,

    BigDecimal totalAmount,

    BigDecimal platformFee,

    /** The booking expires (moves to 'expired') at this instant if payment is not confirmed. */
    Instant expiresAt
) {}
