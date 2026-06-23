package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderBreakBulkDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreightOrderBreakBulkDetailRepository extends JpaRepository<FreightOrderBreakBulkDetail, Long> {

    Optional<FreightOrderBreakBulkDetail> findByFreightOrder(FreightOrder freightOrder);

}