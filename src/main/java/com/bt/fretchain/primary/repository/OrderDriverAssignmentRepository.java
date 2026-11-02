package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.OrderDriverAssignment;
import com.bt.fretchain.primary.pojo.enums.DriverAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderDriverAssignmentRepository
        extends JpaRepository<OrderDriverAssignment, Long> {

    Optional<OrderDriverAssignment>
    findByUuidAndDeletedFalse(String uuid);

    List<OrderDriverAssignment>
    findByFreightOrderRecordIdOrderByAssignedAtDesc(
            Long orderId
    );


    Optional<OrderDriverAssignment>
    findByFreightOrderRecordIdAndDriverRecordIdAndAssignmentStatus(
            Long orderId,
            Long driverId,
            String status
    );

    boolean existsByFreightOrderRecordIdAndAssignmentStatus(
            Long orderId,
            DriverAssignmentStatus status
    );
}