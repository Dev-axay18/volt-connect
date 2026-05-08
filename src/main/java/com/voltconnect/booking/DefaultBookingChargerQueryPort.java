package com.voltconnect.booking;

import com.voltconnect.charger.ChargerEntity;
import com.voltconnect.charger.ChargerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link BookingChargerQueryPort} that delegates to
 * {@link ChargerRepository}.
 *
 * <p>This component lives in the booking package but depends on the charger
 * repository directly. The dependency direction is booking → charger (one-way),
 * which avoids a circular dependency.
 *
 * <p>Satisfies Requirements 6.1, 6.2.
 */
@Component
@RequiredArgsConstructor
public class DefaultBookingChargerQueryPort implements BookingChargerQueryPort {

    private final ChargerRepository chargerRepository;

    @Override
    public Optional<ChargerInfo> findChargerById(UUID chargerId) {
        return chargerRepository.findById(chargerId)
                .map(this::toChargerInfo);
    }

    private ChargerInfo toChargerInfo(ChargerEntity entity) {
        return new ChargerInfo(
            entity.getId(),
            entity.getHostId(),
            entity.getPricePerHour(),
            entity.getStatus(),
            entity.isAvailable()
        );
    }
}
