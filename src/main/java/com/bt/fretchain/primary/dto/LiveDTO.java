package com.bt.fretchain.primary.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Live map: where each driver (and the vehicle he drives) is right now, and what he is doing. */
public class LiveDTO {

    @Getter
    @Setter
    public static class Driver {
        private String uuid;
        private String name;
        private String mobile;
        private String photoUrl;
        private String vehicleNumber;
        private String vehicleType;
        /** The fleet owner the driver works for, empty for platform drivers. */
        private String vendorName;
        private Double lat;
        private Double lng;
        private LocalDateTime lastSeenAt;
        private Long secondsAgo;
        /** ON_ORDER | ONLINE | OFFLINE */
        private String state;
        /** Online or on an order, but no position for several minutes (phone off, no signal, app closed). */
        private boolean stale;
        private String orderUuid;
        /** FREIGHT | SHOP | FOOD */
        private String orderType;
        private String orderNumber;
        private String orderStatus;
        private String pickupAddress;
        private String dropAddress;
    }

    @Getter
    @Setter
    public static class Counts {
        private int total;
        private int onOrder;
        private int online;
        private int offline;
        private int noLocation;
        private int stale;
    }

    @Getter
    @Setter
    public static class Response {
        private List<Driver> items = new ArrayList<>();
        private Counts counts = new Counts();
        private LocalDateTime serverTime = LocalDateTime.now();
    }
}
