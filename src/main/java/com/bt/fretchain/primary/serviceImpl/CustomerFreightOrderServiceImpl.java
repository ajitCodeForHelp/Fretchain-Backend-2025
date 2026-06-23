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
import lombok.RequiredArgsConstructor;
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

    private final VehicleCategoryRepository vehicleCategoryRepository;

    private final SystemPricingRepository systemPricingRepository;

    private final DistanceCalculationService distanceCalculationService;

    private final FreightOrderStatusLogRepository freightOrderStatusLogRepository;

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
            CustomerFreightOrderDTO.CreateDraftRequest request
    ) {

        VehicleCategory vehicleCategory =
                vehicleCategoryRepository
                        .findByUuidAndDeletedFalse(request.getVehicleCategoryUuid())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("vehicle.category.notfound")
                        );

        validateServiceArea(
                request.getPickupAddress(),
                request.getDropAddresses()
        );

        FreightOrder order = new FreightOrder();

        order.setOrderType(request.getOrderType());
        order.setOrderStatus(OrderStatus.DRAFT);
        order.setVehicleCategory(vehicleCategory);

        order.setDistanceKm(request.getDistanceKm());
        order.setEstimatedTimeMinutes(request.getEstimatedTimeMinutes());

        FreightOrder savedOrder =
                freightOrderRepository.save(order);

        saveOrderAddresses(
                savedOrder,
                request.getPickupAddress(),
                request.getDropAddresses()
        );


        savedOrder =
                freightOrderRepository.save(savedOrder);


        savedOrder.setOrderNumber("FC-" + savedOrder.getRecordId());

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
            CustomerFreightOrderDTO.UpdateDraftRequest request
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("order.notfound")
                        );

        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new BadRequestException("Only draft order can be updated");
        }

        if (request.getVehicleCategoryUuid() != null
                && !request.getVehicleCategoryUuid().isBlank()) {

            VehicleCategory vehicleCategory =
                    vehicleCategoryRepository
                            .findByUuidAndDeletedFalse(request.getVehicleCategoryUuid())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("vehicle.category.notfound")
                            );

            order.setVehicleCategory(vehicleCategory);
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

            freightOrderAddressRepository
                    .deleteByFreightOrder(order);

            saveOrderAddresses(
                    order,
                    request.getPickupAddress(),
                    request.getDropAddresses()
            );

            if (request.getDistanceKm() != null) {
                order.setDistanceKm(
                        request.getDistanceKm()
                );
            }

            if (request.getEstimatedTimeMinutes() != null) {
                order.setEstimatedTimeMinutes(
                        request.getEstimatedTimeMinutes()
                );
            }
        }

        FreightOrder savedOrder =
                freightOrderRepository.save(order);

        if (savedOrder.getOrderType() == FreightOrderType.CONTAINER
                && request.getContainerDetail() != null) {

            deleteOldContainerDetails(savedOrder);

            saveContainerDetails(
                    savedOrder,
                    request.getContainerDetail()
            );
        }

        if (savedOrder.getOrderType() == FreightOrderType.BREAK_BULK
                && request.getBreakBulkDetail() != null) {

            deleteOldBreakBulkDetails(savedOrder);

            saveBreakBulkDetails(
                    savedOrder,
                    request.getBreakBulkDetail()
            );
        }

        if (request.getDocuments() != null) {

            documentRepository.deleteByFreightOrder(savedOrder);

            saveDocuments(
                    savedOrder,
                    request.getDocuments()
            );
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

        saveOrUpdatePricing(
                savedOrder,
                estimateResponse
        );

        savedOrder.setTotalAmount(
                estimateResponse.getTotalAmount()
        );

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
            String orderUuid
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
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

        mapOrderAddresses(order, response);
        mapOrderPricing(order, response);
        mapOrderDocuments(order, response);
        mapContainerDetail(order, response);
        mapBreakBulkDetail(order, response);

        return response;
    }


    @Override
    @Transactional
    public CustomerFreightOrderDTO.PlaceOrderResponse placeOrder(
            String orderUuid
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
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

        CustomerFreightOrderDTO.PlaceOrderResponse response =
                new CustomerFreightOrderDTO.PlaceOrderResponse();

        response.setOrderUuid(savedOrder.getUuid());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setTotalAmount(savedOrder.getTotalAmount());

        return response;
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

    private void deleteOldContainerDetails(FreightOrder order) {
        containerDetailRepository
                .findByFreightOrder(order)
                .ifPresent(containerDetail -> {
                    containerItemRepository.deleteByContainerDetail(containerDetail);
                    containerDetailRepository.delete(containerDetail);
                });
    }

    private void deleteOldBreakBulkDetails(FreightOrder order) {
        breakBulkDetailRepository
                .findByFreightOrder(order)
                .ifPresent(breakBulkDetailRepository::delete);
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