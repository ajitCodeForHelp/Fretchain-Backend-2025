package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RestaurantGlobalCategoryRepository
        extends JpaRepository<RestaurantGlobalCategory, Long>,
                JpaSpecificationExecutor<RestaurantGlobalCategory> {

    Optional<RestaurantGlobalCategory> findByUuidAndDeletedFalse(String uuid);

    List<RestaurantGlobalCategory> findByUuidInAndDeletedFalse(Collection<String> uuids);

    boolean existsByNameEnIgnoreCaseAndDeletedFalse(String nameEn);

    boolean existsByNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
            String nameEn, Long recordId);

    List<RestaurantGlobalCategory> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameEnAsc();

    /** Customer search box: global categories whose name matches. */
    @Query("""
        SELECT g FROM RestaurantGlobalCategory g
        WHERE g.active = true
          AND g.deleted = false
          AND (LOWER(g.nameEn) LIKE :pattern ESCAPE '!'
               OR LOWER(g.nameFr) LIKE :pattern ESCAPE '!')
        ORDER BY g.displayOrder ASC, g.nameEn ASC
        """)
    List<RestaurantGlobalCategory> searchActive(
            @Param("pattern") String pattern,
            Pageable pageable);
}
