package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.VendorMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface VendorMasterRepository extends JpaRepository<VendorMaster, Long>, JpaSpecificationExecutor<VendorMaster> {

    Optional<VendorMaster> findByUuidAndDeletedFalse(String uuid);

    Optional<VendorMaster> findByUser_RecordIdAndDeletedFalse(Long userId);

    Optional<VendorMaster> findByUser_UuidAndDeletedFalse(String userUuid);

    List<VendorMaster> findByActiveTrueAndDeletedFalseOrderByCompanyNameAsc();

    boolean existsByCompanyNameIgnoreCaseAndDeletedFalse(String companyName);

    boolean existsByCompanyNameIgnoreCaseAndDeletedFalseAndRecordIdNot(String companyName, Long recordId);
}
