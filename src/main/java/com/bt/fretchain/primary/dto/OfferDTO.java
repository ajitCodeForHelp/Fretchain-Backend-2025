package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.DriverAssignmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferDTO {

    /** What a driver sees for an order offer. */
    @Getter
    @Setter
    public static class View {
        private String assignmentUuid;
        /** FREIGHT | SHOP | FOOD */
        private String orderType;
        private String orderUuid;
        private String orderNumber;
        /** e.g. "Container", "Shop order", "Food order" */
        private String title;
        private String pickupName;
        private String pickupAddress;
        private Double pickupLat;
        private Double pickupLng;
        private String dropName;
        private String dropAddress;
        private Double dropLat;
        private Double dropLng;
        /** Driver's distance to the pickup. */
        private Double distanceKm;
        /** Pickup to drop distance. */
        private Double tripKm;
        private BigDecimal amount;
        private Integer itemCount;
        private String paymentMethod;
        private LocalDateTime expiresAt;
        private long secondsLeft;
        private String mode;
        private DriverAssignmentStatus status;

        // ---- accepted orders: where the order is and what the driver does next
        private String orderStatus;
        private String statusLabel;
        private String customerName;
        private String customerPhone;
        private java.util.List<Step> steps;
        private NextStep next;
    }

    @Getter
    @Setter
    public static class Step {
        private String key;
        private String label;
        private boolean done;
        private boolean current;
    }

    /** The single button the driver can press now. enabled=false shows the hint instead (e.g. waiting for the store). */
    @Getter
    @Setter
    public static class NextStep {
        private String key;
        private String label;
        private boolean enabled = true;
        private String hint;
    }

    @Getter
    @Setter
    public static class Reject {
        @jakarta.validation.constraints.Size(max = 500)
        private String reason;
    }
}
