package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ServiceRadius;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRadiusRepository extends JpaRepository<ServiceRadius, Long> {
    List<ServiceRadius> findByActiveTrueAndDeletedFalse();

}