package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopMaster;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopMasterRepository
        extends JpaRepository<ShopMaster, Long>,
                JpaSpecificationExecutor<ShopMaster> {

    Optional<ShopMaster> findByUuid(String uuid);

    Optional<ShopMaster> findByUuidAndDeletedFalse(String uuid);

    Optional<ShopMaster> findByShopCodeIgnoreCaseAndDeletedFalse(
            String shopCode
    );

    List<ShopMaster> findByActiveTrueAndDeletedFalseOrderByNameEnAsc();

    @Query("""
        SELECT s FROM ShopMaster s
        WHERE s.owner.uuid = :ownerUuid
          AND s.deleted = false
        """)
    List<ShopMaster> findByOwnerUuid(
            @Param("ownerUuid") String ownerUuid
    );
}