package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantItemRepository
        extends JpaRepository<RestaurantItem, Long>,
                JpaSpecificationExecutor<RestaurantItem> {

    Optional<RestaurantItem> findByUuidAndDeletedFalse(String uuid);

    @Query("""
        SELECT i FROM RestaurantItem i
        WHERE i.restaurant.uuid = :restaurantUuid
          AND i.active = true
          AND i.deleted = false
          AND i.available = true
        ORDER BY i.displayOrder ASC, i.nameEn ASC
        """)
    List<RestaurantItem> findAvailableByRestaurantUuid(
            @Param("restaurantUuid") String restaurantUuid
    );

    @Query("""
        SELECT i FROM RestaurantItem i
        WHERE i.restaurant.uuid = :restaurantUuid
          AND i.category.uuid = :categoryUuid
          AND i.deleted = false
        ORDER BY i.displayOrder ASC, i.nameEn ASC
        """)
    List<RestaurantItem> findByRestaurantAndCategory(
            @Param("restaurantUuid") String restaurantUuid,
            @Param("categoryUuid") String categoryUuid
    );


    /**
     * Customer-visible item counts of a restaurant, grouped by category and subcategory.
     * Row = [categoryId (Long), subCategoryId (Long or null), count (Long)].
     */
    @Query("""
        SELECT c.recordId, sc.recordId, COUNT(i)
        FROM RestaurantItem i
        JOIN i.category c
        LEFT JOIN i.subCategory sc
        WHERE i.restaurant.recordId = :restaurantId
          AND i.active = true
          AND i.deleted = false
          AND i.available = true
          AND c.active = true
          AND c.deleted = false
          AND (sc IS NULL OR (sc.active = true AND sc.deleted = false))
        GROUP BY c.recordId, sc.recordId
        """)
    List<Object[]> countVisibleItemsByCategory(@Param("restaurantId") Long restaurantId);

    /** Full customer menu of a restaurant. */
    @Query("""
        SELECT i FROM RestaurantItem i
        JOIN FETCH i.category c
        LEFT JOIN FETCH i.subCategory sc
        WHERE i.restaurant.recordId = :restaurantId
          AND i.active = true
          AND i.deleted = false
          AND i.available = true
          AND c.active = true
          AND c.deleted = false
          AND (sc IS NULL OR (sc.active = true AND sc.deleted = false))
        ORDER BY i.featured DESC, i.displayOrder ASC, i.nameEn ASC
        """)
    List<RestaurantItem> findVisibleMenuItems(@Param("restaurantId") Long restaurantId);
}
