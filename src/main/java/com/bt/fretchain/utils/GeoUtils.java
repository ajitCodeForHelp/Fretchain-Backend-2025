package com.bt.fretchain.utils;

/**
 * Lightweight geo helpers for "nearby" lookups.
 *
 * Nearby search = cheap bounding-box filter (uses the lat/lng index)
 *               + exact Haversine distance on the remaining rows.
 */
public final class GeoUtils {

    public static final double EARTH_RADIUS_KM = 6371.0;

    /** Approx. km per degree of latitude. */
    private static final double KM_PER_DEGREE = 111.045;

    /** Used for the delivery ETA estimate: minutes of riding per km. */
    public static final int RIDE_MINUTES_PER_KM = 3;

    /** Used for the ETA when a store has not configured its preparation time. */
    public static final int DEFAULT_PREPARATION_MINUTES = 15;

    private GeoUtils() {
    }

    public static double haversineKm(
            double lat1, double lng1, double lat2, double lng2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** @return {minLat, maxLat, minLng, maxLng} around the point. */
    public static double[] boundingBox(double lat, double lng, double radiusKm) {
        double latDelta = radiusKm / KM_PER_DEGREE;
        double cosLat = Math.max(0.01, Math.cos(Math.toRadians(lat)));
        double lngDelta = radiusKm / (KM_PER_DEGREE * cosLat);

        return new double[]{
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta
        };
    }

    public static int estimateDeliveryMinutes(
            Integer preparationMinutes, double distanceKm) {

        int prep = preparationMinutes == null
                ? DEFAULT_PREPARATION_MINUTES : preparationMinutes;

        return prep + (int) Math.ceil(distanceKm * RIDE_MINUTES_PER_KM);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
