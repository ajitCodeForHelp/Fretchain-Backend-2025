package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.SystemPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemPricingRepository
        extends JpaRepository<SystemPricing, Long> {

    Optional<SystemPricing>
    findTopByOrderByRecordIdAsc();
}