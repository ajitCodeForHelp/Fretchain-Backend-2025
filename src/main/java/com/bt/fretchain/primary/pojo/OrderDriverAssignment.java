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

    /** Set for freight offers (null for shop / food orders). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_order_id")
    private FreightOrder freightOrder;

    /** Set for shop / food offers (null for freight orders). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id")
    private StoreOrder storeOrder;


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


    /** The offer stops being valid at this time (the driver can no longer accept). */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** AUTO (sent by the system) or MANUAL (picked by admin / fleet owner). */
    @Column(name = "offer_mode", length = 10)
    private String offerMode;

    /** 1, 2, 3... which round of offers this belongs to (auto mode). */
    @Column(name = "wave")
    private Integer wave;

    /** Driver's distance to the pickup when the offer was made. */
    @Column(name = "distance_km")
    private Double distanceKm;

    public boolean isForStoreOrder() {
        return storeOrder != null;
    }
}
