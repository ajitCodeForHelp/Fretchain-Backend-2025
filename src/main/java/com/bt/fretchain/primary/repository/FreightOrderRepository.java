package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FreightOrderRepository extends JpaRepository<FreightOrder, Long>
        , JpaSpecificationExecutor<FreightOrder> {
    Optional<FreightOrder> findByUuidAndDeletedFalse(String uuid);


    Optional<FreightOrder> findByUuidAndCustomerAndDeletedFalse(
            String orderUuid,
            SystemUser customer
    );

    @Query("""
            SELECT f
            FROM FreightOrder f
            WHERE f.customer = :customer
            AND f.category.uuid = :categoryUuid
            AND (
                  (:subCategoryUuid IS NULL AND f.subCategory IS NULL)
                  OR f.subCategory.uuid = :subCategoryUuid
            )
            AND f.orderStatus = :orderStatus
            AND f.deleted = false
            ORDER BY f.modifiedAt DESC
            """)
    Optional<FreightOrder> findLatestDraft(
            SystemUser customer,
            String categoryUuid,
            String subCategoryUuid,
            OrderStatus orderStatus
    );


    @Query("""
                SELECT f
                FROM FreightOrder f
                WHERE f.customer = :customer
                  AND f.category.uuid = :categoryUuid
                  AND f.orderStatus = :orderStatus
                  AND f.deleted = false
                ORDER BY f.modifiedAt DESC
            """)
    List<FreightOrder> findLatestDraft(
            SystemUser customer,
            String categoryUuid,
            OrderStatus orderStatus,
            Pageable pageable
    );
}