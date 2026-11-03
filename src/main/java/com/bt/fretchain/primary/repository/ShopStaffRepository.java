package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ShopStaffRepository
        extends JpaRepository<ShopStaff, Long>, JpaSpecificationExecutor<ShopStaff> {

    Optional<ShopStaff> findByUser_RecordIdAndDeletedFalse(Long userId);

    Optional<ShopStaff> findByUuidAndShop_RecordIdAndDeletedFalse(String uuid, Long shopId);

    Optional<ShopStaff> findFirstByShop_RecordIdAndOwnerTrueAndDeletedFalse(Long shopId);
}
