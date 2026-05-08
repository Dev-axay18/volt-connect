package com.voltconnect.booking;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code bookings} table in Supabase PostgreSQL.
 *
 * <p>Tracks the full lifecycle of a booking from {@code pending_payment} through
 * {@code confirmed}, {@code active}, {@code completed}, {@code cancelled},
 * {@code cancelled_refunded}, and {@code expired}.
 *
 * <p>Satisfies Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5.
 */
@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** FK → users.id (driver) */
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    /** FK → chargers.id */
    @Column(name = "charger_id", nullable = false)
    private UUID chargerId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_hours", precision = 5, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "host_payout", precision = 10, scale = 2)
    private BigDecimal hostPayout;

    /**
     * Booking lifecycle status:
     * pending_payment → confirmed → active → completed
     * pending_payment → expired (10-min timer)
     * confirmed / active → cancelled / cancelled_refunded
     */
    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "pending_payment";

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    /**
     * Payment status: unpaid, paid, refunded.
     */
    @Builder.Default
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "unpaid";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
