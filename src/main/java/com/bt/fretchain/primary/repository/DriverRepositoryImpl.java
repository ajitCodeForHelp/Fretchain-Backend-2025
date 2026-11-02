package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.dto.AvailableDriverResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DriverRepositoryImpl
        implements DriverRepositoryCustom {

    private final EntityManager entityManager;


    @Override
    public List<AvailableDriverResponse> getAvailableDriversForOrder(
            Long orderId
    ) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "SP_GET_AVAILABLE_DRIVERS_FOR_ORDER"
                );


        query.registerStoredProcedureParameter(
                "P_ORDER_ID",
                Long.class,
                ParameterMode.IN
        );


        query.setParameter(
                "P_ORDER_ID",
                orderId
        );


        List<Object[]> rows = query.getResultList();

        List<AvailableDriverResponse> response =
                new ArrayList<>();


        for (Object[] row : rows) {

            AvailableDriverResponse driver =
                    AvailableDriverResponse.builder()

                            .driverId(
                                    toLong(row[0])
                            )

                            .fullName(
                                    toStringValue(row[1])
                            )

                            .mobile(
                                    toStringValue(row[2])
                            )

                            .countryCode(
                                    toStringValue(row[3])
                            )

                            .profileImageUrl(
                                    toStringValue(row[4])
                            )

                            .verified(
                                    toBoolean(row[5])
                            )

                            .available(
                                    toBoolean(row[6])
                            )

                            .onlineStatus(
                                    toBoolean(row[7])
                            )

                            .overallRating(
                                    toBigDecimal(row[8])
                            )

                            .currentLatitude(
                                    toDouble(row[9])
                            )

                            .currentLongitude(
                                    toDouble(row[10])
                            )

                            .lastLocationUpdatedAt(
                                    toLocalDateTime(row[11])
                            )

                            .vehicleId(
                                    toLong(row[12])
                            )

                            .vehicleNumber(
                                    toStringValue(row[13])
                            )

                            .vehicleCategoryId(
                                    toLong(row[14])
                            )

                            .vehicleCategoryNameEn(
                                    toStringValue(row[15])
                            )

                            .vehicleCategoryNameFr(
                                    toStringValue(row[16])
                            )

                            .vehicleTypeId(
                                    toLong(row[17])
                            )

                            .vehicleTypeNameEn(
                                    toStringValue(row[18])
                            )

                            .vehicleTypeNameFr(
                                    toStringValue(row[19])
                            )

                            .distanceKm(
                                    toDouble(row[20])
                            )

                            .build();


            response.add(driver);
        }

        return response;
    }


    private Long toLong(Object value) {

        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }


    private Double toDouble(Object value) {

        if (value == null) {
            return null;
        }

        return ((Number) value).doubleValue();
    }


    private BigDecimal toBigDecimal(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        return new BigDecimal(value.toString());
    }


    private Boolean toBoolean(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        return Boolean.parseBoolean(value.toString());
    }


    private String toStringValue(Object value) {

        return value != null
                ? value.toString()
                : null;
    }


    private LocalDateTime toLocalDateTime(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        return null;
    }
}