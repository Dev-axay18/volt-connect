package com.voltconnect.auth;

import com.voltconnect.shared.utils.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * JPA entity mapping to the {@code users} table in Supabase PostgreSQL.
 *
 * <p>The {@code id} column stores the Firebase UID (a UUID string).
 * PostgreSQL array columns ({@code role}, {@code vehicle_type},
 * {@code connector_type}) are handled by {@link StringListConverter}.
 *
 * <p>Satisfies Requirements 1.2, 2.2, 2.4, 2.5.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /** Firebase UID — used as the primary key. */
    @Id
    @Column(nullable = false)
    private String id;

    @Column(unique = true, length = 15)
    private String phone;

    @Column(length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * User roles: {@code ['driver']}, {@code ['host']}, or {@code ['driver','host']}.
     * Stored as a PostgreSQL {@code text[]} column.
     */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text[]")
    private List<String> role;

    /**
     * Connector types owned by the user (driver perspective).
     * Stored as a PostgreSQL {@code text[]} column.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "vehicle_type", columnDefinition = "text[]")
    private List<String> vehicleType;

    /**
     * Preferred connector types (driver filter preference).
     * Stored as a PostgreSQL {@code text[]} column.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "connector_type", columnDefinition = "text[]")
    private List<String> connectorType;

    @Column(name = "bank_account_number", length = 20)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 11)
    private String bankIfsc;

    @Column(name = "is_host_verified")
    private boolean isHostVerified;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
