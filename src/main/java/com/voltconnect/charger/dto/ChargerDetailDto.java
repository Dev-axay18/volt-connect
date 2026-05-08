package com.voltconnect.charger.dto;

import com.voltconnect.charger.ChargerEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full charger representation returned for detail views and host management.
 *
 * <p>Extends the summary fields with description, power, photos, session stats,
 * host identity, and compliance document URLs.
 *
 * <p>Satisfies Requirements 4.1, 4.2, 4.3, 5.1, 13.2, 13.3, 13.4, 13.5.
 */
public record ChargerDetailDto(

        UUID id,
        double latitude,
        double longitude,
        String title,
        String address,
        String status,
        List<String> connectorType,
        BigDecimal pricePerHour,
        Double averageRating,
        boolean isAvailable,

        // Extended fields
        String description,
        BigDecimal powerKw,
        List<String> photos,
        int totalSessions,
        UUID hostId,
        String electricityBillUrl,
        String bisCertificateUrl
) {

    /**
     * Creates a {@link ChargerDetailDto} from a {@link ChargerEntity}.
     */
    public static ChargerDetailDto from(ChargerEntity entity) {
        return new ChargerDetailDto(
                entity.getId(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getTitle(),
                entity.getAddress(),
                entity.getStatus(),
                entity.getConnectorType(),
                entity.getPricePerHour(),
                entity.getAverageRating(),
                entity.isAvailable(),
                entity.getDescription(),
                entity.getPowerKw(),
                entity.getPhotos(),
                entity.getTotalSessions(),
                entity.getHostId(),
                entity.getElectricityBillUrl(),
                entity.getBisCertificateUrl()
        );
    }
}
