package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ServiceRadius;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ServiceRadiusRepository extends
        JpaRepository<ServiceRadius, Long>,
        JpaSpecificationExecutor<ServiceRadius> {

    Optional<ServiceRadius> findByUuidAndDeletedFalse(String uuid);

    Optional<ServiceRadius> findByAreaNameIgnoreCaseAndDeletedFalse(
            String areaName
    );

    List<ServiceRadius> findByActiveTrueAndDeletedFalse();
}