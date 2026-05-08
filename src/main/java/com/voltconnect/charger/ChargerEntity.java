package com.voltconnect.charger;

import com.voltconnect.shared.utils.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code chargers} table in Supabase PostgreSQL.
 *
 * <p>Array columns ({@code connector_type}, {@code photos}) are handled by
 * {@link StringListConverter}. The {@code host_id} column is a UUID foreign key
 * referencing the {@code users} table.
 *
 * <p>Satisfies Requirements 3.2, 4.1, 4.2, 4.3, 5.1, 13.1, 13.2, 13.3, 13.4, 13.5.
 */
@Entity
@Table(name = "chargers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** FK → users.id */
    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String address;

    /**
     * Connector types supported by this charger (e.g. ["Type2","CCS2"]).
     * Stored as a PostgreSQL {@code text[]} column.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "connector_type", columnDefinition = "text[]")
    private List<String> connectorType;

    @Column(name = "power_kw", precision = 5, scale = 2)
    private BigDecimal powerKw;

    @Column(name = "price_per_hour", precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Builder.Default
    @Column(name = "is_available")
    private boolean isAvailable = true;

    @Builder.Default
    @Column(name = "is_verified")
    private boolean isVerified = false;

    /**
     * Lifecycle status: {@code pending_verification}, {@code active},
     * {@code inactive}, or {@code suspended}.
     */
    @Builder.Default
    @Column(name = "status")
    private String status = "pending_verification";

    @Column(name = "electricity_bill_url")
    private String electricityBillUrl;

    @Column(name = "bis_certificate_url")
    private String bisCertificateUrl;

    /**
     * Up to 5 Supabase Storage photo URLs.
     * Stored as a PostgreSQL {@code text[]} column.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "photos", columnDefinition = "text[]")
    private List<String> photos;

    @Builder.Default
    @Column(name = "total_sessions")
    private int totalSessions = 0;

    @Builder.Default
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "created_at")
    private Instant createdAt;
}
