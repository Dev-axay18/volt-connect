package com.voltconnect.charger.dto;

import com.voltconnect.charger.ChargerEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight charger representation used for map markers and list views.
 *
 * <p>Satisfies Requirements 3.2, 13.1.
 */
public record ChargerSummaryDto(

        UUID id,
        double latitude,
        double longitude,
        String title,
        String address,
        String status,
        List<String> connectorType,
        BigDecimal pricePerHour,
        Double averageRating,
        boolean isAvailable
) {

    /**
     * Creates a {@link ChargerSummaryDto} from a {@link ChargerEntity}.
     */
    public static ChargerSummaryDto from(ChargerEntity entity) {
        return new ChargerSummaryDto(
                entity.getId(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getTitle(),
                entity.getAddress(),
                entity.getStatus(),
                entity.getConnectorType(),
                entity.getPricePerHour(),
                entity.getAverageRating(),
                entity.isAvailable()
        );
    }
}
