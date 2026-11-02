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
}