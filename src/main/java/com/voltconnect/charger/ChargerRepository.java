package com.voltconnect.charger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChargerEntity}.
 *
 * <p>The {@link #findNearby} method uses a native Haversine formula query to
 * return all {@code active} chargers within a given radius (in kilometres) of
 * the supplied latitude/longitude coordinates.
 *
 * <p>Satisfies Requirements 3.2, 4.1, 4.2, 4.3, 13.1.
 */
public interface ChargerRepository extends JpaRepository<ChargerEntity, UUID> {

    /**
     * Returns all chargers owned by the given host.
     */
    List<ChargerEntity> findByHostId(UUID hostId);

    /**
     * Geospatial nearby search using the Haversine formula.
     * Only returns chargers with {@code status = 'active'}.
     *
     * @param lat      centre latitude in decimal degrees
     * @param lng      centre longitude in decimal degrees
     * @param radiusKm search radius in kilometres
     * @return chargers within the radius
     */
    @Query(value = """
            SELECT * FROM chargers c
            WHERE c.status = 'active'
            AND (6371 * acos(cos(radians(:lat)) * cos(radians(c.latitude))
                * cos(radians(c.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(c.latitude)))) <= :radiusKm
            """, nativeQuery = true)
    List<ChargerEntity> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );

    /**
     * Returns {@code true} if a charger with the given {@code id} exists and
     * belongs to the given {@code hostId}. Used for ownership checks.
     */
    boolean existsByIdAndHostId(UUID id, UUID hostId);
}
