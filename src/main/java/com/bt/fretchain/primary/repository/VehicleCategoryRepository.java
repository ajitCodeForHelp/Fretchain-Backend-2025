package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleCategoryRepository extends
        JpaRepository<VehicleCategory, Long>,
        JpaSpecificationExecutor<VehicleCategory> {

    Optional<VehicleCategory> findByUuid(String uuid);

    Optional<VehicleCategory> findByUuidAndDeletedFalse(String uuid);

    Optional<VehicleCategory> findByNameEnIgnoreCaseAndDeletedFalse(String nameEn);

    Optional<VehicleCategory> findByNameFrIgnoreCaseAndDeletedFalse(String nameFr);

    List<VehicleCategory> findByActiveTrueAndDeletedFalseOrderBySortOrderAsc();


    @Query("""
        SELECT vc
        FROM VehicleCategory vc
        WHERE vc.deleted = false
        AND vc.active = true
        AND vc.category.uuid = :categoryUuid
        ORDER BY vc.sortOrder ASC
        """)
    List<VehicleCategory> findByCategory(
            @Param("categoryUuid") String categoryUuid
    );

    @Query("""
        SELECT vc
        FROM VehicleCategory vc
        WHERE vc.deleted = false
        AND vc.active = true
        AND vc.category.uuid = :categoryUuid
        AND vc.subCategory.uuid = :subCategoryUuid
        ORDER BY vc.sortOrder ASC
        """)
    List<VehicleCategory> findByCategoryAndSubCategory(
            @Param("categoryUuid") String categoryUuid,
            @Param("subCategoryUuid") String subCategoryUuid
    );
}