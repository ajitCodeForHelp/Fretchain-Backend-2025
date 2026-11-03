package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopMasterDTO;
import com.bt.fretchain.primary.dto.ShopPartnerDTO;
import com.bt.fretchain.primary.dto.ShopRegistrationDTO;
import com.bt.fretchain.primary.mapper.ShopMasterMapper;
import com.bt.fretchain.primary.pojo.ShopGlobalCategory;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.ShopStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import com.bt.fretchain.primary.repository.ShopGlobalCategoryRepository;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Public shop registration and the approval workflow.
 *
 *   register   (public)  -> shop PENDING + owner login
 *   approve / reject / suspend (admin)
 *   resubmit   (owner of a REJECTED shop) -> PENDING again
 */
@Service
@RequiredArgsConstructor
public class ShopRegistrationService {

    /** Allowed status changes through the approval action. */
    private static final Map<ShopStatus, Set<ShopStatus>> TRANSITIONS = Map.of(
            ShopStatus.PENDING, EnumSet.of(ShopStatus.APPROVED, ShopStatus.REJECTED),
            ShopStatus.REJECTED, EnumSet.of(ShopStatus.APPROVED, ShopStatus.PENDING),
            ShopStatus.APPROVED, EnumSet.of(ShopStatus.SUSPENDED),
            ShopStatus.SUSPENDED, EnumSet.of(ShopStatus.APPROVED)
    );

    private final ShopMasterRepository shopRepository;
    private final ShopGlobalCategoryRepository globalCategoryRepository;
    private final SystemUserRepository userRepository;
    private final ShopPartnerService partnerService;
    private final ShopAccessService access;
    private final ShopMasterMapper mapper;

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
    public ShopRegistrationDTO.Registered register(ShopRegistrationDTO.Register req) {
        if ((req.getOpeningTime() == null) != (req.getClosingTime() == null)
                || (req.getOpeningTime() != null && req.getOpeningTime().equals(req.getClosingTime()))) {
            throw new BadRequestException("shop.timing.invalid");
        }
        // checked up-front so no half-created shop is left behind
        if (userRepository.existsByEmailIgnoreCase(req.getOwnerEmail().trim())) {
            throw new DuplicateResourceException("user.email.exists");
        }

        Set<String> uuids = new LinkedHashSet<>(req.getGlobalCategoryUuids());
        List<ShopGlobalCategory> categories = globalCategoryRepository.findByUuidInAndDeletedFalse(uuids);
        if (categories.size() != uuids.size() || categories.stream().anyMatch(c -> !c.isActive())) {
            throw new BadRequestException("shop.global.category.invalid");
        }

        ShopMaster shop = new ShopMaster();
        shop.setNameEn(req.getNameEn().trim());
        shop.setNameFr(req.getNameFr());
        shop.setDescriptionEn(req.getDescriptionEn());
        shop.setDescriptionFr(req.getDescriptionFr());
        shop.setCountryCode(req.getCountryCode());
        shop.setMobile(req.getMobile());
        shop.setEmail(req.getEmail());
        shop.setLogoUrl(req.getLogoUrl());
        shop.setBannerUrl(req.getBannerUrl());
        shop.setAddress(req.getAddress());
        shop.setCity(req.getCity());
        shop.setState(req.getState());
        shop.setCountry(req.getCountry());
        shop.setPostalCode(req.getPostalCode());
        shop.setLatitude(req.getLatitude());
        shop.setLongitude(req.getLongitude());
        shop.setOpeningTime(req.getOpeningTime());
        shop.setClosingTime(req.getClosingTime());
        shop.setGlobalCategories(new LinkedHashSet<>(categories));
        // approval-related fields are always decided by the platform
        shop.setStatus(ShopStatus.PENDING);
        shop.setStatusChangedAt(LocalDateTime.now());
        shop.setSelfRegistered(true);
        shop = shopRepository.save(shop);

        ShopPartnerDTO.OwnerSave owner = new ShopPartnerDTO.OwnerSave();
        owner.setFullName(req.getOwnerName());
        owner.setEmail(req.getOwnerEmail());
        owner.setMobile(req.getOwnerMobile());
        owner.setPassword(req.getPassword());
        ShopPartnerDTO.StaffDetail ownerDetail = partnerService.saveOwner(shop.getUuid(), owner, (Long) null);

        ShopRegistrationDTO.Registered result = new ShopRegistrationDTO.Registered();
        result.setShopUuid(shop.getUuid());
        result.setShopName(shop.getNameEn());
        result.setStatus(shop.getStatus());
        result.setLoginEmail(ownerDetail.getEmail());
        return result;
    }

    // =====================================================================
    // Admin
    // =====================================================================

    @Transactional
    public ShopMasterDTO.Detail changeStatus(String shopUuid, ShopRegistrationDTO.ApprovalUpdate req, SystemUser admin) {
        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(shopUuid)
                .orElseThrow(() -> new ResourceNotFoundException("shop.notfound"));

        ShopStatus target = req.getStatus();
        if (!TRANSITIONS.getOrDefault(shop.getStatus(), Set.of()).contains(target)) {
            throw new BadRequestException("approval.transition.invalid");
        }
        String reason = req.getReason() == null ? null : req.getReason().trim();
        boolean needsReason = target == ShopStatus.REJECTED || target == ShopStatus.SUSPENDED;
        if (needsReason && (reason == null || reason.isEmpty())) {
            throw new BadRequestException("approval.reason.required");
        }

        shop.setStatus(target);
        shop.setStatusReason(needsReason ? reason : null);
        shop.setStatusChangedAt(LocalDateTime.now());
        shop.setModifiedBy(admin.getRecordId());
        return mapper.toDetailDto(shopRepository.save(shop));
    }

    // =====================================================================
    // Shop login
    // =====================================================================

    /** A rejected shop fixed its details and asks for a new review. */
    @Transactional
    public ShopPartnerDTO.Session resubmit(SystemUser user) {
        ShopStaff staff = access.require(user, ShopActivity.PROFILE_EDIT);
        ShopMaster shop = staff.getShop();
        if (shop.getStatus() != ShopStatus.REJECTED) {
            throw new BadRequestException("store.resubmit.invalid");
        }
        shop.setStatus(ShopStatus.PENDING);
        shop.setStatusReason(null);
        shop.setStatusChangedAt(LocalDateTime.now());
        shop.setModifiedBy(user.getRecordId());
        shopRepository.save(shop);
        return access.session(staff);
    }
}
