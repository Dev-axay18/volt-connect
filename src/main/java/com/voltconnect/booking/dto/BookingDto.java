package com.voltconnect.booking.dto;

import com.voltconnect.booking.BookingEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Full representation of a booking, returned by most booking endpoints.
 *
 * <p>Satisfies Requirements 6.3, 6.4, 6.5, 7.4, 7.5, 8.1, 8.3, 8.4, 8.5.
 */
public record BookingDto(

    UUID id,
    UUID driverId,
    UUID chargerId,
    Instant startTime,
    Instant endTime,
    BigDecimal durationHours,
    BigDecimal totalAmount,
    BigDecimal platformFee,
    BigDecimal hostPayout,
    String status,
    String razorpayOrderId,
    String razorpayPaymentId,
    String razorpaySignature,
    String paymentStatus,
    Instant createdAt
) {

    /**
     * Factory method to convert a {@link BookingEntity} to a {@link BookingDto}.
     */
    public static BookingDto from(BookingEntity entity) {
        return new BookingDto(
            entity.getId(),
            entity.getDriverId(),
            entity.getChargerId(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getDurationHours(),
            entity.getTotalAmount(),
            entity.getPlatformFee(),
            entity.getHostPayout(),
            entity.getStatus(),
            entity.getRazorpayOrderId(),
            entity.getRazorpayPaymentId(),
            entity.getRazorpaySignature(),
            entity.getPaymentStatus(),
            entity.getCreatedAt()
        );
    }
}
