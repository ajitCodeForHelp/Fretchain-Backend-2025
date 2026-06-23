package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreightOrderStatusLogRepository
        extends JpaRepository<FreightOrderStatusLog, Long> {
}