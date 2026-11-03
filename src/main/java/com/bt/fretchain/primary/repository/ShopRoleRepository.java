package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ShopRoleRepository
        extends JpaRepository<ShopRole, Long>, JpaSpecificationExecutor<ShopRole> {

    Optional<ShopRole> findByUuidAndShop_RecordIdAndDeletedFalse(String uuid, Long shopId);

    Optional<ShopRole> findFirstByShop_RecordIdAndOwnerRoleTrueAndDeletedFalse(Long shopId);

    List<ShopRole> findByShop_RecordIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long shopId);

    boolean existsByShop_RecordIdAndNameIgnoreCaseAndDeletedFalse(Long shopId, String name);

    boolean existsByShop_RecordIdAndNameIgnoreCaseAndDeletedFalseAndRecordIdNot(
            Long shopId, String name, Long recordId);
}
