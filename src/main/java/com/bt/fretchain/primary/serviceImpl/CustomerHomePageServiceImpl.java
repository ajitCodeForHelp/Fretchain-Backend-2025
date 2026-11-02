package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.CustomerHomePageDTO;
import com.bt.fretchain.primary.dto.DistanceTimeDTO;
import com.bt.fretchain.primary.mapper.BannerMapper;
import com.bt.fretchain.primary.mapper.CustomerHomePageMapper;
import com.bt.fretchain.primary.pojo.VehicleCategory;
import com.bt.fretchain.primary.repository.BannerRepository;
import com.bt.fretchain.primary.repository.CategoryRepository;
import com.bt.fretchain.primary.repository.VehicleCategoryRepository;
import com.bt.fretchain.primary.service.CustomerHomePageService;
import com.bt.fretchain.primary.service.DistanceCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerHomePageServiceImpl implements CustomerHomePageService {

    private final CategoryRepository categoryRepository;

    private final VehicleCategoryRepository vehicleCategoryRepository;

    private final CustomerHomePageMapper customerHomePageMapper;

    private final DistanceCalculationService distanceCalculationService;

    private final BannerRepository bannerRepository;

    private final BannerMapper bannerMapper;

    @Value("${distance.provider}")
    private String distanceApi;


    @Override
    public List<CustomerHomePageDTO.BannerResponse> getBannerList() {

        return bannerRepository
                .findActiveBanners(LocalDateTime.now())
                .stream()
                .map(bannerMapper::toCustomerBannerResponse)
                .toList();
    }

    @Override
    public List<CustomerHomePageDTO.CategoryResponse> getCategoryList() {

        return categoryRepository
                .findByActiveTrueAndDeletedFalseOrderByCreatedAtAsc()
                .stream()
                .map(category ->
                        customerHomePageMapper.toCategoryResponse(
                                category
                        ))
                .toList();
    }

    @Override
    public List<CustomerHomePageDTO.VehicleCategoryResponse>
    getVehicleCategoryList(
            CustomerHomePageDTO.VehicleCategoryFareRequest request
    ) {

        List<VehicleCategory> vehicleCategories;

//        if (request.getSubCategoryUuid() != null
//                && !request.getSubCategoryUuid().isBlank()) {
//
//            vehicleCategories =
//                    vehicleCategoryRepository
//                            .findByCategoryAndSubCategory(
//                                    request.getCategoryUuid(),
//                                    request.getSubCategoryUuid()
//                            );
//
//        } else {

            vehicleCategories =
                    vehicleCategoryRepository
                            .findByCategory(
                                    request.getCategoryUuid()
                            );
//        }

        return vehicleCategories
                .stream()
                .map(vehicleCategory -> {

                    CustomerHomePageDTO.VehicleCategoryResponse response =
                            customerHomePageMapper
                                    .toVehicleCategoryResponse(vehicleCategory);

                    response.setDistanceKm(request.getDistanceKm());
                    response.setEstimatedTimeMinutes(
                            request.getEstimatedTimeMinutes()
                    );
                    response.setDropCount(request.getDropCount());

                    response.setEstimatedFare(
                            calculateEstimatedFare(
                                    vehicleCategory,
                                    request.getDistanceKm(),
                                    request.getDropCount()
                            )
                    );

                    return response;
                })
                .toList();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calculateEstimatedFare(
            VehicleCategory vehicleCategory,
            Double distanceKm,
            Integer dropCount
    ) {

        BigDecimal total =
                safe(vehicleCategory.getBaseCharges())
                        .add(safe(vehicleCategory.getBaseFare()));

        Double baseKm =
                vehicleCategory.getBaseKm() == null
                        ? 0.0
                        : vehicleCategory.getBaseKm();

        Double totalDistance =
                distanceKm == null
                        ? 0.0
                        : distanceKm;

        if (totalDistance > baseKm) {

            double extraKm =
                    totalDistance - baseKm;

            BigDecimal extraKmFare =
                    safe(vehicleCategory.getPerKmFare())
                            .multiply(BigDecimal.valueOf(extraKm));

            total =
                    total.add(extraKmFare);
        }

        int totalDrops =
                dropCount == null || dropCount < 1
                        ? 1
                        : dropCount;

        int extraDrops =
                Math.max(0, totalDrops - 1);

        if (extraDrops > 0) {

            BigDecimal multiDropFare =
                    safe(vehicleCategory.getMultiDropCharges())
                            .multiply(BigDecimal.valueOf(extraDrops));

            total =
                    total.add(multiDropFare);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public CustomerHomePageDTO.CheckServiceResponse checkService(
            CustomerHomePageDTO.CheckServiceRequest request
    ) {

        boolean pickupAvailable =
                distanceCalculationService.isLocationInsideServiceRadius(
                        request.getPickupLatitude(),
                        request.getPickupLongitude()
                );

        boolean allDropAvailable = true;

        for (CustomerHomePageDTO.DropLocationRequest drop :
                request.getDropLocations()) {

            boolean dropAvailable =
                    distanceCalculationService.isLocationInsideServiceRadius(
                            drop.getLatitude(),
                            drop.getLongitude()
                    );

            if (!dropAvailable) {
                allDropAvailable = false;
                break;
            }
        }

        CustomerHomePageDTO.CheckServiceResponse response =
                new CustomerHomePageDTO.CheckServiceResponse();

        response.setPickupServiceAvailable(pickupAvailable);
        response.setAllDropServiceAvailable(allDropAvailable);
        response.setServiceAvailable(pickupAvailable && allDropAvailable);
        response.setDropCount(request.getDropLocations().size());

        if (!pickupAvailable || !allDropAvailable) {

            response.setDistanceKm(0.0);
            response.setEstimatedTimeMinutes(0);

            if (!pickupAvailable && !allDropAvailable) {
                response.setMessage("Pickup and one or more drop locations are outside service area");
            } else if (!pickupAvailable) {
                response.setMessage("Pickup location is outside service area");
            } else {
                response.setMessage("One or more drop locations are outside service area");
            }

            return response;
        }

        DistanceTimeDTO distanceTimeDTO =
                distanceCalculationService.calculateMultiDropDistance(
                        request.getPickupLatitude(),
                        request.getPickupLongitude(),
                        request.getDropLocations()
                );

        response.setDistanceKm(distanceTimeDTO.getDistanceKm());
        response.setEstimatedTimeMinutes(
                distanceTimeDTO.getEstimatedTimeMinutes()
        );
        response.setMessage("Service available");

        return response;
    }
}