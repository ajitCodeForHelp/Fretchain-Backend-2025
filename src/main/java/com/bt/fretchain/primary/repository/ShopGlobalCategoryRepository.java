package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.ShopGlobalCategory;
import com.bt.fretchain.primary.pojo.enums.BusinessType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShopGlobalCategoryRepository
        extends JpaRepository<ShopGlobalCategory, Long>,
                JpaSpecificationExecutor<ShopGlobalCategory> {

    Optional<ShopGlobalCategory> findByUuidAndDeletedFalse(String uuid);

    List<ShopGlobalCategory> findByUuidInAndDeletedFalse(Collection<String> uuids);

    boolean existsByBusinessTypeAndNameEnIgnoreCaseAndDeletedFalse(
            BusinessType businessType, String nameEn);

    boolean existsByBusinessTypeAndNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
            BusinessType businessType, String nameEn, Long recordId);

    List<ShopGlobalCategory>
        findByBusinessTypeAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameEnAsc(
            BusinessType businessType);

    /** Customer search box: global categories whose name matches. */
    @Query("""
        SELECT g FROM ShopGlobalCategory g
        WHERE g.businessType = :businessType
          AND g.active = true
          AND g.deleted = false
          AND (LOWER(g.nameEn) LIKE :pattern ESCAPE '!'
               OR LOWER(g.nameFr) LIKE :pattern ESCAPE '!')
        ORDER BY g.displayOrder ASC, g.nameEn ASC
        """)
    List<ShopGlobalCategory> searchActive(
            @Param("businessType") BusinessType businessType,
            @Param("pattern") String pattern,
            Pageable pageable);
}
