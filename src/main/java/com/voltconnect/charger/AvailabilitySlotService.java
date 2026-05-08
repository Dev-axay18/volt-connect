package com.voltconnect.charger;

import com.voltconnect.charger.dto.AvailabilitySlotDto;
import com.voltconnect.charger.dto.AvailabilitySlotRequest;
import com.voltconnect.shared.exceptions.BadRequestException;
import com.voltconnect.shared.exceptions.ConflictException;
import com.voltconnect.shared.exceptions.ForbiddenException;
import com.voltconnect.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for managing charger availability slots.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Ownership: only the charger's host may manage its slots.</li>
 *   <li>Time ordering: {@code endTime} must be strictly after {@code startTime}.</li>
 *   <li>No overlaps: new slots must not overlap with existing slots on the same day.</li>
 * </ul>
 *
 * <p>Satisfies Requirements 11.1, 11.2, 11.3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilitySlotService {

    private final AvailabilitySlotRepository slotRepository;
    private final ChargerRepository chargerRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new availability slot for the given charger.
     *
     * @param hostId    the authenticated host's user ID
     * @param chargerId the charger to add the slot to
     * @param request   the slot details
     * @return the created slot as a DTO
     * @throws ForbiddenException        if the charger does not belong to this host
     * @throws BadRequestException       if {@code endTime <= startTime}
     * @throws ConflictException         if the slot overlaps with an existing slot
     */
    @Transactional
    public AvailabilitySlotDto createSlot(UUID hostId, UUID chargerId,
                                           AvailabilitySlotRequest request) {
        verifyChargerOwnership(chargerId, hostId);
        validateTimeOrder(request.startTime(), request.endTime());
        checkForOverlap(chargerId, request.dayOfWeek(), request.startTime(), request.endTime(), null);

        boolean active = request.isActive() == null || request.isActive();

        AvailabilitySlotEntity slot = AvailabilitySlotEntity.builder()
                .chargerId(chargerId)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .isActive(active)
                .build();

        AvailabilitySlotEntity saved = slotRepository.save(slot);
        log.info("Availability slot '{}' created for charger '{}' by host '{}'",
                saved.getId(), chargerId, hostId);
        return AvailabilitySlotDto.from(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Updates an existing availability slot.
     *
     * @param hostId  the authenticated host's user ID
     * @param slotId  the slot to update
     * @param request the new slot details
     * @return the updated slot as a DTO
     * @throws ResourceNotFoundException if the slot does not exist
     * @throws ForbiddenException        if the slot's charger does not belong to this host
     * @throws BadRequestException       if {@code endTime <= startTime}
     * @throws ConflictException         if the updated slot overlaps with another slot
     */
    @Transactional
    public AvailabilitySlotDto updateSlot(UUID hostId, UUID slotId,
                                           AvailabilitySlotRequest request) {
        AvailabilitySlotEntity slot = loadSlot(slotId);
        verifyChargerOwnership(slot.getChargerId(), hostId);
        validateTimeOrder(request.startTime(), request.endTime());
        checkForOverlap(slot.getChargerId(), request.dayOfWeek(),
                request.startTime(), request.endTime(), slotId);

        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        if (request.isActive() != null) {
            slot.setActive(request.isActive());
        }

        AvailabilitySlotEntity saved = slotRepository.save(slot);
        log.info("Availability slot '{}' updated by host '{}'", slotId, hostId);
        return AvailabilitySlotDto.from(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes an availability slot after verifying ownership.
     *
     * @param hostId the authenticated host's user ID
     * @param slotId the slot to delete
     * @throws ResourceNotFoundException if the slot does not exist
     * @throws ForbiddenException        if the slot's charger does not belong to this host
     */
    @Transactional
    public void deleteSlot(UUID hostId, UUID slotId) {
        AvailabilitySlotEntity slot = loadSlot(slotId);
        verifyChargerOwnership(slot.getChargerId(), hostId);
        slotRepository.delete(slot);
        log.info("Availability slot '{}' deleted by host '{}'", slotId, hostId);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns all active availability slots for the given charger.
     *
     * @param chargerId the charger UUID
     * @return list of active slot DTOs
     */
    @Transactional(readOnly = true)
    public List<AvailabilitySlotDto> getChargerSlots(UUID chargerId) {
        return slotRepository.findByChargerIdAndIsActiveTrue(chargerId)
                .stream()
                .map(AvailabilitySlotDto::from)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AvailabilitySlotEntity loadSlot(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AvailabilitySlot", slotId.toString()));
    }

    private void verifyChargerOwnership(UUID chargerId, UUID hostId) {
        if (!chargerRepository.existsByIdAndHostId(chargerId, hostId)) {
            throw new ForbiddenException(
                    "You do not have permission to manage slots for charger: " + chargerId);
        }
    }

    private void validateTimeOrder(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException(
                    "End time must be strictly after start time");
        }
    }

    /**
     * Checks whether the proposed [startTime, endTime] window overlaps with any
     * existing slot for the same charger and day.
     *
     * <p>Two slots overlap if NOT (newEnd <= existingStart OR newStart >= existingEnd).
     *
     * @param chargerId   the charger UUID
     * @param dayOfWeek   the day to check
     * @param newStart    proposed start time
     * @param newEnd      proposed end time
     * @param excludeSlot slot ID to exclude from the check (used during updates)
     */
    private void checkForOverlap(UUID chargerId, int dayOfWeek,
                                  LocalTime newStart, LocalTime newEnd,
                                  UUID excludeSlot) {
        List<AvailabilitySlotEntity> existing =
                slotRepository.findByChargerIdAndDayOfWeek(chargerId, dayOfWeek);

        for (AvailabilitySlotEntity slot : existing) {
            // Skip the slot being updated
            if (excludeSlot != null && slot.getId().equals(excludeSlot)) {
                continue;
            }
            LocalTime existingStart = slot.getStartTime();
            LocalTime existingEnd   = slot.getEndTime();

            boolean overlaps = !(newEnd.compareTo(existingStart) <= 0
                    || newStart.compareTo(existingEnd) >= 0);

            if (overlaps) {
                throw new ConflictException(
                        String.format("Slot [%s–%s] overlaps with existing slot [%s–%s] on day %d",
                                newStart, newEnd, existingStart, existingEnd, dayOfWeek));
            }
        }
    }
}
