package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.StoreOrder;
import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long>, JpaSpecificationExecutor<StoreOrder> {

    Optional<StoreOrder> findByUuidAndDeletedFalse(String uuid);
}
