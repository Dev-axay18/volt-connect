package com.voltconnect.charger;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code availability_slots} table.
 *
 * <p>Each slot defines a recurring weekly window during which a charger is
 * available for booking. {@code dayOfWeek} follows the Java convention:
 * 0 = Sunday … 6 = Saturday.
 *
 * <p>Satisfies Requirements 11.1, 11.2, 11.3.
 */
@Entity
@Table(name = "availability_slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** FK → chargers.id */
    @Column(name = "charger_id", nullable = false)
    private UUID chargerId;

    /**
     * Day of week: 0 = Sunday, 1 = Monday, …, 6 = Saturday.
     */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;
}
