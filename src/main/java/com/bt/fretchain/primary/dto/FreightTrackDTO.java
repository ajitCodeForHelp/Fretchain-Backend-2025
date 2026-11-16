package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.FreightOrderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** What the customer sees of their freight orders: the list and the live tracking screen. */
public class FreightTrackDTO {

    @Getter
    @Setter
    public static class ListRequest {
        /** true = in progress, false = past (delivered / cancelled), null = all. Drafts are never listed. */
        private Boolean active;
        @Min(0)
        private Integer pageNumber = 0;
        @Min(1)
        @Max(50)
        private Integer pageSize = 10;
    }

    @Getter
    @Setter
    public static class Card {
        private String uuid;
        private String orderNumber;
        private FreightOrderType orderType;
        private String status;
        /** "Finding a driver", "On the way", "Delivered" ... */
        private String statusLabel;
        private boolean active;
        private String pickupAddress;
        private String dropAddress;
        private int dropCount;
        private String vehicleCategory;
        private BigDecimal totalAmount;
        private String driverName;
        private String driverMobile;
        private String driverVehicle;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    public static class Stop {
        private int sequence;
        private String name;
        private String address;
        private String contactName;
        private String contactPhone;
        private Double lat;
        private Double lng;
        /** Drop already unloaded. */
        private boolean done;
    }

    @Getter
    @Setter
    public static class Step {
        private String key;
        private String label;
        private boolean done;
        private boolean current;
    }

    @Getter
    @Setter
    public static class Event {
        private String status;
        private String label;
        private String note;
        private LocalDateTime at;
    }

    @Getter
    @Setter
    public static class Track extends Card {
        private List<Step> steps = new ArrayList<>();
        private Stop pickup;
        private List<Stop> drops = new ArrayList<>();
        private int currentDrop;
        private Double distanceKm;
        private String driverPhotoUrl;
        private List<Event> timeline = new ArrayList<>();
        /** The customer can still cancel (no driver on the way yet). */
        private boolean cancellable;
        /** Where the driver is now (only while the order is on the road) and when that was reported. */
        private Double driverLat;
        private Double driverLng;
        private java.time.LocalDateTime driverLocationAt;
        /** Pickup and delivery records (photos, condition, who signed); the signature drawing itself is not sent. */
        private List<ProofDTO.View> proofs = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class CancelRequest {
        @jakarta.validation.constraints.Size(max = 300)
        private String reason;
    }
}
