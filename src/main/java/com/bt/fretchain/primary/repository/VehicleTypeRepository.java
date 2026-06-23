package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface VehicleTypeRepository extends
        JpaRepository<VehicleType, Long>,
        JpaSpecificationExecutor<VehicleType> {

    Optional<VehicleType> findByUuid(String uuid);

    Optional<VehicleType> findByUuidAndDeletedFalse(String uuid);

    Optional<VehicleType> findByNameEnIgnoreCaseAndDeletedFalse(String nameEn);

    Optional<VehicleType> findByNameFrIgnoreCaseAndDeletedFalse(String nameFr);

    List<VehicleType> findByActiveTrueAndDeletedFalseOrderBySortOrderAsc();
}