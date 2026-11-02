package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderBulkMaterialDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreightOrderBulkMaterialDetailRepository
        extends JpaRepository<FreightOrderBulkMaterialDetail, Long> {

    Optional<FreightOrderBulkMaterialDetail> findByFreightOrder(
            FreightOrder freightOrder
    );
}