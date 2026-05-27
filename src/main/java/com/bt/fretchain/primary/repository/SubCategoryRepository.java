package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SubCategoryRepository extends
        JpaRepository<SubCategory, Long>,
        JpaSpecificationExecutor<SubCategory> {

    Optional<SubCategory> findByUuid(String uuid);

    Optional<SubCategory> findByUuidAndDeletedFalse(String uuid);

    Optional<SubCategory>
    findByNameEnIgnoreCaseAndDeletedFalse(String nameEn);

    Optional<SubCategory>
    findByNameFrIgnoreCaseAndDeletedFalse(String nameFr);

    List<SubCategory>
    findByActiveTrueAndDeletedFalseOrderByNameEnAsc();
}