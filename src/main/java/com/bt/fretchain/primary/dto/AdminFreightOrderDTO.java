package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.FreightOrderType;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.PickupType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminFreightOrderDTO {

    @Getter
    @Setter
    public static class ListRequest extends BasePageRequest {

        private Filters filters;

        private DateFilter dateFilter;

        private SortRequest sort;
    }

    @Getter
    @Setter
    public static class Filters {

        private OrderStatus orderStatus;

        private FreightOrderType orderType;

        private String categoryUuid;

        private String subCategoryUuid;

        private String vehicleCategoryUuid;

        private PickupType pickupType;
    }

    @Getter
    @Setter
    public static class DateFilter {

        private LocalDate from;

        private LocalDate to;
    }

    @Getter
    @Setter
    public static class SortRequest {

        private String field;

        private String direction;
    }

    @Getter
    @Setter
    public static class ListResponse {

        private String orderUuid;

        private String orderNumber;

        private FreightOrderType orderType;

        private OrderStatus orderStatus;

        private PickupType pickupType;

        private LocalDateTime pickupDateTime;

        private String customerName;

        private String customerPhone;

        private String categoryNameEn;

        private String categoryNameFr;

        private String subCategoryNameEn;

        private String subCategoryNameFr;

        private String vehicleCategoryNameEn;

        private String vehicleCategoryNameFr;

        private Double distanceKm;

        private Integer estimatedTimeMinutes;

        private BigDecimal totalAmount;

        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    public static class DetailResponse {

        private String orderUuid;

        private String orderNumber;

        private FreightOrderType orderType;

        private OrderStatus orderStatus;

        private PickupType pickupType;

        private LocalDateTime pickupDateTime;

        private Double distanceKm;

        private Integer estimatedTimeMinutes;

        private String instructionForDriver;

        private BigDecimal totalAmount;

        private CustomerDetail customer;

        private VehicleCategoryDetail vehicleCategory;

        private CategoryDetail category;

        private CategoryDetail subCategory;

        private AddressDetail pickupAddress;

        private List<AddressDetail> dropAddresses;

        private CustomerFreightOrderDTO.ContainerDetailRequest containerDetail;

        private CustomerFreightOrderDTO.BreakBulkDetailRequest breakBulkDetail;

        private CustomerFreightOrderDTO.ParcelDetailRequest parcelDetail;

        private CustomerFreightOrderDTO.BulkMaterialDetailRequest bulkMaterialDetail;

        private CustomerFreightOrderDTO.EstimateResponse pricing;

        private List<CustomerFreightOrderDTO.DocumentRequest> documents;

        private List<StatusLogResponse> statusLogs;
    }

    @Getter
    @Setter
    public static class CustomerDetail {

        private String uuid;

        private String name;

        private String mobile;

        private String email;
    }

    @Getter
    @Setter
    public static class VehicleCategoryDetail {

        private String uuid;

        private String nameEn;

        private String nameFr;

        private String iconUrl;

        private Long maxWeightKg;
    }

    @Getter
    @Setter
    public static class CategoryDetail {

        private String uuid;

        private String nameEn;

        private String nameFr;
    }

    @Getter
    @Setter
    public static class AddressDetail {

        private String addressType;

        private String addressName;

        private String contactName;

        private String contactNumber;

        private String fullAddress;

        private String landmark;

        private Double latitude;

        private Double longitude;

        private Integer sequenceNo;
    }

    @Getter
    @Setter
    public static class StatusLogResponse {

        private OrderStatus orderStatus;

        private String remarks;

        private LocalDateTime statusTime;

        private String updatedByName;
    }
}