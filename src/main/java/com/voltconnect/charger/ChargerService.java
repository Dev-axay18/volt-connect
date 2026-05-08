package com.voltconnect.charger;

import com.voltconnect.charger.dto.*;
import com.voltconnect.shared.exceptions.ConflictException;
import com.voltconnect.shared.exceptions.ForbiddenException;
import com.voltconnect.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for charger management in the Volt-Connect platform.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Nearby charger search with in-memory filtering ({@link #getNearbyChargers}).</li>
 *   <li>Charger CRUD operations with ownership enforcement.</li>
 *   <li>Availability toggle with status state machine ({@link #toggleAvailability}).</li>
 * </ul>
 *
 * <p>Satisfies Requirements 3.2, 4.1, 4.2, 4.3, 5.1, 13.1, 13.2, 13.3, 13.4, 13.5,
 * 16.4, 16.5, 19.1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargerService {

    private final ChargerRepository chargerRepository;
    private final ChargerBookingQueryPort bookingQueryPort;

    // ── Nearby search ─────────────────────────────────────────────────────────

    /**
     * Returns chargers within {@code radiusKm} of the given coordinates, with
     * optional in-memory filtering applied after the geospatial query.
     *
     * @param lat       centre latitude
     * @param lng       centre longitude
     * @param radiusKm  search radius in kilometres
     * @param filters   optional filter criteria (may be null)
     * @return filtered list of charger summaries
     */
    @Transactional(readOnly = true)
    public List<ChargerSummaryDto> getNearbyChargers(double lat, double lng, double radiusKm,
                                                      ChargerFilterDto filters) {
        List<ChargerEntity> nearby = chargerRepository.findNearby(lat, lng, radiusKm);

        if (filters == null) {
            return nearby.stream().map(ChargerSummaryDto::from).collect(Collectors.toList());
        }

        return nearby.stream()
                .filter(c -> matchesConnectorFilter(c, filters))
                .filter(c -> matchesPowerFilter(c, filters))
                .filter(c -> matchesPriceFilter(c, filters))
                .filter(c -> matchesAvailabilityFilter(c, filters))
                .map(ChargerSummaryDto::from)
                .collect(Collectors.toList());
    }

    // ── Charger detail ────────────────────────────────────────────────────────

    /**
     * Returns the full detail of a single charger.
     *
     * @param chargerId the charger UUID
     * @return charger detail DTO
     * @throws ResourceNotFoundException if no charger with that ID exists
     */
    @Transactional(readOnly = true)
    public ChargerDetailDto getChargerDetail(UUID chargerId) {
        ChargerEntity charger = loadCharger(chargerId);
        return ChargerDetailDto.from(charger);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new charger listing for the given host.
     *
     * <p>New chargers start with {@code status = "pending_verification"} and
     * {@code isAvailable = false} until the host is verified.
     *
     * @param hostId  the authenticated host's user ID
     * @param request the charger details
     * @return the created charger as a detail DTO
     */
    @Transactional
    public ChargerDetailDto createCharger(UUID hostId, CreateChargerRequest request) {
        ChargerEntity charger = ChargerEntity.builder()
                .hostId(hostId)
                .title(request.title())
                .description(request.description())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .address(request.address())
                .connectorType(request.connectorType())
                .powerKw(request.powerKw())
                .pricePerHour(request.pricePerHour())
                .photos(request.photos())
                .isAvailable(false)
                .isVerified(false)
                .status("pending_verification")
                .totalSessions(0)
                .averageRating(0.0)
                .createdAt(Instant.now())
                .build();

        ChargerEntity saved = chargerRepository.save(charger);
        log.info("Charger '{}' created by host '{}'", saved.getId(), hostId);
        return ChargerDetailDto.from(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Partially updates a charger. Only non-null fields in the request are applied.
     *
     * <p>If {@code pricePerHour} changes, the update applies to the entity only;
     * existing confirmed bookings already have their amounts locked.
     *
     * @param hostId    the authenticated host's user ID
     * @param chargerId the charger to update
     * @param request   the fields to update (all optional)
     * @return the updated charger as a detail DTO
     * @throws ResourceNotFoundException if the charger does not exist
     * @throws ForbiddenException        if the charger does not belong to this host
     */
    @Transactional
    public ChargerDetailDto updateCharger(UUID hostId, UUID chargerId, UpdateChargerRequest request) {
        ChargerEntity charger = loadCharger(chargerId);
        verifyOwnership(charger, hostId);

        if (request.title() != null)         charger.setTitle(request.title());
        if (request.description() != null)   charger.setDescription(request.description());
        if (request.latitude() != null)      charger.setLatitude(request.latitude());
        if (request.longitude() != null)     charger.setLongitude(request.longitude());
        if (request.address() != null)       charger.setAddress(request.address());
        if (request.connectorType() != null) charger.setConnectorType(request.connectorType());
        if (request.powerKw() != null)       charger.setPowerKw(request.powerKw());
        if (request.pricePerHour() != null)  charger.setPricePerHour(request.pricePerHour());
        if (request.photos() != null)        charger.setPhotos(request.photos());

        ChargerEntity saved = chargerRepository.save(charger);
        log.info("Charger '{}' updated by host '{}'", chargerId, hostId);
        return ChargerDetailDto.from(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a charger after verifying ownership and checking for active bookings.
     *
     * @param hostId    the authenticated host's user ID
     * @param chargerId the charger to delete
     * @throws ResourceNotFoundException if the charger does not exist
     * @throws ForbiddenException        if the charger does not belong to this host
     * @throws ConflictException         if there are active/upcoming confirmed bookings
     */
    @Transactional
    public void deleteCharger(UUID hostId, UUID chargerId) {
        ChargerEntity charger = loadCharger(chargerId);
        verifyOwnership(charger, hostId);

        List<UUID> conflictingBookingIds = bookingQueryPort.findActiveBookingIdsForCharger(chargerId);
        if (!conflictingBookingIds.isEmpty()) {
            String ids = conflictingBookingIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));
            throw new ConflictException(
                    "Cannot delete charger: active or upcoming bookings exist: [" + ids + "]");
        }

        chargerRepository.delete(charger);
        log.info("Charger '{}' deleted by host '{}'", chargerId, hostId);
    }

    // ── Host chargers ─────────────────────────────────────────────────────────

    /**
     * Returns all chargers owned by the given host.
     *
     * @param hostId the host's user ID
     * @return list of charger detail DTOs
     */
    @Transactional(readOnly = true)
    public List<ChargerDetailDto> getHostChargers(UUID hostId) {
        return chargerRepository.findByHostId(hostId)
                .stream()
                .map(ChargerDetailDto::from)
                .collect(Collectors.toList());
    }

    // ── Availability toggle ───────────────────────────────────────────────────

    /**
     * Flips the {@code isAvailable} flag on a charger and updates the status
     * according to the state machine:
     * <ul>
     *   <li>{@code isAvailable=true} AND {@code isVerified=true} → {@code "active"}</li>
     *   <li>{@code isAvailable=false} → {@code "inactive"}</li>
     * </ul>
     *
     * @param hostId    the authenticated host's user ID
     * @param chargerId the charger to toggle
     * @return the updated charger as a summary DTO
     * @throws ResourceNotFoundException if the charger does not exist
     * @throws ForbiddenException        if the charger does not belong to this host
     */
    @Transactional
    public ChargerSummaryDto toggleAvailability(UUID hostId, UUID chargerId) {
        ChargerEntity charger = loadCharger(chargerId);
        verifyOwnership(charger, hostId);

        boolean newAvailability = !charger.isAvailable();
        charger.setAvailable(newAvailability);

        if (newAvailability && charger.isVerified()) {
            charger.setStatus("active");
        } else if (!newAvailability) {
            charger.setStatus("inactive");
        }
        // If newAvailability=true but not yet verified, status stays as-is
        // (pending_verification or whatever it was)

        ChargerEntity saved = chargerRepository.save(charger);
        log.info("Charger '{}' availability toggled to {} by host '{}'",
                chargerId, newAvailability, hostId);
        return ChargerSummaryDto.from(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ChargerEntity loadCharger(UUID chargerId) {
        return chargerRepository.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger", chargerId.toString()));
    }

    private void verifyOwnership(ChargerEntity charger, UUID hostId) {
        if (!charger.getHostId().equals(hostId)) {
            throw new ForbiddenException(
                    "You do not have permission to modify charger: " + charger.getId());
        }
    }

    // ── Filter predicates ─────────────────────────────────────────────────────

    private boolean matchesConnectorFilter(ChargerEntity c, ChargerFilterDto filters) {
        if (filters.connectorTypes() == null || filters.connectorTypes().isEmpty()) {
            return true;
        }
        List<String> chargerConnectors = c.getConnectorType();
        if (chargerConnectors == null || chargerConnectors.isEmpty()) {
            return false;
        }
        return chargerConnectors.stream().anyMatch(filters.connectorTypes()::contains);
    }

    private boolean matchesPowerFilter(ChargerEntity c, ChargerFilterDto filters) {
        if (filters.minPowerKw() == null) {
            return true;
        }
        if (c.getPowerKw() == null) {
            return false;
        }
        return c.getPowerKw().doubleValue() >= filters.minPowerKw();
    }

    private boolean matchesPriceFilter(ChargerEntity c, ChargerFilterDto filters) {
        if (filters.maxPricePerHour() == null) {
            return true;
        }
        if (c.getPricePerHour() == null) {
            return false;
        }
        return c.getPricePerHour().compareTo(filters.maxPricePerHour()) <= 0;
    }

    private boolean matchesAvailabilityFilter(ChargerEntity c, ChargerFilterDto filters) {
        if (filters.availableOnly() == null || !filters.availableOnly()) {
            return true;
        }
        return c.isAvailable();
    }
}
