package com.bt.fretchain.primary.serviceImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Drivers created before review statuses existed: give them VERIFIED / PENDING from the old "verified" flag. */
@Slf4j
@Component
public class DriverVerificationBackfill implements ApplicationRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int n = em.createNativeQuery(
                "update drivers set verification_status = case when verified = 1 then 'VERIFIED' else 'PENDING' end "
                        + "where verification_status is null").executeUpdate();
        if (n > 0) {
            log.info("Driver verification status set for {} existing drivers", n);
        }
    }
}
