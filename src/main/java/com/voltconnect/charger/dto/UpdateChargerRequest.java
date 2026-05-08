package com.voltconnect.charger.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for partially updating an existing charger listing.
 *
 * <p>All fields are optional (nullable). Only non-null fields are applied
 * during the update (partial update semantics).
 *
 * <p>Satisfies Requirements 4.1, 4.2, 4.3.
 */
public record UpdateChargerRequest(

        String title,

        String description,

        Double latitude,

        Double longitude,

        String address,

        List<String> connectorType,

        BigDecimal powerKw,

        BigDecimal pricePerHour,

        List<String> photos
) {}
