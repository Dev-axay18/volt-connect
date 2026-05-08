package com.voltconnect.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BookingEntity}.
 *
 * <p>Satisfies Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 8.1, 8.3, 8.4, 8.5, 25.1.
 */
public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    /**
     * Returns all bookings for a driver, ordered by start time descending.
     */
    List<BookingEntity> findByDriverIdOrderByStartTimeDesc(UUID driverId);

    /**
     * Returns bookings that overlap with the given time window for a charger,
     * considering only active/confirmed/pending_host_approval statuses.
     */
    @Query("SELECT b FROM BookingEntity b WHERE b.chargerId = :chargerId " +
           "AND b.status IN ('confirmed', 'active', 'pending_host_approval') " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<BookingEntity> findConflictingBookings(
        @Param("chargerId") UUID chargerId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Returns all bookings for chargers owned by the given host, ordered by start time descending.
     * Uses a JPQL join between BookingEntity and ChargerEntity.
     */
    @Query("SELECT b FROM BookingEntity b " +
           "JOIN com.voltconnect.charger.ChargerEntity c ON b.chargerId = c.id " +
           "WHERE c.hostId = :hostId " +
           "ORDER BY b.startTime DESC")
    List<BookingEntity> findByHostId(@Param("hostId") UUID hostId);

    /**
     * Returns IDs of active/confirmed bookings for a charger that haven't ended yet.
     * Used by the charger deletion conflict check.
     */
    @Query("SELECT b.id FROM BookingEntity b WHERE b.chargerId = :chargerId " +
           "AND b.status IN ('confirmed', 'active') " +
           "AND b.endTime > :now")
    List<UUID> findActiveBookingIdsForCharger(
        @Param("chargerId") UUID chargerId,
        @Param("now") Instant now
    );

    /**
     * Bulk-expires all pending_payment bookings whose payment window has elapsed.
     *
     * @param cutoff bookings created before this instant will be expired
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE BookingEntity b SET b.status = 'expired' " +
           "WHERE b.status = 'pending_payment' " +
           "AND b.createdAt < :cutoff")
    int expireStaleBookings(@Param("cutoff") Instant cutoff);
}
