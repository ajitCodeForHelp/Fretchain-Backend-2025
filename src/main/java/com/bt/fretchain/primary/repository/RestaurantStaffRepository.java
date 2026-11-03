package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.RestaurantStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RestaurantStaffRepository
        extends JpaRepository<RestaurantStaff, Long>, JpaSpecificationExecutor<RestaurantStaff> {

    Optional<RestaurantStaff> findByUser_RecordIdAndDeletedFalse(Long userId);

    Optional<RestaurantStaff> findByUuidAndRestaurant_RecordIdAndDeletedFalse(String uuid, Long restaurantId);

    Optional<RestaurantStaff> findFirstByRestaurant_RecordIdAndOwnerTrueAndDeletedFalse(Long restaurantId);
}
