package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantSubCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantSubCategoryRepository
        extends JpaRepository<RestaurantSubCategory, Long>,
                JpaSpecificationExecutor<RestaurantSubCategory> {

    Optional<RestaurantSubCategory> findByUuidAndDeletedFalse(String uuid);

    @Query("""
        SELECT sc FROM RestaurantSubCategory sc
        WHERE sc.restaurant.uuid = :restaurantUuid
          AND sc.category.uuid = :categoryUuid
          AND sc.deleted = false
        ORDER BY sc.displayOrder ASC, sc.nameEn ASC
        """)
    List<RestaurantSubCategory> findByRestaurantAndCategory(
            @Param("restaurantUuid") String restaurantUuid,
            @Param("categoryUuid") String categoryUuid
    );

    boolean existsByRestaurant_RecordIdAndCategory_RecordIdAndNameEnIgnoreCaseAndDeletedFalse(
            Long restaurantId, Long categoryId, String nameEn
    );

    @Query("""
        SELECT sc FROM RestaurantSubCategory sc
        WHERE sc.restaurant.recordId = :restaurantId
          AND sc.active = true
          AND sc.deleted = false
          AND sc.category.active = true
          AND sc.category.deleted = false
        ORDER BY sc.displayOrder ASC, sc.nameEn ASC
        """)
    List<RestaurantSubCategory> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId);
}
