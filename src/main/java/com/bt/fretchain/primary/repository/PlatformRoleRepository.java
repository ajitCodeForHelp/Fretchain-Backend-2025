package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.PlatformRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PlatformRoleRepository
        extends JpaRepository<PlatformRole, Long>, JpaSpecificationExecutor<PlatformRole> {

    Optional<PlatformRole> findByUuidAndDeletedFalse(String uuid);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndRecordIdNot(String name, Long recordId);
}
