package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends
        JpaRepository<Driver, Long>,
        JpaSpecificationExecutor<Driver> {

    Optional<Driver> findByUuidAndDeletedFalse(String uuid);

    Optional<Driver> findByMobileAndDeletedFalse(String mobile);

    Optional<Driver> findByUsernameAndDeletedFalse(String username);

    List<Driver> findByActiveTrueAndDeletedFalseOrderByFullNameAsc();
}