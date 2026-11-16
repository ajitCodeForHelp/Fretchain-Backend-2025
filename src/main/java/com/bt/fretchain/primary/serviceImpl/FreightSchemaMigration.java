package com.bt.fretchain.primary.serviceImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * freight_orders.pickup_address_id points to freight_order_addresses. Older databases still carry a foreign key
 * from an earlier model that pointed it at "addresses", and Hibernate (ddl-auto=update) adds constraints but never
 * removes them. With that old key in place every save of a freight order fails with "Cannot add or update a child
 * row: a foreign key constraint fails". This drops any foreign key on that column that points anywhere else.
 */
@Slf4j
@Component
@Order(1)
public class FreightSchemaMigration implements ApplicationRunner {

    private static final String FIND_STALE_KEYS =
            "select distinct constraint_name from information_schema.key_column_usage "
                    + "where table_schema = database() and table_name = 'freight_orders' "
                    + "and column_name = 'pickup_address_id' and referenced_table_name is not null "
                    + "and referenced_table_name <> 'freight_order_addresses'";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> names = em.createNativeQuery(FIND_STALE_KEYS).getResultList();
            for (Object name : names) {
                String key = String.valueOf(name);
                if (!key.matches("[A-Za-z0-9_]+")) continue;   // never build SQL from an odd name
                em.createNativeQuery("alter table freight_orders drop foreign key `" + key + "`").executeUpdate();
                log.warn("Dropped the old foreign key {} on freight_orders.pickup_address_id", key);
            }
        } catch (Exception e) {
            log.warn("Could not check the foreign keys of freight_orders: {}", e.getMessage());
        }
    }
}
