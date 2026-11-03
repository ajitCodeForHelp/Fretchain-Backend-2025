package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface RestaurantRoleRepository
        extends JpaRepository<RestaurantRole, Long>, JpaSpecificationExecutor<RestaurantRole> {

    Optional<RestaurantRole> findByUuidAndRestaurant_RecordIdAndDeletedFalse(String uuid, Long restaurantId);

    Optional<RestaurantRole> findFirstByRestaurant_RecordIdAndOwnerRoleTrueAndDeletedFalse(Long restaurantId);

    List<RestaurantRole> findByRestaurant_RecordIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long restaurantId);

    boolean existsByRestaurant_RecordIdAndNameIgnoreCaseAndDeletedFalse(Long restaurantId, String name);

    boolean existsByRestaurant_RecordIdAndNameIgnoreCaseAndDeletedFalseAndRecordIdNot(
            Long restaurantId, String name, Long recordId);
}
