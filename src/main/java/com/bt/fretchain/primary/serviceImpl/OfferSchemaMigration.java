package com.bt.fretchain.primary.serviceImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Offers now belong to a freight order OR a store order. Hibernate adds the new columns itself but never
 * relaxes an existing NOT NULL, so older databases need freight_order_id to become nullable.
 */
@Slf4j
@Component
@Order(1)
public class OfferSchemaMigration implements ApplicationRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            em.createNativeQuery("alter table order_driver_assignments modify freight_order_id bigint null").executeUpdate();
        } catch (Exception e) {
            log.warn("order_driver_assignments.freight_order_id could not be made nullable (already done?): {}", e.getMessage());
        }
    }
}
