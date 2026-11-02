package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopCategoryRepository
        extends JpaRepository<ShopCategory, Long>,
                JpaSpecificationExecutor<ShopCategory> {

    Optional<ShopCategory> findByUuidAndDeletedFalse(String uuid);

    boolean existsByShop_RecordIdAndNameEnIgnoreCaseAndDeletedFalse(
            Long shopId, String nameEn
    );

    boolean existsByShop_RecordIdAndNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
            Long shopId, String nameEn, Long recordId
    );

    /** Shop sections linked to a global category (used when the shop's global categories change). */
    List<ShopCategory> findByShop_RecordIdAndGlobalCategoryIsNotNullAndDeletedFalse(
            Long shopId
    );

    boolean existsByShop_RecordIdAndNameFrIgnoreCaseAndDeletedFalse(
            Long shopId, String nameFr
    );

    @Query("""
        SELECT c FROM ShopCategory c
        WHERE c.shop.uuid = :shopUuid
          AND c.active = true
          AND c.deleted = false
        ORDER BY c.displayOrder ASC, c.nameEn ASC
        """)
    List<ShopCategory> findActiveByShopUuid(
            @Param("shopUuid") String shopUuid
    );

    @Query("""
        SELECT c FROM ShopCategory c
        LEFT JOIN FETCH c.globalCategory
        WHERE c.shop.recordId = :shopId
          AND c.active = true
          AND c.deleted = false
        ORDER BY c.displayOrder ASC, c.nameEn ASC
        """)
    List<ShopCategory> findActiveByShopId(@Param("shopId") Long shopId);
}
