package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.RestaurantMasterDTO;
import com.bt.fretchain.primary.dto.RestaurantPartnerDTO;
import com.bt.fretchain.primary.dto.RestaurantRegistrationDTO;
import com.bt.fretchain.primary.mapper.RestaurantMasterMapper;
import com.bt.fretchain.primary.pojo.RestaurantGlobalCategory;
import com.bt.fretchain.primary.pojo.RestaurantMaster;
import com.bt.fretchain.primary.pojo.RestaurantStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import com.bt.fretchain.primary.repository.RestaurantGlobalCategoryRepository;
import com.bt.fretchain.primary.repository.RestaurantMasterRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Public restaurant registration and the approval workflow.
 *
 *   register   (public)  -> restaurant PENDING + owner login
 *   approve / reject / suspend (admin)
 *   resubmit   (owner of a REJECTED restaurant) -> PENDING again
 */
@Service
@RequiredArgsConstructor
public class RestaurantRegistrationService {

    /** Allowed status changes through the approval action. */
    private static final Map<RestaurantStatus, Set<RestaurantStatus>> TRANSITIONS = Map.of(
            RestaurantStatus.PENDING, EnumSet.of(RestaurantStatus.APPROVED, RestaurantStatus.REJECTED),
            RestaurantStatus.REJECTED, EnumSet.of(RestaurantStatus.APPROVED, RestaurantStatus.PENDING),
            RestaurantStatus.APPROVED, EnumSet.of(RestaurantStatus.SUSPENDED),
            RestaurantStatus.SUSPENDED, EnumSet.of(RestaurantStatus.APPROVED)
    );

    private final RestaurantMasterRepository restaurantRepository;
    private final RestaurantGlobalCategoryRepository globalCategoryRepository;
    private final SystemUserRepository userRepository;
    private final RestaurantPartnerService partnerService;
    private final RestaurantAccessService access;
    private final RestaurantMasterMapper mapper;

    // =====================================================================
    // Public
    // =====================================================================

    public List<KeyValueDto> globalCategories() {
        return globalCategoryRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameEnAsc()
                .stream()
                .map(c -> new KeyValueDto(c.getUuid(), c.getNameEn()))
                .toList();
    }

    @Transactional
    public RestaurantRegistrationDTO.Registered register(RestaurantRegistrationDTO.Register req) {
        if ((req.getOpeningTime() == null) != (req.getClosingTime() == null)
                || (req.getOpeningTime() != null && req.getOpeningTime().equals(req.getClosingTime()))) {
            throw new BadRequestException("restaurant.timing.invalid");
        }
        // checked up-front so no half-created restaurant is left behind
        if (userRepository.existsByEmailIgnoreCase(req.getOwnerEmail().trim())) {
            throw new DuplicateResourceException("user.email.exists");
        }

        Set<String> uuids = new LinkedHashSet<>(req.getGlobalCategoryUuids());
        List<RestaurantGlobalCategory> categories = globalCategoryRepository.findByUuidInAndDeletedFalse(uuids);
        if (categories.size() != uuids.size() || categories.stream().anyMatch(c -> !c.isActive())) {
            throw new BadRequestException("restaurant.global.category.invalid");
        }

        RestaurantMaster restaurant = new RestaurantMaster();
        restaurant.setNameEn(req.getNameEn().trim());
        restaurant.setNameFr(req.getNameFr());
        restaurant.setDescriptionEn(req.getDescriptionEn());
        restaurant.setDescriptionFr(req.getDescriptionFr());
        restaurant.setCountryCode(req.getCountryCode());
        restaurant.setMobile(req.getMobile());
        restaurant.setEmail(req.getEmail());
        restaurant.setLogoUrl(req.getLogoUrl());
        restaurant.setBannerUrl(req.getBannerUrl());
        restaurant.setAddress(req.getAddress());
        restaurant.setCity(req.getCity());
        restaurant.setState(req.getState());
        restaurant.setCountry(req.getCountry());
        restaurant.setPostalCode(req.getPostalCode());
        restaurant.setLatitude(req.getLatitude());
        restaurant.setLongitude(req.getLongitude());
        restaurant.setOpeningTime(req.getOpeningTime());
        restaurant.setClosingTime(req.getClosingTime());
        restaurant.setFoodType(req.getFoodType());
        restaurant.setCostForTwo(req.getCostForTwo());
        restaurant.setGlobalCategories(new LinkedHashSet<>(categories));
        // approval-related fields are always decided by the platform
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setStatusChangedAt(LocalDateTime.now());
        restaurant.setSelfRegistered(true);
        restaurant = restaurantRepository.save(restaurant);

        RestaurantPartnerDTO.OwnerSave owner = new RestaurantPartnerDTO.OwnerSave();
        owner.setFullName(req.getOwnerName());
        owner.setEmail(req.getOwnerEmail());
        owner.setMobile(req.getOwnerMobile());
        owner.setPassword(req.getPassword());
        RestaurantPartnerDTO.StaffDetail ownerDetail = partnerService.saveOwner(restaurant.getUuid(), owner, (Long) null);

        RestaurantRegistrationDTO.Registered result = new RestaurantRegistrationDTO.Registered();
        result.setRestaurantUuid(restaurant.getUuid());
        result.setRestaurantName(restaurant.getNameEn());
        result.setStatus(restaurant.getStatus());
        result.setLoginEmail(ownerDetail.getEmail());
        return result;
    }

    // =====================================================================
    // Admin
    // =====================================================================

    @Transactional
    public RestaurantMasterDTO.Detail changeStatus(String restaurantUuid, RestaurantRegistrationDTO.ApprovalUpdate req, SystemUser admin) {
        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(restaurantUuid)
                .orElseThrow(() -> new ResourceNotFoundException("restaurant.notfound"));

        RestaurantStatus target = req.getStatus();
        if (!TRANSITIONS.getOrDefault(restaurant.getStatus(), Set.of()).contains(target)) {
            throw new BadRequestException("approval.transition.invalid");
        }
        String reason = req.getReason() == null ? null : req.getReason().trim();
        boolean needsReason = target == RestaurantStatus.REJECTED || target == RestaurantStatus.SUSPENDED;
        if (needsReason && (reason == null || reason.isEmpty())) {
            throw new BadRequestException("approval.reason.required");
        }

        restaurant.setStatus(target);
        restaurant.setStatusReason(needsReason ? reason : null);
        restaurant.setStatusChangedAt(LocalDateTime.now());
        restaurant.setModifiedBy(admin.getRecordId());
        return mapper.toDetailDto(restaurantRepository.save(restaurant));
    }

    // =====================================================================
    // Restaurant login
    // =====================================================================

    /** A rejected restaurant fixed its details and asks for a new review. */
    @Transactional
    public RestaurantPartnerDTO.Session resubmit(SystemUser user) {
        RestaurantStaff staff = access.require(user, RestaurantActivity.PROFILE_EDIT);
        RestaurantMaster restaurant = staff.getRestaurant();
        if (restaurant.getStatus() != RestaurantStatus.REJECTED) {
            throw new BadRequestException("store.resubmit.invalid");
        }
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setStatusReason(null);
        restaurant.setStatusChangedAt(LocalDateTime.now());
        restaurant.setModifiedBy(user.getRecordId());
        restaurantRepository.save(restaurant);
        return access.session(staff);
    }
}
