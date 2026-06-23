package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.CustomerHomePageDTO;
import com.bt.fretchain.primary.dto.DistanceTimeDTO;
import com.bt.fretchain.primary.pojo.ServiceRadius;
import com.bt.fretchain.primary.repository.ServiceRadiusRepository;
import com.bt.fretchain.primary.service.DistanceCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DistanceCalculationServiceImpl
        implements DistanceCalculationService {

    private final ServiceRadiusRepository serviceRadiusRepository;

    private final RestTemplate restTemplate;

    @Value("${google.maps.api-key:}")
    private String googleApiKey;

    @Value("${distance.provider:HAVERSINE}")
    private String distanceProvider;

    @Override
    public boolean isLocationInsideServiceRadius(
            Double latitude,
            Double longitude
    ) {

        List<ServiceRadius> radiusList =
                serviceRadiusRepository.findByActiveTrueAndDeletedFalse();

        for (ServiceRadius radius : radiusList) {

            double distanceInMeter =
                    calculateDistanceInMeter(
                            latitude,
                            longitude,
                            radius.getCenterLatitude(),
                            radius.getCenterLongitude()
                    );

            if (distanceInMeter <= radius.getRadiusInMeter()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public DistanceTimeDTO calculateMultiDropDistance(
            Double pickupLat,
            Double pickupLng,
            List<CustomerHomePageDTO.DropLocationRequest> dropLocations
    ) {

        double totalDistanceKm = 0.0;
        int totalTimeMinutes = 0;

        double startLat = pickupLat;
        double startLng = pickupLng;

        for (CustomerHomePageDTO.DropLocationRequest drop : dropLocations) {

            DistanceTimeDTO segmentDistance;

            try {

                if ("GOOGLE".equalsIgnoreCase(distanceProvider)) {
                    segmentDistance =
                            calculateByGoogle(
                                    startLat,
                                    startLng,
                                    drop.getLatitude(),
                                    drop.getLongitude()
                            );
                } else {
                    segmentDistance =
                            calculateByHaversine(
                                    startLat,
                                    startLng,
                                    drop.getLatitude(),
                                    drop.getLongitude()
                            );
                }

            } catch (Exception ex) {

                segmentDistance =
                        calculateByHaversine(
                                startLat,
                                startLng,
                                drop.getLatitude(),
                                drop.getLongitude()
                        );
            }

            totalDistanceKm += segmentDistance.getDistanceKm();
            totalTimeMinutes += segmentDistance.getEstimatedTimeMinutes();

            startLat = drop.getLatitude();
            startLng = drop.getLongitude();
        }

        DistanceTimeDTO total = new DistanceTimeDTO();
        total.setDistanceKm(
                Math.round(totalDistanceKm * 100.0) / 100.0
        );
        total.setEstimatedTimeMinutes(totalTimeMinutes);

        return total;
    }

    @Override
    public DistanceTimeDTO calculateByHaversine(
            Double pickupLat,
            Double pickupLng,
            Double dropLat,
            Double dropLng
    ) {

        double distanceKm =
                calculateDistanceInMeter(
                        pickupLat,
                        pickupLng,
                        dropLat,
                        dropLng
                ) / 1000.0;

        DistanceTimeDTO dto = new DistanceTimeDTO();
        dto.setDistanceKm(
                Math.round(distanceKm * 100.0) / 100.0
        );

        dto.setEstimatedTimeMinutes(
                (int) Math.ceil((distanceKm / 35.0) * 60)
        );

        return dto;
    }

    @Override
    public DistanceTimeDTO calculateByGoogle(
            Double pickupLat,
            Double pickupLng,
            Double dropLat,
            Double dropLng
    ) {

        String url =
                "https://maps.googleapis.com/maps/api/distancematrix/json"
                        + "?units=metric"
                        + "&origins=" + pickupLat + "," + pickupLng
                        + "&destinations=" + dropLat + "," + dropLng
                        + "&key=" + googleApiKey;

        Map response =
                restTemplate.getForObject(url, Map.class);

        if (response == null || !"OK".equals(response.get("status"))) {
            throw new RuntimeException("Google distance API failed");
        }

        List rows = (List) response.get("rows");
        Map row = (Map) rows.get(0);

        List elements = (List) row.get("elements");
        Map element = (Map) elements.get(0);

        if (!"OK".equals(element.get("status"))) {
            throw new RuntimeException("Route not found");
        }

        Map distance = (Map) element.get("distance");
        Map duration = (Map) element.get("duration");

        Number distanceMeter = (Number) distance.get("value");
        Number durationSecond = (Number) duration.get("value");

        DistanceTimeDTO dto = new DistanceTimeDTO();

        dto.setDistanceKm(
                Math.round(
                        (distanceMeter.doubleValue() / 1000.0) * 100.0
                ) / 100.0
        );

        dto.setEstimatedTimeMinutes(
                (int) Math.ceil(durationSecond.doubleValue() / 60.0)
        );

        return dto;
    }

    private double calculateDistanceInMeter(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        final int earthRadiusInMeter = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        +
                        Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return earthRadiusInMeter * c;
    }
}