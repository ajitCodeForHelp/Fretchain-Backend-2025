package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.VendorDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.VendorMaster;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.pojo.enums.VendorStatus;
import com.bt.fretchain.primary.repository.DriverRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.repository.VehicleRepository;
import com.bt.fretchain.primary.repository.VendorMasterRepository;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import com.bt.fretchain.utils.TextUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Vendors (fleet owners). The admin creates a vendor together with its VENDOR login;
 * the vendor then manages its own company profile, vehicles and drivers in the vendor panel.
 */
@Service
@RequiredArgsConstructor
public class VendorService {

    /** Allowed status changes through the approval action (same rules as shops). */
    private static final java.util.Map<VendorStatus, Set<VendorStatus>> TRANSITIONS = java.util.Map.of(
            VendorStatus.PENDING, java.util.EnumSet.of(VendorStatus.APPROVED, VendorStatus.REJECTED),
            VendorStatus.REJECTED, java.util.EnumSet.of(VendorStatus.APPROVED, VendorStatus.PENDING),
            VendorStatus.APPROVED, java.util.EnumSet.of(VendorStatus.SUSPENDED),
            VendorStatus.SUSPENDED, java.util.EnumSet.of(VendorStatus.APPROVED));

    private final VendorMasterRepository vendorRepository;
    private final SystemUserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final StoreLoginHelper loginHelper;

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public DataTableResponsePacket<VendorDTO.Detail> list(VendorDTO.ListOrSearchData req) {
        Specification<VendorMaster> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            String pattern = SearchUtils.containsPattern(req.getSearch());
            if (pattern != null) {
                char esc = SearchUtils.LIKE_ESCAPE;
                p.add(cb.or(
                        cb.like(cb.lower(root.get("companyName")), pattern, esc),
                        cb.like(cb.lower(root.get("vendorCode")), pattern, esc),
                        cb.like(cb.lower(root.get("contactPerson")), pattern, esc),
                        cb.like(cb.lower(root.get("mobile")), pattern, esc),
                        cb.like(cb.lower(root.get("user").get("email")), pattern, esc)));
            }
            if (req.getFilters() != null) {
                if (req.getFilters().getActive() != null) {
                    p.add(cb.equal(root.get("active"), req.getFilters().getActive()));
                }
                if (req.getFilters().getStatus() != null) {
                    if (req.getFilters().getStatus() == VendorStatus.APPROVED) {
                        p.add(cb.or(cb.isNull(root.get("status")), cb.equal(root.get("status"), VendorStatus.APPROVED)));
                    } else {
                        p.add(cb.equal(root.get("status"), req.getFilters().getStatus()));
                    }
                }
                if (req.getFilters().getCity() != null && !req.getFilters().getCity().isBlank()) {
                    p.add(cb.equal(cb.lower(root.get("city")), req.getFilters().getCity().trim().toLowerCase()));
                }
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PaginationUtils.createResponse(vendorRepository.findAll(spec, page(req)).map(this::toDetail));
    }

    /** key = vendor login uuid (what vehicles / drivers store), value = company name. */
    @Transactional(readOnly = true)
    public List<KeyValueDto> keyValue() {
        return vendorRepository.findByActiveTrueAndDeletedFalseOrderByCompanyNameAsc().stream()
                .filter(VendorMaster::isApproved)
                .map(v -> new KeyValueDto(v.getUser().getUuid(), v.getCompanyName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorDTO.Detail get(String uuid) {
        return toDetail(find(uuid));
    }

    @Transactional
    public VendorDTO.Detail save(VendorDTO.SaveOrUpdate req, SystemUser actor) {
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("password.required");
        }
        String name = req.getCompanyName().trim();
        if (vendorRepository.existsByCompanyNameIgnoreCaseAndDeletedFalse(name)) {
            throw new DuplicateResourceException("vendor.name.exists");
        }
        // the login's display name is the company name (shown on vehicles / drivers as "vendor")
        SystemUser user = loginHelper.create(name, req.getLoginEmail(), req.getLoginMobile(),
                req.getPassword(), UserTypeEnum.VENDOR, actor.getRecordId());

        VendorMaster v = new VendorMaster();
        v.setUser(user);
        applyProfile(v, req);
        v.setCommissionPercentage(req.getCommissionPercentage());
        v.setStatus(VendorStatus.APPROVED);
        v.setSelfRegistered(false);
        v.setCreatedBy(actor.getRecordId());
        v.setModifiedBy(actor.getRecordId());
        return toDetail(vendorRepository.save(v));
    }

    @Transactional
    public VendorDTO.Detail update(String uuid, VendorDTO.SaveOrUpdate req, SystemUser actor) {
        VendorMaster v = find(uuid);
        String name = req.getCompanyName().trim();
        if (vendorRepository.existsByCompanyNameIgnoreCaseAndDeletedFalseAndRecordIdNot(name, v.getRecordId())) {
            throw new DuplicateResourceException("vendor.name.exists");
        }
        loginHelper.update(v.getUser(), name, req.getLoginEmail(), req.getLoginMobile(),
                req.getPassword(), actor.getRecordId());
        applyProfile(v, req);
        v.setCommissionPercentage(req.getCommissionPercentage());
        v.setModifiedBy(actor.getRecordId());
        return toDetail(vendorRepository.save(v));
    }

    /** Deactivating a vendor also blocks its login. */
    @Transactional
    public void updateStatus(String uuid, boolean active, SystemUser actor) {
        VendorMaster v = find(uuid);
        v.setActive(active);
        v.setModifiedBy(actor.getRecordId());
        vendorRepository.save(v);
        loginHelper.setActive(v.getUser(), active, actor.getRecordId());
    }

    // ---------------------------------------------------------------- public sign-up and approval

    /** Public: a vendor signs up. It starts PENDING and can log in, but cannot add vehicles / drivers until approved. */
    @Transactional
    public VendorDTO.Detail register(VendorDTO.Register req) {
        String name = req.getCompanyName().trim();
        if (vendorRepository.existsByCompanyNameIgnoreCaseAndDeletedFalse(name)) {
            throw new DuplicateResourceException("vendor.name.exists");
        }
        SystemUser user = loginHelper.create(name, req.getLoginEmail(), req.getLoginMobile(),
                req.getPassword(), UserTypeEnum.VENDOR, null);
        VendorMaster v = new VendorMaster();
        v.setUser(user);
        applyProfile(v, req);
        v.setStatus(VendorStatus.PENDING);
        v.setSelfRegistered(true);
        v.setStatusChangedAt(LocalDateTime.now());
        return toDetail(vendorRepository.save(v));
    }

    @Transactional
    public VendorDTO.Detail changeStatus(String uuid, VendorDTO.ApprovalUpdate req, SystemUser admin) {
        VendorMaster v = find(uuid);
        VendorStatus current = v.getStatus() == null ? VendorStatus.APPROVED : v.getStatus();
        VendorStatus next = req.getStatus();
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new BadRequestException("approval.transition.invalid");
        }
        boolean needsReason = next == VendorStatus.REJECTED || next == VendorStatus.SUSPENDED;
        if (needsReason && (req.getReason() == null || req.getReason().isBlank())) {
            throw new BadRequestException("approval.reason.required");
        }
        v.setStatus(next);
        v.setStatusReason(needsReason ? req.getReason().trim() : null);
        v.setStatusChangedAt(LocalDateTime.now());
        v.setModifiedBy(admin.getRecordId());
        return toDetail(vendorRepository.save(v));
    }

    /** A rejected vendor fixed its details and asks for a new review. */
    @Transactional
    public VendorDTO.Session resubmit(SystemUser user) {
        VendorMaster v = current(user);
        if (v.getStatus() != VendorStatus.REJECTED) {
            throw new BadRequestException("store.resubmit.invalid");
        }
        v.setStatus(VendorStatus.PENDING);
        v.setStatusReason(null);
        v.setStatusChangedAt(LocalDateTime.now());
        vendorRepository.save(v);
        return session(user);
    }

    /** Fleet changes (vehicles, drivers) need an approved vendor. */
    @Transactional(readOnly = true)
    public VendorMaster currentApproved(SystemUser user) {
        VendorMaster v = current(user);
        if (!v.isApproved()) {
            throw new BadRequestException("vendor.notapproved");
        }
        return v;
    }

    // ---------------------------------------------------------------- vendor panel

    /** The vendor record of a VENDOR login, or 403. */
    @Transactional(readOnly = true)
    public VendorMaster current(SystemUser user) {
        if (user == null || user.getUserType() != UserTypeEnum.VENDOR) {
            throw new ForbiddenException("access.denied");
        }
        VendorMaster v = vendorRepository.findByUser_RecordIdAndDeletedFalse(user.getRecordId())
                .orElseThrow(() -> new ForbiddenException("access.denied"));
        if (!v.isActive()) {
            throw new ForbiddenException("vendor.inactive");
        }
        return v;
    }

    @Transactional(readOnly = true)
    public VendorDTO.Detail me(SystemUser user) {
        return toDetail(current(user));
    }

    /** The vendor edits its company details (not the login, commission or status). */
    @Transactional
    public VendorDTO.Detail updateMe(SystemUser user, VendorDTO.Profile req) {
        VendorMaster v = current(user);
        String name = req.getCompanyName().trim();
        if (vendorRepository.existsByCompanyNameIgnoreCaseAndDeletedFalseAndRecordIdNot(name, v.getRecordId())) {
            throw new DuplicateResourceException("vendor.name.exists");
        }
        applyProfile(v, req);
        v.setModifiedBy(user.getRecordId());
        SystemUser login = v.getUser();
        login.setFullName(name);
        login.setModifiedBy(user.getRecordId());
        userRepository.save(login);
        return toDetail(vendorRepository.save(v));
    }

    @Transactional
    public void changePassword(SystemUser user, VendorDTO.ChangePassword req) {
        VendorMaster v = current(user);
        SystemUser login = v.getUser();
        if (!TextUtils.matchPassword(req.getCurrentPassword(), login.getPassword())) {
            throw new BadRequestException("password.current.invalid");
        }
        login.setPassword(TextUtils.getEncodedPassword(req.getNewPassword()));
        login.setModifiedBy(user.getRecordId());
        userRepository.save(login);
    }

    @Transactional(readOnly = true)
    public VendorDTO.Session session(SystemUser user) {
        VendorMaster v = current(user);
        VendorDTO.Session s = new VendorDTO.Session();
        s.setVendorUuid(v.getUuid());
        s.setUserUuid(user.getUuid());
        s.setStatus(v.getStatus() == null ? VendorStatus.APPROVED : v.getStatus());
        s.setStatusReason(v.getStatusReason());
        s.setCompanyName(v.getCompanyName());
        s.setLogoUrl(v.getLogoUrl());
        s.setVehicleCount(vehicleRepository.countByVendor_UuidAndDeletedFalse(user.getUuid()));
        s.setDriverCount(driverRepository.countByVendor_UuidAndDeletedFalse(user.getUuid()));
        return s;
    }

    // ---------------------------------------------------------------- helpers

    private VendorMaster find(String uuid) {
        return vendorRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private static void applyProfile(VendorMaster v, VendorDTO.Profile req) {
        v.setVendorCode(blankToNull(req.getVendorCode()));
        v.setCompanyName(req.getCompanyName().trim());
        v.setContactPerson(blankToNull(req.getContactPerson()));
        v.setCountryCode(blankToNull(req.getCountryCode()));
        v.setMobile(blankToNull(req.getMobile()));
        v.setEmail(blankToNull(req.getEmail()));
        v.setAddress(blankToNull(req.getAddress()));
        v.setCity(blankToNull(req.getCity()));
        v.setState(blankToNull(req.getState()));
        v.setCountry(blankToNull(req.getCountry()));
        v.setPostalCode(blankToNull(req.getPostalCode()));
        v.setTaxNumber(blankToNull(req.getTaxNumber()));
        v.setRegistrationNumber(blankToNull(req.getRegistrationNumber()));
        v.setLogoUrl(blankToNull(req.getLogoUrl()));
    }

    private VendorDTO.Detail toDetail(VendorMaster v) {
        VendorDTO.Detail d = new VendorDTO.Detail();
        SystemUser u = v.getUser();
        d.setUuid(v.getUuid());
        d.setUserUuid(u.getUuid());
        d.setVendorCode(v.getVendorCode());
        d.setCompanyName(v.getCompanyName());
        d.setContactPerson(v.getContactPerson());
        d.setCountryCode(v.getCountryCode());
        d.setMobile(v.getMobile());
        d.setEmail(v.getEmail());
        d.setAddress(v.getAddress());
        d.setCity(v.getCity());
        d.setState(v.getState());
        d.setCountry(v.getCountry());
        d.setPostalCode(v.getPostalCode());
        d.setTaxNumber(v.getTaxNumber());
        d.setRegistrationNumber(v.getRegistrationNumber());
        d.setLogoUrl(v.getLogoUrl());
        d.setCommissionPercentage(v.getCommissionPercentage());
        d.setLoginEmail(u.getEmail());
        d.setLoginMobile(u.getMobile());
        d.setLastLogin(u.getLastLogin());
        d.setVehicleCount(vehicleRepository.countByVendor_UuidAndDeletedFalse(u.getUuid()));
        d.setDriverCount(driverRepository.countByVendor_UuidAndDeletedFalse(u.getUuid()));
        d.setStatus(v.getStatus() == null ? VendorStatus.APPROVED : v.getStatus());
        d.setStatusReason(v.getStatusReason());
        d.setSelfRegistered(Boolean.TRUE.equals(v.getSelfRegistered()));
        d.setActive(v.isActive());
        d.setCreatedAt(v.getCreatedAt());
        return d;
    }

    private static Pageable page(VendorDTO.ListOrSearchData req) {
        Set<String> allowed = Set.of("companyName", "city", "createdAt");
        String field = "createdAt";
        Sort.Direction dir = Sort.Direction.DESC;
        if (req.getSort() != null && req.getSort().getField() != null && allowed.contains(req.getSort().getField())) {
            field = req.getSort().getField();
            dir = "desc".equalsIgnoreCase(req.getSort().getDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        }
        int n = Math.max(0, req.getPageNumber() == null ? 0 : req.getPageNumber());
        int size = req.getPageSize() == null ? 10 : Math.max(1, Math.min(100, req.getPageSize()));
        return PageRequest.of(n, size, Sort.by(dir, field));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
