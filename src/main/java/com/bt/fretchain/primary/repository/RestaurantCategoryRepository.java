package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantCategoryRepository
        extends JpaRepository<RestaurantCategory, Long>,
                JpaSpecificationExecutor<RestaurantCategory> {

    Optional<RestaurantCategory> findByUuidAndDeletedFalse(String uuid);

    boolean existsByRestaurant_RecordIdAndNameEnIgnoreCaseAndDeletedFalse(
            Long restaurantId, String nameEn
    );

    boolean existsByRestaurant_RecordIdAndNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
            Long restaurantId, String nameEn, Long recordId
    );

    /** Restaurant sections linked to a global category (used when the restaurant's global categories change). */
    List<RestaurantCategory> findByRestaurant_RecordIdAndGlobalCategoryIsNotNullAndDeletedFalse(
            Long restaurantId
    );

    boolean existsByRestaurant_RecordIdAndNameFrIgnoreCaseAndDeletedFalse(
            Long restaurantId, String nameFr
    );

    @Query("""
        SELECT c FROM RestaurantCategory c
        WHERE c.restaurant.uuid = :restaurantUuid
          AND c.active = true
          AND c.deleted = false
        ORDER BY c.displayOrder ASC, c.nameEn ASC
        """)
    List<RestaurantCategory> findActiveByRestaurantUuid(
            @Param("restaurantUuid") String restaurantUuid
    );

    @Query("""
        SELECT c FROM RestaurantCategory c
        LEFT JOIN FETCH c.globalCategory
        WHERE c.restaurant.recordId = :restaurantId
          AND c.active = true
          AND c.deleted = false
        ORDER BY c.displayOrder ASC, c.nameEn ASC
        """)
    List<RestaurantCategory> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId);
}
