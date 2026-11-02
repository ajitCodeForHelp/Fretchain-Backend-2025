package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.AdminFreightOrderDTO;
import com.bt.fretchain.primary.dto.AvailableDriverResponse;
import com.bt.fretchain.primary.dto.CustomerFreightOrderDTO;
import com.bt.fretchain.primary.dto.DriverAssignmentDTO;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.AddressRole;
import com.bt.fretchain.primary.pojo.enums.DriverAssignmentStatus;
import com.bt.fretchain.primary.pojo.enums.FreightOrderType;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.primary.service.AdminFreightOrderService;
import com.bt.fretchain.primary.service.DriverNotificationService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminFreightOrderServiceImpl implements AdminFreightOrderService {

    private final FreightOrderRepository freightOrderRepository;

    private final FreightOrderAddressRepository freightOrderAddressRepository;

    private final FreightOrderPricingRepository freightOrderPricingRepository;

    private final FreightOrderDocumentRepository documentRepository;

    private final FreightOrderContainerDetailRepository containerDetailRepository;

    private final FreightOrderContainerItemRepository containerItemRepository;

    private final FreightOrderBreakBulkDetailRepository breakBulkDetailRepository;

    private final FreightOrderParcelDetailRepository parcelDetailRepository;

    private final FreightOrderBulkMaterialDetailRepository bulkMaterialDetailRepository;

    private final FreightOrderStatusLogRepository statusLogRepository;

    private final DriverRepository driverRepository;

    private final OrderDriverAssignmentRepository orderDriverAssignmentRepository;

    private final DriverNotificationService driverNotificationService;
    @Override
    @Transactional(readOnly = true)
    public DataTableResponsePacket<AdminFreightOrderDTO.ListResponse> getListData(
            AdminFreightOrderDTO.ListRequest request
    ) {

        Pageable pageable = pageRequestCreate(request);

        Specification<FreightOrder> specification =
                createFilterOrSearch(request);

        Page<FreightOrder> page =
                freightOrderRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(this::toListResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFreightOrderDTO.DetailResponse getById(
            String orderUuid
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("order.notfound")
                        );

        AdminFreightOrderDTO.DetailResponse response =
                new AdminFreightOrderDTO.DetailResponse();

        response.setOrderUuid(order.getUuid());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderType(order.getOrderType());
        response.setOrderStatus(order.getOrderStatus());
        response.setPickupType(order.getPickupType());
        response.setPickupDateTime(order.getPickupDateTime());
        response.setDistanceKm(order.getDistanceKm());
        response.setEstimatedTimeMinutes(order.getEstimatedTimeMinutes());
        response.setInstructionForDriver(order.getInstructionForDriver());
        response.setTotalAmount(order.getTotalAmount());

        mapCustomer(order, response);
        mapCategory(order, response);
        mapVehicleCategory(order, response);
        mapAddresses(order, response);
        mapPricing(order, response);
        mapDocuments(order, response);
        mapOrderTypeDetail(order, response);
        mapStatusLogs(order, response);

        return response;
    }

    @Override
    @Transactional
    public void updateStatus(
            String orderUuid,
            OrderStatus status,
            String remarks,
            SystemUser user
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("order.notfound")
                        );

        if (order.getOrderStatus() == status) {
            throw new BadRequestException("order.status.same");
        }

        order.setOrderStatus(status);
        order.setModifiedBy(user.getRecordId());

        FreightOrder savedOrder =
                freightOrderRepository.save(order);

        FreightOrderStatusLog log =
                new FreightOrderStatusLog();

        log.setFreightOrder(savedOrder);
        log.setOrderStatus(status);
        log.setRemarks(remarks);
        log.setUpdatedBy(user);
        log.setStatusTime(LocalDateTime.now());

        statusLogRepository.save(log);
    }

    private AdminFreightOrderDTO.ListResponse toListResponse(
            FreightOrder order
    ) {

        AdminFreightOrderDTO.ListResponse response =
                new AdminFreightOrderDTO.ListResponse();

        response.setOrderUuid(order.getUuid());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderType(order.getOrderType());
        response.setOrderStatus(order.getOrderStatus());
        response.setPickupType(order.getPickupType());
        response.setPickupDateTime(order.getPickupDateTime());
        response.setDistanceKm(order.getDistanceKm());
        response.setEstimatedTimeMinutes(order.getEstimatedTimeMinutes());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getCustomer() != null) {
            response.setCustomerName(order.getCustomer().getFullName());
            response.setCustomerPhone(order.getCustomer().getMobile());
        }

        if (order.getCategory() != null) {
            response.setCategoryNameEn(order.getCategory().getNameEn());
            response.setCategoryNameFr(order.getCategory().getNameFr());
        }

        if (order.getSubCategory() != null) {
            response.setSubCategoryNameEn(order.getSubCategory().getNameEn());
            response.setSubCategoryNameFr(order.getSubCategory().getNameFr());
        }

        if (order.getVehicleCategory() != null) {
            response.setVehicleCategoryNameEn(order.getVehicleCategory().getNameEn());
            response.setVehicleCategoryNameFr(order.getVehicleCategory().getNameFr());
        }

        return response;
    }

    private void mapCustomer(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
    ) {

        if (order.getCustomer() == null) {
            return;
        }

        AdminFreightOrderDTO.CustomerDetail customer =
                new AdminFreightOrderDTO.CustomerDetail();

        customer.setUuid(order.getCustomer().getUuid());
        customer.setName(order.getCustomer().getFullName());
        customer.setMobile(order.getCustomer().getMobile());
        customer.setEmail(order.getCustomer().getEmail());

        response.setCustomer(customer);
    }

    private void mapCategory(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
    ) {

        if (order.getCategory() != null) {
            AdminFreightOrderDTO.CategoryDetail category =
                    new AdminFreightOrderDTO.CategoryDetail();

            category.setUuid(order.getCategory().getUuid());
            category.setNameEn(order.getCategory().getNameEn());
            category.setNameFr(order.getCategory().getNameFr());

            response.setCategory(category);
        }

        if (order.getSubCategory() != null) {
            AdminFreightOrderDTO.CategoryDetail subCategory =
                    new AdminFreightOrderDTO.CategoryDetail();

            subCategory.setUuid(order.getSubCategory().getUuid());
            subCategory.setNameEn(order.getSubCategory().getNameEn());
            subCategory.setNameFr(order.getSubCategory().getNameFr());

            response.setSubCategory(subCategory);
        }
    }

    private void mapVehicleCategory(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
    ) {

        if (order.getVehicleCategory() == null) {
            return;
        }

        VehicleCategory vc = order.getVehicleCategory();

        AdminFreightOrderDTO.VehicleCategoryDetail vehicle =
                new AdminFreightOrderDTO.VehicleCategoryDetail();

        vehicle.setUuid(vc.getUuid());
        vehicle.setNameEn(vc.getNameEn());
        vehicle.setNameFr(vc.getNameFr());
        vehicle.setIconUrl(vc.getIconUrl());
        vehicle.setMaxWeightKg(vc.getMaxWeightKg());

        response.setVehicleCategory(vehicle);
    }

    private void mapAddresses(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
    ) {

        List<FreightOrderAddress> addresses =
                freightOrderAddressRepository
                        .findByFreightOrderOrderBySequenceNoAsc(order);

        List<AdminFreightOrderDTO.AddressDetail> drops =
                new ArrayList<>();

        for (FreightOrderAddress address : addresses) {

            AdminFreightOrderDTO.AddressDetail dto =
                    new AdminFreightOrderDTO.AddressDetail();

            dto.setAddressType(address.getAddressType());
            dto.setAddressName(address.getAddressName());
            dto.setContactName(address.getContactName());
            dto.setContactNumber(address.getContactNumber());
            dto.setFullAddress(address.getFullAddress());
            dto.setLandmark(address.getLandmark());
            dto.setLatitude(address.getLatitude());
            dto.setLongitude(address.getLongitude());
            dto.setSequenceNo(address.getSequenceNo());

            if (address.getAddressRole() == AddressRole.PICKUP) {
                response.setPickupAddress(dto);
            } else {
                drops.add(dto);
            }
        }

        response.setDropAddresses(drops);
    }

    private void mapPricing(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
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

    private void mapDocuments(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
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

    private void mapOrderTypeDetail(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
    ) {

        if (order.getOrderType() == null) {
            return;
        }

        switch (order.getOrderType()) {

            case CONTAINER -> mapContainerDetail(order, response);

            case BREAK_BULK -> mapBreakBulkDetail(order, response);

            case PARCEL -> mapParcelDetail(order, response);

            case BULK_MATERIAL -> mapBulkMaterialDetail(order, response);
        }
    }

    private void mapContainerDetail(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
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
            AdminFreightOrderDTO.DetailResponse response
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
            AdminFreightOrderDTO.DetailResponse response
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
            AdminFreightOrderDTO.DetailResponse response
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

    private void mapStatusLogs(
            FreightOrder order,
            AdminFreightOrderDTO.DetailResponse response
    ) {

        List<AdminFreightOrderDTO.StatusLogResponse> logs =
                statusLogRepository
                        .findByFreightOrderOrderByStatusTimeAsc(order)
                        .stream()
                        .map(log -> {

                            AdminFreightOrderDTO.StatusLogResponse dto =
                                    new AdminFreightOrderDTO.StatusLogResponse();

                            dto.setOrderStatus(log.getOrderStatus());
                            dto.setRemarks(log.getRemarks());
                            dto.setStatusTime(log.getStatusTime());

                            if (log.getUpdatedBy() != null) {
                                dto.setUpdatedByName(
                                        log.getUpdatedBy().getFullName()
                                );
                            }

                            return dto;
                        })
                        .toList();

        response.setStatusLogs(logs);
    }

    private Pageable pageRequestCreate(
            AdminFreightOrderDTO.ListRequest request
    ) {

        Sort sort =
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                );

        if (request.getSort() != null
                && request.getSort().getField() != null
                && !request.getSort().getField().isBlank()) {

            Sort.Direction direction =
                    "asc".equalsIgnoreCase(request.getSort().getDirection())
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            sort =
                    Sort.by(
                            direction,
                            request.getSort().getField()
                    );
        }

        return PageRequest.of(
                request.getPageNumber(),
                request.getPageSize(),
                sort
        );
    }

    private Specification<FreightOrder> createFilterOrSearch(
            AdminFreightOrderDTO.ListRequest request
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    cb.equal(
                            root.get("deleted"),
                            false
                    )
            );

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search =
                        "%"
                                + request.getSearch().toLowerCase()
                                + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("orderNumber")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("customer").get("name")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("customer").get("mobile")),
                                        search
                                )
                        )
                );
            }

            if (request.getFilters() != null) {

                if (request.getFilters().getOrderStatus() != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("orderStatus"),
                                    request.getFilters().getOrderStatus()
                            )
                    );
                }

                if (request.getFilters().getOrderType() != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("orderType"),
                                    request.getFilters().getOrderType()
                            )
                    );
                }

                if (request.getFilters().getPickupType() != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("pickupType"),
                                    request.getFilters().getPickupType()
                            )
                    );
                }

                if (request.getFilters().getCategoryUuid() != null
                        && !request.getFilters().getCategoryUuid().isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("category").get("uuid"),
                                    request.getFilters().getCategoryUuid()
                            )
                    );
                }

                if (request.getFilters().getSubCategoryUuid() != null
                        && !request.getFilters().getSubCategoryUuid().isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("subCategory").get("uuid"),
                                    request.getFilters().getSubCategoryUuid()
                            )
                    );
                }

                if (request.getFilters().getVehicleCategoryUuid() != null
                        && !request.getFilters().getVehicleCategoryUuid().isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("vehicleCategory").get("uuid"),
                                    request.getFilters().getVehicleCategoryUuid()
                            )
                    );
                }
            }

            if (request.getDateFilter() != null) {

                if (request.getDateFilter().getFrom() != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(
                                    root.get("createdAt"),
                                    request.getDateFilter()
                                            .getFrom()
                                            .atStartOfDay()
                            )
                    );
                }

                if (request.getDateFilter().getTo() != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(
                                    root.get("createdAt"),
                                    request.getDateFilter()
                                            .getTo()
                                            .atTime(23, 59, 59)
                            )
                    );
                }
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }


    @Override
    @Transactional(readOnly = true)
    public List<AvailableDriverResponse> getAvailableDrivers(
            String orderUuid,
            SystemUser user
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "freight.order.not.found"
                                )
                        );

//        validateOrderAccess(order, user);

        return driverRepository.getAvailableDriversForOrder(
                order.getRecordId()
        );
    }


    @Override
    @Transactional
     public  DriverAssignmentDTO.AssignResponse assignDriver(
            String orderUuid,
            DriverAssignmentDTO.AssignRequest request,
            SystemUser user
    ) {

        FreightOrder order =
                freightOrderRepository
                        .findByUuidAndDeletedFalse(orderUuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "freight.order.not.found"
                                )
                        );

        if (order.getOrderStatus() == OrderStatus.CANCELLED_BY_CUSTOMER) {

            throw new BadRequestException(
                    "freight.order.cancelled"
            );
        }

        if (order.getDriver() != null) {
            throw new BadRequestException(
                    "freight.order.driver.already.assigned"
            );
        }

        Driver driver =
                driverRepository
                        .findByUuidAndDeletedFalse(
                                request.getDriverUuid()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "driver.not.found"
                                )
                        );

        if (!Boolean.TRUE.equals(driver.getVerified())) {

            throw new BadRequestException(
                    "driver.not.verified"
            );
        }


        if (!Boolean.TRUE.equals(driver.getAvailable())) {

            throw new BadRequestException(
                    "driver.not.available"
            );
        }


        if (!Boolean.TRUE.equals(driver.getOnlineStatus())) {

            throw new BadRequestException(
                    "driver.not.online"
            );
        }


        if (driver.getVehicle() == null) {

            throw new ResourceNotFoundException(
                    "driver.vehicle.not.assigned"
            );
        }

        if (order.getVehicleCategory() == null
                || driver.getVehicle().getVehicleCategory() == null) {

            throw new ResourceNotFoundException(
                    "vehicle.category.not.configured"
            );
        }


        if (!order.getVehicleCategory()
                .getRecordId()
                .equals(
                        driver.getVehicle()
                                .getVehicleCategory()
                                .getRecordId()
                )) {

            throw new BadRequestException(
                    "driver.vehicle.category.mismatch"
            );
        }

        boolean pendingAssignmentExists =
                orderDriverAssignmentRepository
                        .existsByFreightOrderRecordIdAndAssignmentStatus(
                                order.getRecordId(),
                                DriverAssignmentStatus.PENDING
                        );

        if (pendingAssignmentExists) {

            throw new BadRequestException(
                    "driver.assignment.already.pending"
            );
        }

        OrderDriverAssignment assignment =
                new OrderDriverAssignment();

        assignment.setFreightOrder(order);

        assignment.setDriver(driver);

        assignment.setAssignmentStatus(
                DriverAssignmentStatus.PENDING
        );

        assignment.setAssignedAt(
                LocalDateTime.now()
        );


        assignment.setCreatedBy(
                user.getRecordId()
        );

        assignment.setModifiedBy(
                user.getRecordId()
        );


        OrderDriverAssignment savedAssignment =
                orderDriverAssignmentRepository.save(
                        assignment
                );

        driverNotificationService.sendOrderAssignmentNotification(
                savedAssignment
        );

        if (driver.getDeviceToken() == null
                || driver.getDeviceToken().isBlank()) {
            savedAssignment.setNotificationSent(true);

            savedAssignment.setNotificationSentAt(
                    LocalDateTime.now()
            );

            orderDriverAssignmentRepository.save(
                    savedAssignment
            );
        }

        return DriverAssignmentDTO.AssignResponse.builder()

                .assignmentUuid(
                        savedAssignment.getUuid()
                )

                .orderUuid(
                        order.getUuid()
                )

                .orderNumber(
                        order.getOrderNumber()
                )

                .driverUuid(
                        driver.getUuid()
                )

                .driverName(
                        driver.getFullName()
                )

                .assignmentStatus(
                        savedAssignment
                                .getAssignmentStatus()
                )

                .build();
    }
}