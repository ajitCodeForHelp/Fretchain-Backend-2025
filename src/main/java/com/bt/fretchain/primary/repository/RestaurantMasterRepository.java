package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantMaster;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RestaurantMasterRepository
        extends JpaRepository<RestaurantMaster, Long>,
                JpaSpecificationExecutor<RestaurantMaster>,
                RestaurantMasterRepositoryCustom {

    Optional<RestaurantMaster> findByUuid(String uuid);

    Optional<RestaurantMaster> findByUuidAndDeletedFalse(String uuid);

    Optional<RestaurantMaster> findByRestaurantCodeIgnoreCaseAndDeletedFalse(
            String restaurantCode
    );

    List<RestaurantMaster> findByActiveTrueAndDeletedFalseOrderByNameEnAsc();

    @Query("""
        SELECT s FROM RestaurantMaster s
        WHERE s.owner.uuid = :ownerUuid
          AND s.deleted = false
        """)
    List<RestaurantMaster> findByOwnerUuid(
            @Param("ownerUuid") String ownerUuid
    );

    /** Loads stores (ids from the nearby query) with their global categories in one go. */
    @Query("""
        SELECT DISTINCT s FROM RestaurantMaster s
        LEFT JOIN FETCH s.globalCategories
        WHERE s.recordId IN :ids
        """)
    List<RestaurantMaster> findAllWithGlobalCategoriesByRecordIdIn(
            @Param("ids") Collection<Long> ids
    );
}
