package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends
        JpaRepository<Category, Long>,
        JpaSpecificationExecutor<Category> {

    Optional<Category> findByUuid(String uuid);

    Optional<Category> findByUuidAndDeletedFalse(String uuid);

    Optional<Category> findByNameEnIgnoreCaseAndDeletedFalse(String nameEn);

    Optional<Category> findByNameFrIgnoreCaseAndDeletedFalse(String nameFr);

    List<Category> findByActiveTrueAndDeletedFalseOrderByNameEnAsc();
    List<Category> findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc();

    List<Category> findByActiveTrueAndDeletedFalseOrderByCreatedAtAsc();

}