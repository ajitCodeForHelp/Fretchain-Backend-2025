package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.DriverAssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_driver_assignments",
        indexes = {
                @Index(
                        name = "idx_assignment_order",
                        columnList = "freight_order_id"
                ),
                @Index(
                        name = "idx_assignment_driver",
                        columnList = "driver_id"
                ),
                @Index(
                        name = "idx_assignment_status",
                        columnList = "assignment_status"
                )
        }
)
@Getter
@Setter
public class OrderDriverAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "freight_order_id",
            nullable = false
    )
    private FreightOrder freightOrder;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "driver_id",
            nullable = false
    )
    private Driver driver;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "assignment_status",
            nullable = false,
            length = 30
    )
    private DriverAssignmentStatus assignmentStatus;


    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;


    @Column(name = "responded_at")
    private LocalDateTime respondedAt;


    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;


    @Column(name = "notification_sent")
    private Boolean notificationSent = false;


    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;
}