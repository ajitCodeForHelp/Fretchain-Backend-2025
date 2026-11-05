package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<SystemUser> {
    Optional<SystemUser> findByEmailAndDeletedFalse(String email);

    Optional<SystemUser> findByMobileAndDeletedFalse(String mobile);

    Optional<SystemUser> findByEmailOrMobile(String email, String mobile);

    Optional<SystemUser> findByUuid(String uuid);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    boolean existsByUuid(String uuid);

    Optional<SystemUser> findByUuidAndDeletedFalse(
            String uuid
    );


    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndRecordIdNot(String email, Long recordId);

    boolean existsByMobileAndRecordIdNot(String mobile, Long recordId);

    java.util.List<SystemUser> findByUserTypeAndDeletedFalseOrderByFullNameAsc(
            com.bt.fretchain.primary.pojo.enums.UserTypeEnum userType);
}
