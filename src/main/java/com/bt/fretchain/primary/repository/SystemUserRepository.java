package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
    Optional<SystemUser> findByEmail(String email);

    Optional<SystemUser> findByMobile(String mobile);

    Optional<SystemUser> findByEmailOrMobile(String email, String mobile);

    Optional<SystemUser> findByUuid(String uuid);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    boolean existsByUuid(String uuid);


}