package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderParcelDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreightOrderParcelDetailRepository
        extends JpaRepository<FreightOrderParcelDetail, Long> {

    Optional<FreightOrderParcelDetail> findByFreightOrder(
            FreightOrder freightOrder
    );

}