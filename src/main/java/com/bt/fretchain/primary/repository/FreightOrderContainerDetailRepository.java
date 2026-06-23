package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderContainerDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreightOrderContainerDetailRepository extends JpaRepository<FreightOrderContainerDetail, Long> {

    Optional<FreightOrderContainerDetail> findByFreightOrder(FreightOrder freightOrder);

    void deleteByFreightOrder(FreightOrder freightOrder);

}