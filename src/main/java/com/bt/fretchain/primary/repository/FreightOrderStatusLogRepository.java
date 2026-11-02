package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreightOrderStatusLogRepository
        extends JpaRepository<FreightOrderStatusLog, Long> {


    List<FreightOrderStatusLog> findByFreightOrderOrderByStatusTimeAsc(
            FreightOrder freightOrder
    );
}