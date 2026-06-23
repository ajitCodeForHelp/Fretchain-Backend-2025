package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CustomerFreightOrderDTO {

    @Getter
    @Setter
    public static class EstimateRequest {

        @NotBlank
        private String vehicleCategoryUuid;

        @NotNull
        private FreightOrderType orderType;

        @NotNull
        private Double distanceKm;

        @NotNull
        private Integer estimatedTimeMinutes;

        private Integer dropCount = 1;

        private BigDecimal cargoValue;

        private Boolean insurance = false;

        private Boolean manualLaborRequired = false;

        private Boolean forkliftNeeded = false;

        private Boolean craneNeeded = false;

        private Boolean trolleyNeeded = false;

        private Boolean dangerousGoods = false;

        private Boolean temperatureSensitive = false;
    }

    @Getter
    @Setter
    public static class EstimateResponse {

        private BigDecimal baseCharges;

        private BigDecimal baseFare;

        private BigDecimal extraKmFare;

        private BigDecimal multiDropCharges;

        private BigDecimal insuranceCharges;

        private BigDecimal helperCharges;

        private BigDecimal handlingCharges;

        private BigDecimal adminServiceCharge;

        private BigDecimal taxAmount;

        private BigDecimal subTotal;

        private BigDecimal totalAmount;
    }


    @Getter
    @Setter
    public static class CreateDraftRequest {

        @NotNull
        private FreightOrderType orderType;

        @NotBlank
        private String vehicleCategoryUuid;

        @NotNull
        private Double distanceKm;

        @NotNull
        private Integer estimatedTimeMinutes;

        @Valid
        @NotNull
        private AddressRequest pickupAddress;

        @Valid
        @NotNull
        private List<AddressRequest> dropAddresses;
    }


    @Getter
    @Setter
    public static class UpdateDraftRequest {

        private String vehicleCategoryUuid;

        private PickupType pickupType;

        private LocalDateTime pickupDateTime;

        private Double distanceKm;

        private Integer estimatedTimeMinutes;

        private String instructionForDriver;

        private AddressRequest pickupAddress;

        private List<AddressRequest> dropAddresses;

        private ContainerDetailRequest containerDetail;

        private BreakBulkDetailRequest breakBulkDetail;

        private List<DocumentRequest> documents;
    }

    @Getter
    @Setter
    public static class AddressRequest {

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
    public static class ContainerDetailRequest {

        private ContainerLoadType loadType;

        private Boolean dangerousGoods = false;

        private String natureOfGoods;

        private String shippingLine;

        private String shippingLineEmail;

        private Double cargoWeightKg;

        private Boolean insurance = false;

        private BigDecimal cargoValue;

        private Boolean returnEmptyContainer = false;

        private List<ContainerItemRequest> containerItems;
    }

    @Getter
    @Setter
    public static class ContainerItemRequest {

        private String containerNumber;

        private String sealNumber;
    }

    @Getter
    @Setter
    public static class BreakBulkDetailRequest {

        private String cargoType;

        private String otherCargoType;

        private String cargoDetails;

        private String natureOfGoods;

        private Double totalWeightKg;

        private Double totalVolumeCbm;

        private Integer numberOfPieces;

        private String dimensionsPerPiece;

        private Boolean fragile = false;

        private Boolean handlingRequired = false;

        private Boolean safetyRequired = false;

        private Boolean hazardousMaterial = false;

        private Boolean temperatureSensitive = false;

        private Boolean insuranceRequired = false;

        private Boolean forkliftNeeded = false;

        private Boolean craneNeeded = false;

        private Boolean trolleyNeeded = false;

        private Boolean manualLaborRequired = false;
    }

    @Getter
    @Setter
    public static class DocumentRequest {

        private FreightOrderDocumentType documentType;

        private String fileUrl;
    }

    @Getter
    @Setter
    public static class SaveDraftResponse {

        private String orderUuid;

        private String orderNumber;

        private BigDecimal totalAmount;
    }


    @Getter
    @Setter
    public static class OrderDetailResponse {

        private String orderUuid;
        private String orderNumber;

        private FreightOrderType orderType;
        private OrderStatus orderStatus;

        private PickupType pickupType;
        private LocalDateTime pickupDateTime;

        private Double distanceKm;
        private Integer estimatedTimeMinutes;

        private String instructionForDriver;

        private VehicleDetail vehicle;

        private AddressRequest pickupAddress;
        private List<AddressRequest> dropAddresses;

        private ContainerDetailRequest containerDetail;
        private BreakBulkDetailRequest breakBulkDetail;

        private List<DocumentRequest> documents;

        private EstimateResponse pricing;
    }

    @Getter
    @Setter
    public static class VehicleDetail {

        private String uuid;
        private String nameEn;
        private String nameFr;
        private String iconUrl;
        private Long maxWeightKg;
    }

    @Getter
    @Setter
    public static class PlaceOrderResponse {

        private String orderUuid;
        private String orderNumber;
        private OrderStatus orderStatus;
        private BigDecimal totalAmount;
    }
}

