package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCoupon_RecordIdAndCustomer_RecordIdAndDeletedFalse(Long couponId, Long customerId);

    List<CouponUsage> findByStoreOrder_RecordIdAndDeletedFalse(Long storeOrderId);
}
