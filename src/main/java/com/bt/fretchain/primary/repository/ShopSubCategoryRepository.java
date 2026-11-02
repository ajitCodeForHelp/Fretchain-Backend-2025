package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopSubCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopSubCategoryRepository
        extends JpaRepository<ShopSubCategory, Long>,
                JpaSpecificationExecutor<ShopSubCategory> {

    Optional<ShopSubCategory> findByUuidAndDeletedFalse(String uuid);

    @Query("""
        SELECT sc FROM ShopSubCategory sc
        WHERE sc.shop.uuid = :shopUuid
          AND sc.category.uuid = :categoryUuid
          AND sc.deleted = false
        ORDER BY sc.displayOrder ASC, sc.nameEn ASC
        """)
    List<ShopSubCategory> findByShopAndCategory(
            @Param("shopUuid") String shopUuid,
            @Param("categoryUuid") String categoryUuid
    );

    boolean existsByShop_RecordIdAndCategory_RecordIdAndNameEnIgnoreCaseAndDeletedFalse(
            Long shopId, Long categoryId, String nameEn
    );
}