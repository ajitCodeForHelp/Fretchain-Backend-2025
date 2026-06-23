package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreightOrderPricingRepository extends JpaRepository<FreightOrderPricing, Long> {

    Optional<FreightOrderPricing> findByFreightOrder(FreightOrder freightOrder);


}