package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.CustomerFreightOrderDTO;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.AddressRole;
import com.bt.fretchain.primary.pojo.enums.FreightOrderType;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.PickupType;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.primary.service.CustomerFreightOrderService;
import com.bt.fretchain.primary.service.DistanceCalculationService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerFreightOrderServiceImpl implements CustomerFreightOrderService {

    private final FreightOrderRepository freightOrderRepository;

    private final FreightOrderPricingRepository freightOrderPricingRepository;

    private final FreightOrderContainerDetailRepository containerDetailRepository;

    private final FreightOrderContainerItemRepository containerItemRepository;

    private final FreightOrderBreakBulkDetailRepository breakBulkDetailRepository;

    private final FreightOrderDocumentRepository documentRepository;

    private final FreightOrderAddressRepository freightOrderAddressRepository;

    private final CategoryRepository categoryRepository;

    private final SubCategoryRepository subCategoryRepository;

    private final VehicleCategoryRepository vehicleCategoryRepository;


    private final SystemPricingRepository systemPricingRepository;

    private final DistanceCalculationService distanceCalculationService;

    private final FreightOrderStatusLogRepository freightOrderStatusLogRepository;

    private final FreightOrderBulkMaterialDetailRepository bulkMaterialDetailRepository;

    private final FreightOrderParcelDetailRepository parcelDetailRepository;

    private final OfferService offers;

    private final DispatchService dispatch;

    @Override
    public CustomerFreightOrderDTO.EstimateResponse estimate(
            CustomerFreightOrderDTO.EstimateRequest request
    ) {

        VehicleCategory vehicleCategory =
                vehicleCategoryRepository
                        .findByUuidAndDeletedFalse(request.getVehicleCategoryUuid())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("vehicle.category.notfound")
                        );

        SystemPricing systemPricing =
                systemPricingRepository
                        .findTopByOrderByRecordIdAsc()
                        .orElse(new SystemPricing());

        return calculateFare(
                vehicleCategory,
                systemPricing,
                request
        );
    }

    @Override
    @Transactional
    public CustomerFreightOrderDTO.SaveDraftResponse createDraft(
            CustomerFreightOrderDTO.CreateDraftRequest request,
            SystemUser customer
    ) {

        ValidationResult validationResult =
                validateCategorySelection(
                        request.getCategoryUuid(),
                        request.getSubCategoryUuid(),
                        request.getVehicleCategoryUuid()
                );

        validateServiceArea(
                request.getPickupAddress(),
                request.getDropAddresses()
        );

        FreightOrder order = new FreightOrder();

        order.setOrderType(request.getOrderType());
        order.setOrderStatus(OrderStatus.DRAFT);

        order.setCategory(validationResult.getCategory());
        order.setSubCategory(validationResult.getSubCategory());
        order.setVehicleCategory(validationResult.getVehicleCategory());

        order.setDistanceKm(request.getDistanceKm());
        order.setEstimatedTimeMinutes(request.getEstimatedTimeMinutes());

        order.setCustomer(customer);
        order.setCreatedBy(customer.getRecordId());
        order.setModifiedBy(customer.getRecordId());

        FreightOrder savedOrder =
                freightOrderRepository.save(order);

        saveOrderAddresses(
                savedOrder,
                request.getPickupAddress(),
                request.getDropAddresses()
        );

        savedOrder.setOrderNumber(
                "FC-" + savedOrder.getRecordId()
        );
        savedOrder.setModifiedBy(customer.getRecordId());

        savedOrder =
                freightOrderRepository.save(savedOrder);

        CustomerFreightOrderDTO.SaveDraftResponse response =
                new CustomerFreightOrderDTO.SaveDraftResponse();

        response.setOrderUuid(savedOrder.getUuid());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setTotalAmount(BigDecimal.ZERO);

        return response;
    }


    @Override
    @Transactional
    public CustomerFreightOrderDTO.SaveDraftResponse updateDraft(
            String orderUuid,
            CustomerFreightOrderDTO.UpdateDraftRequest request,
            SystemUser customer
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndCustomerAndDeletedFalse(
                                orderUuid,
                                customer
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException("order.notfound")
                        );

        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new BadRequestException("Only draft order can be updated");
        }

        if (request.getCategoryUuid() != null
                || request.getSubCategoryUuid() != null
                || request.getVehicleCategoryUuid() != null) {

            String categoryUuid =
                    request.getCategoryUuid() != null
                            ? request.getCategoryUuid()
                            : order.getCategory().getUuid();

            String subCategoryUuid =
                    request.getSubCategoryUuid() != null
                            ? request.getSubCategoryUuid()
                            : order.getSubCategory() != null
                            ? order.getSubCategory().getUuid()
                            : null;

            String vehicleCategoryUuid =
                    request.getVehicleCategoryUuid() != null
                            ? request.getVehicleCategoryUuid()
                            : order.getVehicleCategory().getUuid();

            ValidationResult validationResult =
                    validateCategorySelection(
                            categoryUuid,
                            subCategoryUuid,
                            vehicleCategoryUuid
                    );

            order.setCategory(validationResult.getCategory());
            order.setSubCategory(validationResult.getSubCategory());
            order.setVehicleCategory(validationResult.getVehicleCategory());
        }

        if (request.getPickupType() != null) {
            order.setPickupType(request.getPickupType());

            if (request.getPickupType() == PickupType.NOW) {
                order.setPickupDateTime(LocalDateTime.now());
            } else {
                order.setPickupDateTime(request.getPickupDateTime());
            }
        }

        if (request.getInstructionForDriver() != null) {
            order.setInstructionForDriver(request.getInstructionForDriver());
        }

        if (request.getPickupAddress() != null
                && request.getDropAddresses() != null
                && !request.getDropAddresses().isEmpty()) {

            validateServiceArea(
                    request.getPickupAddress(),
                    request.getDropAddresses()
            );

            freightOrderAddressRepository.deleteByFreightOrder(order);

            saveOrderAddresses(
                    order,
                    request.getPickupAddress(),
                    request.getDropAddresses()
            );

            if (request.getDistanceKm() != null) {
                order.setDistanceKm(request.getDistanceKm());
            }

            if (request.getEstimatedTimeMinutes() != null) {
                order.setEstimatedTimeMinutes(
                        request.getEstimatedTimeMinutes()
                );
            }
        }

        order.setModifiedBy(customer.getRecordId());

        FreightOrder savedOrder =
                freightOrderRepository.save(order);
        switch (savedOrder.getOrderType()) {

            case CONTAINER -> {

                if (request.getContainerDetail() != null) {

                    deleteOldContainerDetails(savedOrder);

                    saveContainerDetails(
                            savedOrder,
                            request.getContainerDetail()
                    );
                }
            }
//           TODO DO FOR OTHER TYPE ALSO

//            case CONTAINER -> {
//                if (request.getContainerDetail() != null) {
//                    saveOrUpdateContainerDetails(
//                            savedOrder,
//                            request.getContainerDetail()
//                    );
//                }
//            }

            case BREAK_BULK -> {

                if (request.getBreakBulkDetail() != null) {

                    deleteOldBreakBulkDetails(savedOrder);

                    saveBreakBulkDetails(
                            savedOrder,
                            request.getBreakBulkDetail()
                    );
                }
            }

            case PARCEL -> {

                if (request.getParcelDetail() != null) {

                    deleteOldParcelDetails(savedOrder);

                    saveParcelDetails(
                            savedOrder,
                            request.getParcelDetail()
                    );
                }
            }

            case BULK_MATERIAL -> {

                if (request.getBulkMaterialDetail() != null) {

                    deleteOldBulkMaterialDetails(savedOrder);

                    saveBulkMaterialDetails(
                            savedOrder,
                            request.getBulkMaterialDetail()
                    );
                }
            }
        }

        if (request.getDocuments() != null) {
            documentRepository.deleteByFreightOrder(savedOrder);
            saveDocuments(savedOrder, request.getDocuments());
        }

        CustomerFreightOrderDTO.EstimateRequest estimateRequest =
                buildEstimateRequest(savedOrder, request);

        CustomerFreightOrderDTO.EstimateResponse estimateResponse =
                calculateFare(
                        savedOrder.getVehicleCategory(),
                        systemPricingRepository
                                .findTopByOrderByRecordIdAsc()
                                .orElse(new SystemPricing()),
                        estimateRequest
                );

        saveOrUpdatePricing(savedOrder, estimateResponse);

        savedOrder.setTotalAmount(estimateResponse.getTotalAmount());
        savedOrder.setModifiedBy(customer.getRecordId());

        freightOrderRepository.save(savedOrder);

        CustomerFreightOrderDTO.SaveDraftResponse response =
                new CustomerFreightOrderDTO.SaveDraftResponse();

        response.setOrderUuid(savedOrder.getUuid());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setTotalAmount(savedOrder.getTotalAmount());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerFreightOrderDTO.OrderDetailResponse orderDetail(
            String orderUuid , SystemUser customer
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndCustomerAndDeletedFalse(
                                orderUuid,
                                customer
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException("order.notfound")
                        );

        CustomerFreightOrderDTO.OrderDetailResponse response =
                new CustomerFreightOrderDTO.OrderDetailResponse();

        response.setOrderUuid(order.getUuid());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderType(order.getOrderType());
        response.setOrderStatus(order.getOrderStatus());
        response.setPickupType(order.getPickupType());
        response.setPickupDateTime(order.getPickupDateTime());
        response.setDistanceKm(order.getDistanceKm());
        response.setEstimatedTimeMinutes(order.getEstimatedTimeMinutes());
        response.setInstructionForDriver(order.getInstructionForDriver());

        VehicleCategory vc = order.getVehicleCategory();

        CustomerFreightOrderDTO.VehicleDetail vehicle =
                new CustomerFreightOrderDTO.VehicleDetail();

        vehicle.setUuid(vc.getUuid());
        vehicle.setNameEn(vc.getNameEn());
        vehicle.setNameFr(vc.getNameFr());
        vehicle.setIconUrl(vc.getIconUrl());
        vehicle.setMaxWeightKg(vc.getMaxWeightKg());

        response.setVehicle(vehicle);

        if (order.getCategory() != null) {

            CustomerFreightOrderDTO.CategoryDetail category =
                    new CustomerFreightOrderDTO.CategoryDetail();

            category.setUuid(order.getCategory().getUuid());
            category.setNameEn(order.getCategory().getNameEn());
            category.setNameFr(order.getCategory().getNameFr());

            response.setCategory(category);
        }

        if (order.getSubCategory() != null) {

            CustomerFreightOrderDTO.CategoryDetail subCategory =
                    new CustomerFreightOrderDTO.CategoryDetail();

            subCategory.setUuid(order.getSubCategory().getUuid());
            subCategory.setNameEn(order.getSubCategory().getNameEn());
            subCategory.setNameFr(order.getSubCategory().getNameFr());

            response.setSubCategory(subCategory);
        }


        mapOrderAddresses(order, response);
        mapOrderPricing(order, response);
        mapOrderDocuments(order, response);
        switch (order.getOrderType()) {

            case CONTAINER -> mapContainerDetail(order, response);

            case BREAK_BULK -> mapBreakBulkDetail(order, response);

            case PARCEL -> mapParcelDetail(order, response);

            case BULK_MATERIAL -> mapBulkMaterialDetail(order, response);
        }
        return response;
    }


    @Override
    @Transactional
    public CustomerFreightOrderDTO.PlaceOrderResponse placeOrder(
            String orderUuid , SystemUser customer
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndCustomerAndDeletedFalse(
                                orderUuid,
                                customer
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException("order.notfound")
                        );

        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new BadRequestException("Only draft order can be placed");
        }

        validateOrderBeforePlace(order);

        order.setOrderStatus(OrderStatus.PLACED);

        FreightOrder savedOrder =
                freightOrderRepository.save(order);

        saveStatusLog(
                savedOrder,
                OrderStatus.PLACED,
                "Order placed by customer"
        );

        // AUTO dispatch: ask the nearest drivers with the customer's vehicle type right away
        if (dispatch.settings().getFreightMode() == com.bt.fretchain.primary.pojo.enums.AssignmentMode.AUTO) {
            offers.dispatchNext(savedOrder);
        }

        CustomerFreightOrderDTO.PlaceOrderResponse response =
                new CustomerFreightOrderDTO.PlaceOrderResponse();

        response.setOrderUuid(savedOrder.getUuid());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setTotalAmount(savedOrder.getTotalAmount());

        return response;
    }


    @Override
    @Transactional(readOnly = true)
    public CustomerFreightOrderDTO.OrderDetailResponse getDraftOrder(
            SystemUser customer,
            String categoryUuid,
            String subCategoryUuid
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findLatestDraft(
                                customer,
                                categoryUuid,
                                OrderStatus.DRAFT,
                                PageRequest.of(0, 1)
                        )
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException("draft.order.notfound")
                        );

        return orderDetail(order.getUuid(), customer);
    }

    private void deleteOldParcelDetails(
            FreightOrder order
    ) {

        parcelDetailRepository
                .findByFreightOrder(order)
                .ifPresent(parcelDetailRepository::delete);
        parcelDetailRepository.flush();
    }


    private void deleteOldBulkMaterialDetails(
            FreightOrder order
    ) {

        bulkMaterialDetailRepository
                .findByFreightOrder(order)
                .ifPresent(bulkMaterialDetailRepository::delete);
        bulkMaterialDetailRepository.flush();
    }

    private void saveParcelDetails(
            FreightOrder order,
            CustomerFreightOrderDTO.ParcelDetailRequest request
    ) {

        FreightOrderParcelDetail detail =
                new FreightOrderParcelDetail();

        detail.setFreightOrder(order);

        detail.setPackageType(request.getPackageType());
        detail.setNumberOfPackages(request.getNumberOfPackages());
        detail.setWeightKg(request.getWeightKg());
        detail.setFragile(request.getFragile());

        detail.setCodAmount(request.getCodAmount());
        detail.setPackageValue(request.getPackageValue());

        detail.setPackagingType(request.getPackagingType());

        detail.setExpressDelivery(request.getExpressDelivery());
        detail.setSameDay(request.getSameDay());

        detail.setScheduledDeliveryTime(
                request.getScheduledDeliveryTime()
        );

        parcelDetailRepository.save(detail);
    }


    private void saveBulkMaterialDetails(
            FreightOrder order,
            CustomerFreightOrderDTO.BulkMaterialDetailRequest request
    ) {

        FreightOrderBulkMaterialDetail detail =
                new FreightOrderBulkMaterialDetail();

        detail.setFreightOrder(order);

        detail.setMaterialType(request.getMaterialType());
        detail.setOtherMaterialType(request.getOtherMaterialType());

        detail.setQuantityUnit(request.getQuantityUnit());
        detail.setQuantity(request.getQuantity());

        detail.setEstimatedWeightKg(
                request.getEstimatedWeightKg()
        );

        detail.setLoadingSite(request.getLoadingSite());

        detail.setSiteAccessibility(
                request.getSiteAccessibility()
        );

        detail.setDeliverySite(request.getDeliverySite());

        detail.setDeliveryTimeWindow(
                request.getDeliveryTimeWindow()
        );

        detail.setDeliveryInstructions(
                request.getDeliveryInstructions()
        );

        detail.setNumberOfTrucksNeeded(
                request.getNumberOfTrucksNeeded()
        );

        detail.setEstimatedNumberOfTrips(
                request.getEstimatedNumberOfTrips()
        );

        bulkMaterialDetailRepository.save(detail);
    }
    private ValidationResult validateCategorySelection(
            String categoryUuid,
            String subCategoryUuid,
            String vehicleCategoryUuid
    ) {

        Category category =
                categoryRepository
                        .findByUuidAndDeletedFalse(categoryUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("category.notfound")
                        );

        SubCategory subCategory = null;

        if (subCategoryUuid != null && !subCategoryUuid.isBlank()) {

            subCategory =
                    subCategoryRepository
                            .findByUuidAndDeletedFalse(subCategoryUuid)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("subCategory.notfound")
                            );

            if (!subCategory.getCategory().getRecordId()
                    .equals(category.getRecordId())) {

                throw new BadRequestException("subCategory.invalid");
            }
        }

        VehicleCategory vehicleCategory =
                vehicleCategoryRepository
                        .findByUuidAndDeletedFalse(vehicleCategoryUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("vehicle.category.notfound")
                        );
// TODO RECHECK

//        if (!vehicleCategory.getCategory().getRecordId()
//                .equals(category.getRecordId())) {
//
//            throw new BadRequestException("vehicle.category.invalid");
//        }

//        if (subCategory != null) {
//
//            if (vehicleCategory.getSubCategory() == null
//                    || !vehicleCategory.getSubCategory()
//                    .getRecordId()
//                    .equals(subCategory.getRecordId())) {
//
//                throw new BadRequestException("vehicle.category.invalid");
//            }
//        }

        return new ValidationResult(
                category,
                subCategory,
                vehicleCategory
        );
    }


    @Getter
    @AllArgsConstructor
    private static class ValidationResult {

        private Category category;

        private SubCategory subCategory;

        private VehicleCategory vehicleCategory;
    }

    private void validateOrderBeforePlace(
            FreightOrder order
    ) {

        if (order.getVehicleCategory() == null) {
            throw new BadRequestException("Vehicle category is required");
        }

        if (order.getDistanceKm() == null
                || order.getEstimatedTimeMinutes() == null) {
            throw new BadRequestException("Distance and time are required");
        }

        long pickupCount =
                freightOrderAddressRepository
                        .findByFreightOrderAndAddressRole(
                                order,
                                AddressRole.PICKUP
                        )
                        .size();

        long dropCount =
                freightOrderAddressRepository
                        .findByFreightOrderAndAddressRole(
                                order,
                                AddressRole.DROP
                        )
                        .size();

        if (pickupCount == 0 || dropCount == 0) {
            throw new BadRequestException("Pickup and drop address are required");
        }

        boolean hasPricing =
                freightOrderPricingRepository
                        .findByFreightOrder(order)
                        .isPresent();

        if (!hasPricing || order.getTotalAmount() == null) {
            throw new BadRequestException("Pricing is required");
        }

        if (order.getOrderType() == FreightOrderType.CONTAINER) {

            boolean hasContainerDetail =
                    containerDetailRepository
                            .findByFreightOrder(order)
                            .isPresent();

            if (!hasContainerDetail) {
                throw new BadRequestException("Container details are required");
            }
        }

        if (order.getOrderType() == FreightOrderType.BREAK_BULK) {

            boolean hasBreakBulkDetail =
                    breakBulkDetailRepository
                            .findByFreightOrder(order)
                            .isPresent();

            if (!hasBreakBulkDetail) {
                throw new BadRequestException("BreakBulk details are required");
            }
        }
    }


    private void saveStatusLog(
            FreightOrder order,
            OrderStatus orderStatus,
            String remarks
    ) {

        FreightOrderStatusLog log =
                new FreightOrderStatusLog();

        log.setFreightOrder(order);
        log.setOrderStatus(orderStatus);
        log.setRemarks(remarks);
        log.setStatusTime(LocalDateTime.now());

        freightOrderStatusLogRepository.save(log);
    }
    private void mapOrderAddresses(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        List<FreightOrderAddress> addresses =
                freightOrderAddressRepository
                        .findByFreightOrderOrderBySequenceNoAsc(order);

        List<CustomerFreightOrderDTO.AddressRequest> drops =
                new ArrayList<>();

        for (FreightOrderAddress address : addresses) {

            CustomerFreightOrderDTO.AddressRequest dto =
                    new CustomerFreightOrderDTO.AddressRequest();

            dto.setAddressType(address.getAddressType());
            dto.setAddressName(address.getAddressName());
            dto.setContactName(address.getContactName());
            dto.setContactNumber(address.getContactNumber());
            dto.setFullAddress(address.getFullAddress());
            dto.setLandmark(address.getLandmark());
            dto.setSequenceNo(address.getSequenceNo());
            dto.setLatitude(address.getLatitude());
            dto.setLongitude(address.getLongitude());

            if (address.getAddressRole() == AddressRole.PICKUP) {
                response.setPickupAddress(dto);
            } else {
                drops.add(dto);
            }
        }


        response.setDropAddresses(drops);
    }

    private void mapOrderPricing(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        freightOrderPricingRepository
                .findByFreightOrder(order)
                .ifPresent(pricing -> {

                    CustomerFreightOrderDTO.EstimateResponse dto =
                            new CustomerFreightOrderDTO.EstimateResponse();

                    dto.setBaseCharges(pricing.getBaseCharges());
                    dto.setBaseFare(pricing.getBaseFare());
                    dto.setExtraKmFare(pricing.getExtraKmFare());
                    dto.setMultiDropCharges(pricing.getMultiDropCharges());
                    dto.setInsuranceCharges(pricing.getInsuranceCharges());
                    dto.setHelperCharges(pricing.getHelperCharges());
                    dto.setTaxAmount(pricing.getTaxAmount());
                    dto.setSubTotal(pricing.getSubTotal());
                    dto.setTotalAmount(pricing.getTotalAmount());

                    response.setPricing(dto);
                });
    }



    private void mapOrderDocuments(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        List<CustomerFreightOrderDTO.DocumentRequest> documents =
                documentRepository
                        .findByFreightOrder(order)
                        .stream()
                        .map(doc -> {
                            CustomerFreightOrderDTO.DocumentRequest dto =
                                    new CustomerFreightOrderDTO.DocumentRequest();

                            dto.setDocumentType(doc.getDocumentType());
                            dto.setFileUrl(doc.getFileUrl());

                            return dto;
                        })
                        .toList();

        response.setDocuments(documents);
    }


    private void mapContainerDetail(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        containerDetailRepository
                .findByFreightOrder(order)
                .ifPresent(detail -> {

                    CustomerFreightOrderDTO.ContainerDetailRequest dto =
                            new CustomerFreightOrderDTO.ContainerDetailRequest();

                    dto.setLoadType(detail.getLoadType());
                    dto.setDangerousGoods(detail.getDangerousGoods());
                    dto.setNatureOfGoods(detail.getNatureOfGoods());
                    dto.setShippingLine(detail.getShippingLine());
                    dto.setShippingLineEmail(detail.getShippingLineEmail());
                    dto.setCargoWeightKg(detail.getCargoWeightKg());
                    dto.setInsurance(detail.getInsurance());
                    dto.setCargoValue(detail.getCargoValue());
                    dto.setReturnEmptyContainer(detail.getReturnEmptyContainer());

                    List<CustomerFreightOrderDTO.ContainerItemRequest> items =
                            containerItemRepository
                                    .findByContainerDetail(detail)
                                    .stream()
                                    .map(item -> {
                                        CustomerFreightOrderDTO.ContainerItemRequest itemDto =
                                                new CustomerFreightOrderDTO.ContainerItemRequest();

                                        itemDto.setContainerNumber(item.getContainerNumber());
                                        itemDto.setSealNumber(item.getSealNumber());

                                        return itemDto;
                                    })
                                    .toList();

                    dto.setContainerItems(items);

                    response.setContainerDetail(dto);
                });
    }

    private void mapBreakBulkDetail(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        breakBulkDetailRepository
                .findByFreightOrder(order)
                .ifPresent(detail -> {

                    CustomerFreightOrderDTO.BreakBulkDetailRequest dto =
                            new CustomerFreightOrderDTO.BreakBulkDetailRequest();

                    dto.setCargoType(detail.getCargoType());
                    dto.setOtherCargoType(detail.getOtherCargoType());
                    dto.setCargoDetails(detail.getCargoDetails());
                    dto.setNatureOfGoods(detail.getNatureOfGoods());
                    dto.setTotalWeightKg(detail.getTotalWeightKg());
                    dto.setTotalVolumeCbm(detail.getTotalVolumeCbm());
                    dto.setNumberOfPieces(detail.getNumberOfPieces());
                    dto.setDimensionsPerPiece(detail.getDimensionsPerPiece());
                    dto.setFragile(detail.getFragile());
                    dto.setHandlingRequired(detail.getHandlingRequired());
                    dto.setSafetyRequired(detail.getSafetyRequired());
                    dto.setHazardousMaterial(detail.getHazardousMaterial());
                    dto.setTemperatureSensitive(detail.getTemperatureSensitive());
                    dto.setInsuranceRequired(detail.getInsuranceRequired());
                    dto.setForkliftNeeded(detail.getForkliftNeeded());
                    dto.setCraneNeeded(detail.getCraneNeeded());
                    dto.setTrolleyNeeded(detail.getTrolleyNeeded());
                    dto.setManualLaborRequired(detail.getManualLaborRequired());

                    response.setBreakBulkDetail(dto);
                });
    }

    private void mapParcelDetail(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        parcelDetailRepository
                .findByFreightOrder(order)
                .ifPresent(detail -> {

                    CustomerFreightOrderDTO.ParcelDetailRequest dto =
                            new CustomerFreightOrderDTO.ParcelDetailRequest();

                    dto.setPackageType(detail.getPackageType());
                    dto.setNumberOfPackages(detail.getNumberOfPackages());
                    dto.setWeightKg(detail.getWeightKg());
                    dto.setFragile(detail.getFragile());
                    dto.setCodAmount(detail.getCodAmount());
                    dto.setPackageValue(detail.getPackageValue());
                    dto.setPackagingType(detail.getPackagingType());
                    dto.setExpressDelivery(detail.getExpressDelivery());
                    dto.setSameDay(detail.getSameDay());
                    dto.setScheduledDeliveryTime(detail.getScheduledDeliveryTime());

                    response.setParcelDetail(dto);
                });
    }


    private void mapBulkMaterialDetail(
            FreightOrder order,
            CustomerFreightOrderDTO.OrderDetailResponse response
    ) {

        bulkMaterialDetailRepository
                .findByFreightOrder(order)
                .ifPresent(detail -> {

                    CustomerFreightOrderDTO.BulkMaterialDetailRequest dto =
                            new CustomerFreightOrderDTO.BulkMaterialDetailRequest();

                    dto.setMaterialType(detail.getMaterialType());
                    dto.setOtherMaterialType(detail.getOtherMaterialType());
                    dto.setQuantityUnit(detail.getQuantityUnit());
                    dto.setQuantity(detail.getQuantity());
                    dto.setEstimatedWeightKg(detail.getEstimatedWeightKg());
                    dto.setLoadingSite(detail.getLoadingSite());
                    dto.setSiteAccessibility(detail.getSiteAccessibility());
                    dto.setDeliverySite(detail.getDeliverySite());
                    dto.setDeliveryTimeWindow(detail.getDeliveryTimeWindow());
                    dto.setDeliveryInstructions(detail.getDeliveryInstructions());
                    dto.setNumberOfTrucksNeeded(detail.getNumberOfTrucksNeeded());
                    dto.setEstimatedNumberOfTrips(detail.getEstimatedNumberOfTrips());

                    response.setBulkMaterialDetail(dto);
                });
    }
    private void saveOrderAddresses(
            FreightOrder order,
            CustomerFreightOrderDTO.AddressRequest pickupAddress,
            List<CustomerFreightOrderDTO.AddressRequest> dropAddresses
    ) {

        FreightOrderAddress pickup =
                toAddress(pickupAddress);

        pickup.setFreightOrder(order);
        pickup.setAddressRole(AddressRole.PICKUP);
        pickup.setSequenceNo(0);

        freightOrderAddressRepository.save(pickup);

        order.setPickupAddress(pickup);


        if (dropAddresses == null) {
            return;
        }

        int sequence = 1;

        for (CustomerFreightOrderDTO.AddressRequest drop : dropAddresses) {

            FreightOrderAddress dropAddress =
                    toAddress(drop);

            dropAddress.setFreightOrder(order);
            dropAddress.setAddressRole(AddressRole.DROP);
            dropAddress.setSequenceNo(sequence++);

            freightOrderAddressRepository.save(dropAddress);
        }
    }
    private void validateServiceArea(
            CustomerFreightOrderDTO.AddressRequest pickupAddress,
            List<CustomerFreightOrderDTO.AddressRequest> dropAddresses
    ) {

        boolean pickupAvailable =
                distanceCalculationService.isLocationInsideServiceRadius(
                        pickupAddress.getLatitude(),
                        pickupAddress.getLongitude()
                );

        if (!pickupAvailable) {
            throw new BadRequestException("Pickup location is outside service area");
        }

        for (CustomerFreightOrderDTO.AddressRequest drop : dropAddresses) {

            boolean dropAvailable =
                    distanceCalculationService.isLocationInsideServiceRadius(
                            drop.getLatitude(),
                            drop.getLongitude()
                    );

            if (!dropAvailable) {
                throw new BadRequestException("One or more drop locations are outside service area");
            }
        }
    }

    @Transactional
    private void deleteOldContainerDetails(FreightOrder order) {

        containerDetailRepository
                .findByFreightOrder(order)
                .ifPresent(containerDetail -> {

                    containerItemRepository.deleteByContainerDetail(containerDetail);
                    containerItemRepository.flush();

                    containerDetailRepository.delete(containerDetail);
                    containerDetailRepository.flush();
                });
    }

    private void saveOrUpdateContainerDetails(
            FreightOrder order,
            CustomerFreightOrderDTO.ContainerDetailRequest request
    ) {
        FreightOrderContainerDetail detail =
                containerDetailRepository.findByFreightOrder(order)
                        .orElse(new FreightOrderContainerDetail());

        detail.setFreightOrder(order);
        detail.setLoadType(request.getLoadType());
        detail.setDangerousGoods(request.getDangerousGoods());
        detail.setNatureOfGoods(request.getNatureOfGoods());
        detail.setShippingLine(request.getShippingLine());
        detail.setShippingLineEmail(request.getShippingLineEmail());
        detail.setCargoWeightKg(request.getCargoWeightKg());
        detail.setInsurance(request.getInsurance());
        detail.setCargoValue(request.getCargoValue());
        detail.setReturnEmptyContainer(request.getReturnEmptyContainer());

        FreightOrderContainerDetail savedDetail =
                containerDetailRepository.save(detail);

        containerItemRepository.deleteByContainerDetail(savedDetail);
        containerItemRepository.flush();

        if (request.getContainerItems() != null) {
            request.getContainerItems().forEach(item -> {
                FreightOrderContainerItem containerItem = new FreightOrderContainerItem();
                containerItem.setContainerDetail(savedDetail);
                containerItem.setContainerNumber(item.getContainerNumber());
                containerItem.setSealNumber(item.getSealNumber());
                containerItemRepository.save(containerItem);
            });
        }
    }

    private void deleteOldBreakBulkDetails(FreightOrder order) {
        breakBulkDetailRepository
                .findByFreightOrder(order)
                .ifPresent(breakBulkDetailRepository::delete);
        breakBulkDetailRepository.flush();
    }
    private CustomerFreightOrderDTO.EstimateRequest buildEstimateRequest(
            FreightOrder order,
            CustomerFreightOrderDTO.UpdateDraftRequest request
    ) {

        CustomerFreightOrderDTO.EstimateRequest estimateRequest =
                new CustomerFreightOrderDTO.EstimateRequest();

        estimateRequest.setVehicleCategoryUuid(
                order.getVehicleCategory().getUuid()
        );

        estimateRequest.setOrderType(
                order.getOrderType()
        );

        estimateRequest.setDistanceKm(
                order.getDistanceKm()
        );

        estimateRequest.setEstimatedTimeMinutes(
                order.getEstimatedTimeMinutes()
        );

        int dropCount =
                freightOrderAddressRepository
                        .findByFreightOrderAndAddressRole(
                                order,
                                AddressRole.DROP
                        )
                        .size();

        estimateRequest.setDropCount(dropCount);


        if (request.getContainerDetail() != null) {

            estimateRequest.setInsurance(
                    request.getContainerDetail().getInsurance()
            );

            estimateRequest.setCargoValue(
                    request.getContainerDetail().getCargoValue()
            );

            estimateRequest.setDangerousGoods(
                    request.getContainerDetail().getDangerousGoods()
            );
        }

        if (request.getBreakBulkDetail() != null) {

            estimateRequest.setInsurance(
                    request.getBreakBulkDetail().getInsuranceRequired()
            );

            estimateRequest.setManualLaborRequired(
                    request.getBreakBulkDetail().getManualLaborRequired()
            );

            estimateRequest.setForkliftNeeded(
                    request.getBreakBulkDetail().getForkliftNeeded()
            );

            estimateRequest.setCraneNeeded(
                    request.getBreakBulkDetail().getCraneNeeded()
            );

            estimateRequest.setTrolleyNeeded(
                    request.getBreakBulkDetail().getTrolleyNeeded()
            );

            estimateRequest.setTemperatureSensitive(
                    request.getBreakBulkDetail().getTemperatureSensitive()
            );

            estimateRequest.setDangerousGoods(
                    request.getBreakBulkDetail().getHazardousMaterial()
            );
        }

        return estimateRequest;
    }

    private void saveOrUpdatePricing(
            FreightOrder order,
            CustomerFreightOrderDTO.EstimateResponse estimate
    ) {

        FreightOrderPricing pricing =
                freightOrderPricingRepository
                        .findByFreightOrder(order)
                        .orElse(new FreightOrderPricing());

        pricing.setFreightOrder(order);
        pricing.setBaseCharges(estimate.getBaseCharges());
        pricing.setBaseFare(estimate.getBaseFare());
        pricing.setExtraKmFare(estimate.getExtraKmFare());
        pricing.setMultiDropCharges(estimate.getMultiDropCharges());
        pricing.setInsuranceCharges(estimate.getInsuranceCharges());
        pricing.setHelperCharges(estimate.getHelperCharges());
//        pricing.setHandlingCharges(estimate.getHandlingCharges());
//        pricing.setAdminServiceCharge(estimate.getAdminServiceCharge());
        pricing.setTaxAmount(estimate.getTaxAmount());
        pricing.setSubTotal(estimate.getSubTotal());
        pricing.setTotalAmount(estimate.getTotalAmount());

        freightOrderPricingRepository.save(pricing);
    }
    private CustomerFreightOrderDTO.EstimateResponse calculateFare(
            VehicleCategory vehicleCategory,
            SystemPricing systemPricing,
            CustomerFreightOrderDTO.EstimateRequest request
    ) {

        BigDecimal baseCharges =
                safe(vehicleCategory.getBaseCharges());

        BigDecimal baseFare =
                safe(vehicleCategory.getBaseFare());

        BigDecimal extraKmFare =
                BigDecimal.ZERO;

        double distanceKm =
                request.getDistanceKm() == null
                        ? 0.0
                        : request.getDistanceKm();

        double baseKm =
                vehicleCategory.getBaseKm() == null
                        ? 0.0
                        : vehicleCategory.getBaseKm();

        if (distanceKm > baseKm) {

            double extraKm =
                    distanceKm - baseKm;

            extraKmFare =
                    safe(vehicleCategory.getPerKmFare())
                            .multiply(BigDecimal.valueOf(extraKm));
        }

        int dropCount =
                request.getDropCount() == null || request.getDropCount() < 1
                        ? 1
                        : request.getDropCount();

        int extraDrops =
                Math.max(0, dropCount - 1);

        BigDecimal multiDropCharges =
                safe(vehicleCategory.getMultiDropCharges())
                        .multiply(BigDecimal.valueOf(extraDrops));

        BigDecimal insuranceCharges =
                BigDecimal.ZERO;

        if (Boolean.TRUE.equals(request.getInsurance())
                && request.getCargoValue() != null) {

            insuranceCharges =
                    request.getCargoValue()
                            .multiply(safe(systemPricing.getInsurancePercentage()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal helperCharges =
                BigDecimal.ZERO;

        if (Boolean.TRUE.equals(request.getManualLaborRequired())) {
            helperCharges =
                    safe(systemPricing.getHelperCharge());
        }

        BigDecimal handlingCharges =
                BigDecimal.ZERO;

        if (Boolean.TRUE.equals(request.getForkliftNeeded())) {
            handlingCharges =
                    handlingCharges.add(safe(systemPricing.getHelperCharge()));
        }

        if (Boolean.TRUE.equals(request.getCraneNeeded())) {
            handlingCharges =
                    handlingCharges.add(safe(systemPricing.getHelperCharge()));
        }

        if (Boolean.TRUE.equals(request.getTrolleyNeeded())) {
            handlingCharges =
                    handlingCharges.add(safe(systemPricing.getHelperCharge()));
        }

        if (Boolean.TRUE.equals(request.getDangerousGoods())) {
            handlingCharges =
                    handlingCharges.add(safe(systemPricing.getAdminServiceCharge()));
        }

        if (Boolean.TRUE.equals(request.getTemperatureSensitive())) {
            handlingCharges =
                    handlingCharges.add(safe(systemPricing.getAdminServiceCharge()));
        }

        BigDecimal adminServiceCharge =
                safe(systemPricing.getAdminServiceCharge());

        BigDecimal subTotal =
                baseCharges
                        .add(baseFare)
                        .add(extraKmFare)
                        .add(multiDropCharges)
                        .add(insuranceCharges)
                        .add(helperCharges)
                        .add(handlingCharges)
                        .add(adminServiceCharge);

        BigDecimal taxAmount =
                subTotal
                        .multiply(safe(systemPricing.getTaxPercentage()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount =
                subTotal.add(taxAmount);

        CustomerFreightOrderDTO.EstimateResponse response =
                new CustomerFreightOrderDTO.EstimateResponse();

        response.setBaseCharges(scale(baseCharges));
        response.setBaseFare(scale(baseFare));
        response.setExtraKmFare(scale(extraKmFare));
        response.setMultiDropCharges(scale(multiDropCharges));
        response.setInsuranceCharges(scale(insuranceCharges));
        response.setHelperCharges(scale(helperCharges));
        response.setHandlingCharges(scale(handlingCharges));
        response.setAdminServiceCharge(scale(adminServiceCharge));
        response.setTaxAmount(scale(taxAmount));
        response.setSubTotal(scale(subTotal));
        response.setTotalAmount(scale(totalAmount));

        return response;
    }

    private FreightOrderAddress toAddress(
            CustomerFreightOrderDTO.AddressRequest request
    ) {

        FreightOrderAddress address = new FreightOrderAddress();
        address.setAddressType(request.getAddressType());
        address.setAddressName(request.getAddressName());
        address.setContactName(request.getContactName());
        address.setContactNumber(request.getContactNumber());
        address.setFullAddress(request.getFullAddress());
        address.setLandmark(request.getLandmark());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        return address;
    }

    private void saveContainerDetails(
            FreightOrder order,
            CustomerFreightOrderDTO.ContainerDetailRequest request
    ) {

        FreightOrderContainerDetail detail =
                new FreightOrderContainerDetail();

        detail.setFreightOrder(order);
        detail.setLoadType(request.getLoadType());
        detail.setDangerousGoods(request.getDangerousGoods());
        detail.setNatureOfGoods(request.getNatureOfGoods());
        detail.setShippingLine(request.getShippingLine());
        detail.setShippingLineEmail(request.getShippingLineEmail());
        detail.setCargoWeightKg(request.getCargoWeightKg());
        detail.setInsurance(request.getInsurance());
        detail.setCargoValue(request.getCargoValue());
        detail.setReturnEmptyContainer(request.getReturnEmptyContainer());

        FreightOrderContainerDetail savedDetail =
                containerDetailRepository.save(detail);

        if (request.getContainerItems() != null) {
            request.getContainerItems().forEach(item -> {

                FreightOrderContainerItem containerItem =
                        new FreightOrderContainerItem();

                containerItem.setContainerDetail(savedDetail);
                containerItem.setContainerNumber(item.getContainerNumber());
                containerItem.setSealNumber(item.getSealNumber());

                containerItemRepository.save(containerItem);
            });
        }
    }

    private void saveBreakBulkDetails(
            FreightOrder order,
            CustomerFreightOrderDTO.BreakBulkDetailRequest request
    ) {

        FreightOrderBreakBulkDetail detail =
                new FreightOrderBreakBulkDetail();

        detail.setFreightOrder(order);
        detail.setCargoType(request.getCargoType());
        detail.setOtherCargoType(request.getOtherCargoType());
        detail.setCargoDetails(request.getCargoDetails());
        detail.setNatureOfGoods(request.getNatureOfGoods());
        detail.setTotalWeightKg(request.getTotalWeightKg());
        detail.setTotalVolumeCbm(request.getTotalVolumeCbm());
        detail.setNumberOfPieces(request.getNumberOfPieces());
        detail.setDimensionsPerPiece(request.getDimensionsPerPiece());
        detail.setFragile(request.getFragile());
        detail.setHandlingRequired(request.getHandlingRequired());
        detail.setSafetyRequired(request.getSafetyRequired());
        detail.setHazardousMaterial(request.getHazardousMaterial());
        detail.setTemperatureSensitive(request.getTemperatureSensitive());
        detail.setInsuranceRequired(request.getInsuranceRequired());
        detail.setForkliftNeeded(request.getForkliftNeeded());
        detail.setCraneNeeded(request.getCraneNeeded());
        detail.setTrolleyNeeded(request.getTrolleyNeeded());
        detail.setManualLaborRequired(request.getManualLaborRequired());

        breakBulkDetailRepository.save(detail);
    }

    private void saveDocuments(
            FreightOrder order,
            java.util.List<CustomerFreightOrderDTO.DocumentRequest> documents
    ) {

        documents.forEach(doc -> {

            FreightOrderDocument document =
                    new FreightOrderDocument();

            document.setFreightOrder(order);
            document.setDocumentType(doc.getDocumentType());
            document.setFileUrl(doc.getFileUrl());

            documentRepository.save(document);
        });
    }


    private BigDecimal safe(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }
}