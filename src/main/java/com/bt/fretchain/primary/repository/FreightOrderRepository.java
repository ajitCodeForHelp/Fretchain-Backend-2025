package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreightOrderRepository extends JpaRepository<FreightOrder, Long> {
    Optional<FreightOrder> findByUuidAndDeletedFalse(String uuid);
}