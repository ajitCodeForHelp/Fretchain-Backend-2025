package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.dto.CustomerRestaurantDTO.RestaurantSortBy;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import com.bt.fretchain.utils.GeoUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Native MySQL implementation of the nearby-restaurant lookup.
 *
 * 1. Bounding box on latitude/longitude (index friendly) removes far away rows.
 * 2. Haversine gives the exact distance for the remaining rows.
 * 3. Store must cover the customer with its own delivery radius.
 *
 * If the number of stores grows very large, switch latitude/longitude to a
 * POINT SRID 4326 column with a SPATIAL index and use ST_Distance_Sphere;
 * the service layer does not need to change.
 */
@Repository
@RequiredArgsConstructor
public class RestaurantMasterRepositoryImpl implements RestaurantMasterRepositoryCustom {

    private static final String DISTANCE_EXPR = """
            (6371 * ACOS(LEAST(1, GREATEST(-1,
                COS(RADIANS(:lat)) * COS(RADIANS(s.latitude))
                * COS(RADIANS(s.longitude) - RADIANS(:lng))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))))))""";

    /** Handles stores open past midnight (closing_time < opening_time). */
    private static final String OPEN_NOW_EXPR = """
            (CASE WHEN (s.accepting_orders IS NULL OR s.accepting_orders = 1)
                   AND (s.opening_time IS NULL OR s.closing_time IS NULL
                        OR (s.opening_time <= s.closing_time
                            AND :now >= s.opening_time AND :now < s.closing_time)
                        OR (s.opening_time > s.closing_time
                            AND (:now >= s.opening_time OR :now < s.closing_time)))
                  THEN 1 ELSE 0 END)""";

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<NearbyRestaurantRow> findNearbyRestaurants(NearbyRestaurantCriteria criteria) {

        String sql = "SELECT t.restaurant_id, t.distance_km, t.open_now FROM ("
                + innerSql(criteria)
                + ") t "
                + outerWhere(criteria)
                + orderBy(criteria.sortBy())
                + " LIMIT :limit OFFSET :offset";

        Query query = entityManager.createNativeQuery(sql);
        bind(query, criteria);
        query.setParameter("limit", criteria.limit());
        query.setParameter("offset", criteria.offset());

        List<Object[]> rows = query.getResultList();
        List<NearbyRestaurantRow> result = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            result.add(new NearbyRestaurantRow(
                    ((Number) row[0]).longValue(),
                    GeoUtils.round2(((Number) row[1]).doubleValue()),
                    ((Number) row[2]).intValue() == 1
            ));
        }
        return result;
    }

    @Override
    public long countNearbyRestaurants(NearbyRestaurantCriteria criteria) {

        String sql = "SELECT COUNT(*) FROM ("
                + innerSql(criteria)
                + ") t "
                + outerWhere(criteria);

        Query query = entityManager.createNativeQuery(sql);
        bind(query, criteria);

        return ((Number) query.getSingleResult()).longValue();
    }

    // ------------------------------------------------------------------

    private String innerSql(NearbyRestaurantCriteria c) {

        StringBuilder sql = new StringBuilder()
                .append("SELECT s.record_id AS restaurant_id, ")
                .append(DISTANCE_EXPR).append(" AS distance_km, ")
                .append(OPEN_NOW_EXPR).append(" AS open_now, ")
                .append("s.delivery_radius_km, s.rating, s.avg_preparation_minutes, ")
                .append("s.cost_for_two ")
                .append("FROM restaurant_master s ")
                .append("WHERE s.deleted = 0 AND s.active = 1 ")
                .append("AND s.status = '").append(RestaurantStatus.APPROVED.name()).append("' ")
                .append("AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL ")
                .append("AND s.latitude BETWEEN :minLat AND :maxLat ")
                .append("AND s.longitude BETWEEN :minLng AND :maxLng ");

        if (c.globalCategoryUuid() != null) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1 FROM restaurant_global_category_map m
                        JOIN restaurant_global_category g ON g.record_id = m.global_category_id
                        WHERE m.restaurant_id = s.record_id
                          AND g._uuid = :globalCategoryUuid
                          AND g.deleted = 0 AND g.active = 1)\s""");
        }

        if (c.searchPattern() != null) {
            sql.append("""
                    AND (LOWER(s.name_en) LIKE :search ESCAPE '!'
                         OR LOWER(s.name_fr) LIKE :search ESCAPE '!'
                         OR EXISTS (
                            SELECT 1 FROM restaurant_global_category_map m2
                            JOIN restaurant_global_category g2 ON g2.record_id = m2.global_category_id
                            WHERE m2.restaurant_id = s.record_id
                              AND g2.deleted = 0 AND g2.active = 1
                              AND (LOWER(g2.name_en) LIKE :search ESCAPE '!'
                                   OR LOWER(g2.name_fr) LIKE :search ESCAPE '!')))\s""");
        }

        if (c.vegOnly()) {
            sql.append("AND s.food_type = 'VEG' ");
        }

        if (c.minRating() != null) {
            sql.append("AND s.rating >= :minRating ");
        }

        if (c.maxCostForTwo() != null) {
            sql.append("AND s.cost_for_two IS NOT NULL AND s.cost_for_two <= :maxCostForTwo ");
        }

        return sql.toString();
    }

    private String outerWhere(NearbyRestaurantCriteria c) {
        String where = "WHERE t.distance_km <= :radiusKm "
                + "AND (t.delivery_radius_km IS NULL OR t.distance_km <= t.delivery_radius_km) ";

        if (c.openNowOnly()) {
            where += "AND t.open_now = 1 ";
        }
        return where;
    }

    /** Only whitelisted SQL fragments - never user input. Open restaurants always first. */
    private String orderBy(RestaurantSortBy sortBy) {
        String eta = "(COALESCE(t.avg_preparation_minutes, "
                + GeoUtils.DEFAULT_PREPARATION_MINUTES + ") + t.distance_km * "
                + GeoUtils.RIDE_MINUTES_PER_KM + ")";

        String order = switch (sortBy == null ? RestaurantSortBy.RELEVANCE : sortBy) {
            case DISTANCE, RELEVANCE -> "t.distance_km ASC";
            case RATING -> "t.rating IS NULL, t.rating DESC, t.distance_km ASC";
            case DELIVERY_TIME -> eta + " ASC, t.distance_km ASC";
            case COST_LOW_TO_HIGH -> "t.cost_for_two IS NULL, t.cost_for_two ASC, t.distance_km ASC";
            case COST_HIGH_TO_LOW -> "t.cost_for_two IS NULL, t.cost_for_two DESC, t.distance_km ASC";
        };

        // restaurant_id as final tie-breaker keeps paging stable
        return " ORDER BY t.open_now DESC, " + order + ", t.restaurant_id ASC";
    }

    private void bind(Query query, NearbyRestaurantCriteria c) {
        double[] box = GeoUtils.boundingBox(c.latitude(), c.longitude(), c.radiusKm());

        query.setParameter("lat", c.latitude());
        query.setParameter("lng", c.longitude());
        query.setParameter("now", c.now());
        query.setParameter("minLat", box[0]);
        query.setParameter("maxLat", box[1]);
        query.setParameter("minLng", box[2]);
        query.setParameter("maxLng", box[3]);
        query.setParameter("radiusKm", c.radiusKm());

        if (c.globalCategoryUuid() != null) {
            query.setParameter("globalCategoryUuid", c.globalCategoryUuid());
        }
        if (c.searchPattern() != null) {
            query.setParameter("search", c.searchPattern());
        }
        if (c.minRating() != null) {
            query.setParameter("minRating", c.minRating());
        }
        if (c.maxCostForTwo() != null) {
            query.setParameter("maxCostForTwo", c.maxCostForTwo());
        }
    }
}
