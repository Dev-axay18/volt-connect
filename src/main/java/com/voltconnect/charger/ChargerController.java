package com.voltconnect.charger;

import com.voltconnect.charger.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for charger management and nearby search.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/v1/chargers/nearby}              — search nearby chargers (public)</li>
 *   <li>{@code GET  /api/v1/chargers/{id}}                — get charger detail (authenticated)</li>
 *   <li>{@code POST /api/v1/chargers}                     — create charger (ROLE_HOST)</li>
 *   <li>{@code PUT  /api/v1/chargers/{id}}                — update charger (ROLE_HOST)</li>
 *   <li>{@code DELETE /api/v1/chargers/{id}}              — delete charger (ROLE_HOST)</li>
 *   <li>{@code GET  /api/v1/chargers/host/my-chargers}    — list host's chargers (ROLE_HOST)</li>
 *   <li>{@code PUT  /api/v1/chargers/{id}/toggle}         — toggle availability (ROLE_HOST)</li>
 *   <li>{@code POST /api/v1/chargers/{chargerId}/slots}   — create slot (ROLE_HOST)</li>
 *   <li>{@code PUT  /api/v1/chargers/{chargerId}/slots/{slotId}} — update slot (ROLE_HOST)</li>
 *   <li>{@code DELETE /api/v1/chargers/{chargerId}/slots/{slotId}} — delete slot (ROLE_HOST)</li>
 *   <li>{@code GET  /api/v1/chargers/{chargerId}/slots}   — list slots (authenticated)</li>
 * </ul>
 *
 * <p>Satisfies Requirements 3.2, 4.1, 4.2, 4.3, 5.1, 11.1, 11.2, 11.3,
 * 13.1, 13.2, 13.3, 13.4, 13.5, 16.4, 16.5, 19.1.
 */
@RestController
@RequestMapping("/api/v1/chargers")
@RequiredArgsConstructor
public class ChargerController {

    private final ChargerService chargerService;
    private final AvailabilitySlotService slotService;

    // ── Nearby search ─────────────────────────────────────────────────────────

    /**
     * Returns chargers within {@code radiusKm} of the given coordinates.
     * Optional filter parameters are applied in-memory after the geospatial query.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<ChargerSummaryDto>> getNearbyChargers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radiusKm,
            @RequestParam(required = false) List<String> connectorTypes,
            @RequestParam(required = false) Double minPowerKw,
            @RequestParam(required = false) BigDecimal maxPricePerHour,
            @RequestParam(required = false) Boolean availableOnly) {

        ChargerFilterDto filters = new ChargerFilterDto(
                connectorTypes, minPowerKw, maxPricePerHour, availableOnly);

        List<ChargerSummaryDto> results =
                chargerService.getNearbyChargers(lat, lng, radiusKm, filters);
        return ResponseEntity.ok(results);
    }

    // ── Host chargers — must be declared before /{id} to avoid path conflict ──

    /**
     * Returns all chargers owned by the authenticated host.
     */
    @GetMapping("/host/my-chargers")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<List<ChargerDetailDto>> getMyChargers(
            @AuthenticationPrincipal String hostId) {

        List<ChargerDetailDto> chargers =
                chargerService.getHostChargers(UUID.fromString(hostId));
        return ResponseEntity.ok(chargers);
    }

    // ── Charger detail ────────────────────────────────────────────────────────

    /**
     * Returns the full detail of a single charger.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChargerDetailDto> getChargerDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(chargerService.getChargerDetail(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new charger listing. Requires ROLE_HOST.
     */
    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ChargerDetailDto> createCharger(
            @AuthenticationPrincipal String hostId,
            @Valid @RequestBody CreateChargerRequest request) {

        ChargerDetailDto created =
                chargerService.createCharger(UUID.fromString(hostId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Partially updates a charger. Requires ROLE_HOST.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ChargerDetailDto> updateCharger(
            @AuthenticationPrincipal String hostId,
            @PathVariable UUID id,
            @RequestBody UpdateChargerRequest request) {

        ChargerDetailDto updated =
                chargerService.updateCharger(UUID.fromString(hostId), id, request);
        return ResponseEntity.ok(updated);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a charger. Requires ROLE_HOST.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> deleteCharger(
            @AuthenticationPrincipal String hostId,
            @PathVariable UUID id) {

        chargerService.deleteCharger(UUID.fromString(hostId), id);
        return ResponseEntity.noContent().build();
    }

    // ── Availability toggle ───────────────────────────────────────────────────

    /**
     * Toggles the {@code isAvailable} flag on a charger. Requires ROLE_HOST.
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ChargerSummaryDto> toggleAvailability(
            @AuthenticationPrincipal String hostId,
            @PathVariable UUID id) {

        ChargerSummaryDto result =
                chargerService.toggleAvailability(UUID.fromString(hostId), id);
        return ResponseEntity.ok(result);
    }

    // ── Availability slots ────────────────────────────────────────────────────

    /**
     * Creates a new availability slot for the given charger. Requires ROLE_HOST.
     */
    @PostMapping("/{chargerId}/slots")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<AvailabilitySlotDto> createSlot(
            @AuthenticationPrincipal String hostId,
            @PathVariable UUID chargerId,
            @Valid @RequestBody AvailabilitySlotRequest request) {

        AvailabilitySlotDto created =
                slotService.createSlot(UUID.fromString(hostId), chargerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing availability slot. Requires ROLE_HOST.
     */
    @PutMapping("/{chargerId}/slots/{slotId}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<AvailabilitySlotDto> updateSlot(
            @AuthenticationPrincipal String hostId,
            @PathVariable UUID chargerId,
            @PathVariable UUID slotId,
            @Valid @RequestBody AvailabilitySlotRequest request) {

        AvailabilitySlotDto updated =
                slotService.updateSlot(UUID.fromString(hostId), slotId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes an availability slot. Requires ROLE_HOST.
     */
    @DeleteMapping("/{chargerId}/slots/{slotId}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> deleteSlot(
            @AuthenticationPrincipal String hostId,
            @PathVariable UUID chargerId,
            @PathVariable UUID slotId) {

        slotService.deleteSlot(UUID.fromString(hostId), slotId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns all active availability slots for the given charger.
     */
    @GetMapping("/{chargerId}/slots")
    public ResponseEntity<List<AvailabilitySlotDto>> getChargerSlots(
            @PathVariable UUID chargerId) {

        return ResponseEntity.ok(slotService.getChargerSlots(chargerId));
    }
}
