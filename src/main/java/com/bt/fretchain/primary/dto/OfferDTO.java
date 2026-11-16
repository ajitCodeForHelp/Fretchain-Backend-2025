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
        /** Who to ask for at the pickup (freight). */
        private String pickupContactName;
        private String pickupContactPhone;
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
        /** Every drop point, in delivery order (freight can have several; shop / food has one). */
        private java.util.List<Drop> drops;
        private Integer currentDrop;
        private String orderStatus;
        private String statusLabel;
        private String customerName;
        private String customerPhone;
        private java.util.List<Step> steps;
        private NextStep next;

        // ---- everything the driver needs to know about the order, grouped by topic (differs per order type)
        private java.util.List<Section> sections;
        /** Note the customer left for the driver. */
        private String instructions;
        /** Cash the driver must collect on delivery (cash-on-delivery shop / food order, COD parcel), else null. */
        private BigDecimal cashToCollect;
        private Boolean paymentPaid;
        /** Freight only: what the driver must record at the step he is on (checks, photos, signature). */
        private ProofRequirement proof;
    }

    /** One block of the order detail screen: label / value rows and / or a list of items. */
    @Getter
    @Setter
    public static class Section {
        private String key;
        private String title;
        private java.util.List<Row> rows = new java.util.ArrayList<>();
        private java.util.List<Item> items = new java.util.ArrayList<>();
    }

    @Getter
    @Setter
    public static class Row {
        private String label;
        private String value;
        /** true = shown highlighted (dangerous goods, fragile, cash to collect ...). */
        private boolean flag;

        public Row() { }

        public Row(String label, String value, boolean flag) {
            this.label = label;
            this.value = value;
            this.flag = flag;
        }
    }

    @Getter
    @Setter
    public static class Item {
        private String name;
        private Integer quantity;
        private String note;
        private String imageUrl;
        /** A document or file the driver can open. */
        private String url;
    }

    /** What the driver has to record before the next step is allowed (freight orders). */
    @Getter
    @Setter
    public static class ProofRequirement {
        /** PICKUP or DROP. */
        private String stage;
        private Integer dropSequence;
        /** The goods check (description, quantity, condition) is needed at this stage. */
        private boolean needsChecklist;
        private int minPhotos;
        private boolean needsSignature;
        /** true once everything required has been saved. */
        private boolean complete;
        /** What is already saved, so the app can show it. */
        private ProofDTO.View saved;
    }

    @Getter
    @Setter
    public static class Drop {
        private int sequence;
        private String name;
        private String address;
        private Double lat;
        private Double lng;
        private String contactName;
        private String contactPhone;
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
