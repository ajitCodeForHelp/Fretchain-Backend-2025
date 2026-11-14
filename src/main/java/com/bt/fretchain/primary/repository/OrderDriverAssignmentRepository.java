package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.OrderDriverAssignment;
import com.bt.fretchain.primary.pojo.enums.DriverAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
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

    // ---- offers (freight or store order)

    List<OrderDriverAssignment> findByFreightOrderRecordIdAndDeletedFalse(Long orderId);

    List<OrderDriverAssignment> findByStoreOrderRecordIdAndDeletedFalse(Long orderId);

    List<OrderDriverAssignment> findByFreightOrderRecordIdAndAssignmentStatusAndDeletedFalse(Long orderId, DriverAssignmentStatus status);

    List<OrderDriverAssignment> findByStoreOrderRecordIdAndAssignmentStatusAndDeletedFalse(Long orderId, DriverAssignmentStatus status);

    /** A driver's open offers (not yet expired), newest first. */
    List<OrderDriverAssignment> findByDriverRecordIdAndAssignmentStatusAndExpiresAtAfterAndDeletedFalseOrderByAssignedAtDesc(
            Long driverId, DriverAssignmentStatus status, LocalDateTime now);

    /** Offers whose time ran out but nobody has marked them yet. */
    List<OrderDriverAssignment> findByAssignmentStatusAndExpiresAtBeforeAndDeletedFalse(DriverAssignmentStatus status, LocalDateTime now);

    /** A driver's current job: accepted offers. */
    List<OrderDriverAssignment> findByDriverRecordIdAndAssignmentStatusAndDeletedFalseOrderByRespondedAtDesc(
            Long driverId, DriverAssignmentStatus status);

    boolean existsByDriverRecordIdAndAssignmentStatusAndExpiresAtAfterAndDeletedFalse(Long driverId, DriverAssignmentStatus status, LocalDateTime now);
}
