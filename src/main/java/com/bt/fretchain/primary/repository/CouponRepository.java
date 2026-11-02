package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CouponRepository extends
        JpaRepository<Coupon, Long>,
        JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByUuidAndDeletedFalse(String uuid);

    Optional<Coupon> findByCouponCodeIgnoreCaseAndDeletedFalse(
            String couponCode
    );
}