package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends
        JpaRepository<Vehicle, Long>,
        JpaSpecificationExecutor<Vehicle> {

    Optional<Vehicle> findByUuid(String uuid);

    Optional<Vehicle> findByUuidAndDeletedFalse(String uuid);

    Optional<Vehicle> findByVehicleNumberIgnoreCaseAndDeletedFalse(
            String vehicleNumber
    );

    Optional<Vehicle> findByRcNumberIgnoreCaseAndDeletedFalse(
            String rcNumber
    );

    List<Vehicle> findByActiveTrueAndDeletedFalseOrderByVehicleNumberAsc();

    List<Vehicle> findByVendor_UuidAndActiveTrueAndDeletedFalseOrderByVehicleNumberAsc(String vendorUuid);

    long countByVendor_UuidAndDeletedFalse(String vendorUuid);
}