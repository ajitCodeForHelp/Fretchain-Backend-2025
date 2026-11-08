package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopItemRepository
        extends JpaRepository<ShopItem, Long>,
                JpaSpecificationExecutor<ShopItem> {

    Optional<ShopItem> findByUuidAndDeletedFalse(String uuid);

    /** Row lock: stock is checked and reduced while placing an order. */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<ShopItem> findWithLockByUuidAndDeletedFalse(String uuid);

    @Query("""
        SELECT i FROM ShopItem i
        WHERE i.shop.uuid = :shopUuid
          AND i.active = true
          AND i.deleted = false
          AND i.available = true
        ORDER BY i.displayOrder ASC, i.nameEn ASC
        """)
    List<ShopItem> findAvailableByShopUuid(
            @Param("shopUuid") String shopUuid
    );

    @Query("""
        SELECT i FROM ShopItem i
        WHERE i.shop.uuid = :shopUuid
          AND i.category.uuid = :categoryUuid
          AND i.deleted = false
        ORDER BY i.displayOrder ASC, i.nameEn ASC
        """)
    List<ShopItem> findByShopAndCategory(
            @Param("shopUuid") String shopUuid,
            @Param("categoryUuid") String categoryUuid
    );

    boolean existsByShop_RecordIdAndSkuIgnoreCaseAndDeletedFalse(
            Long shopId, String sku
    );

    /**
     * Customer-visible item counts of a shop, grouped by category and subcategory.
     * Row = [categoryId (Long), subCategoryId (Long or null), count (Long)].
     */
    @Query("""
        SELECT c.recordId, sc.recordId, COUNT(i)
        FROM ShopItem i
        JOIN i.category c
        LEFT JOIN i.subCategory sc
        WHERE i.shop.recordId = :shopId
          AND i.active = true
          AND i.deleted = false
          AND i.available = true
          AND c.active = true
          AND c.deleted = false
          AND (sc IS NULL OR (sc.active = true AND sc.deleted = false))
        GROUP BY c.recordId, sc.recordId
        """)
    List<Object[]> countVisibleItemsByCategory(@Param("shopId") Long shopId);
}
