package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.mapper.DriverMapper;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.DriverDocument;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.Vehicle;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.VehicleRepository;
import com.bt.fretchain.primary.repository.DriverRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.service.DriverService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final SystemUserRepository systemUserRepository;
    private final DriverMapper driverMapper;
    private final PasswordEncoder passwordEncoder;
    private final VehicleRepository vehicleRepository;

    /** Admin-created drivers are trusted: verified at once. */
    @Override
    public DriverDTO.Detail save(
            DriverDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        return doSave(request, user, true);
    }

    @Override
    public DriverDTO.Detail saveUnverified(
            DriverDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        return doSave(request, user, false);
    }

    @Override
    public void updateVerification(String uuid, Boolean verified, SystemUser user) {
        Driver entity = driverRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
        entity.review(Boolean.TRUE.equals(verified)
                        ? com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED
                        : com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.PENDING,
                null, reviewerName(user));
        entity.setModifiedBy(actorId(user));
        driverRepository.save(entity);
    }

    @Override
    public DriverDTO.Detail review(String uuid, com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus status,
                                   String reason, SystemUser user) {
        Driver entity = driverRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
        if (status == null) {
            throw new com.bt.fretchain.exception.BadRequestException("driver.review.status.required");
        }
        String why = reason == null ? null : reason.trim();
        if (status == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.REJECTED && (why == null || why.isEmpty())) {
            throw new com.bt.fretchain.exception.BadRequestException("driver.review.reason.required");
        }
        entity.review(status, why, reviewerName(user));
        entity.setModifiedBy(actorId(user));
        driverRepository.save(entity);
        return driverMapper.toDetailDto(entity);
    }

    @Override
    public void resubmitIfRejected(String uuid) {
        driverRepository.findByUuidAndDeletedFalse(uuid).ifPresent(d -> {
            if (d.getVerificationStatus() == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.REJECTED) {
                d.review(com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.PENDING, null, null);
                driverRepository.save(d);
            }
        });
    }

    private static String reviewerName(SystemUser user) {
        return user == null ? null : user.getFullName();
    }

    /** user is null for public sign-ups. */
    private static Long actorId(SystemUser user) {
        return user == null ? null : user.getRecordId();
    }

    private DriverDTO.Detail doSave(
            DriverDTO.SaveOrUpdate request,
            SystemUser user,
            boolean verified
    ) {

        validateDuplicate(request);

        Driver entity = driverMapper.toEntity(request);

        entity.setJoiningDate(LocalDateTime.now());

        setRelations(entity, request);

        setEncodedPassword(
                entity,
                request.getPassword()
        );

        entity.review(verified
                        ? com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED
                        : com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.PENDING,
                null, verified ? reviewerName(user) : null);
        entity.setCreatedBy(actorId(user));
        entity.setModifiedBy(actorId(user));

        entity.setDriverDocument(
                buildDriverDocument(
                        request,
                        entity,
                        null,
                        user
                )
        );

        Driver saved = driverRepository.save(entity);

        return driverMapper.toDetailDto(saved);
    }

    @Override
    public DriverDTO.Detail update(
            String uuid,
            DriverDTO.SaveOrUpdate request,
            SystemUser user
    ) {

        Driver entity = driverRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound")
                );

        validateDuplicateForUpdate(request, entity);

        driverMapper.updateEntityFromDto(request, entity);

        setRelations(entity, request);

        setEncodedPassword(
                entity,
                request.getPassword()
        );

        entity.setModifiedBy(user.getRecordId());

        entity.setDriverDocument(
                buildDriverDocument(
                        request,
                        entity,
                        entity.getDriverDocument(),
                        user
                )
        );

        Driver updated = driverRepository.save(entity);

        return driverMapper.toDetailDto(updated);
    }
    private void setEncodedPassword(
            Driver entity,
            String password
    ) {

        if (password != null
                && !password.isBlank()) {

            entity.setPassword(
                    passwordEncoder.encode(password)
            );
        }
    }


    private DriverDocument buildDriverDocument(
            DriverDTO.SaveOrUpdate request,
            Driver driver,
            DriverDocument document,
            SystemUser user
    ) {

        if (request.getDriverDocument() == null) {
            return document;   // documents not sent: keep what is saved
        }

        if (document == null) {

            document = new DriverDocument();

            document.setDriver(driver);

            document.setCreatedBy(actorId(user));
        }

        document.setIdentityNumber(
                request.getDriverDocument().getIdentityNumber()
        );

        document.setIdentityDocumentUrl(
                request.getDriverDocument().getIdentityDocumentUrl()
        );

        document.setLicenseImageUrl(
                request.getDriverDocument().getLicenseImageUrl()
        );

        document.setPoliceVerificationUrl(
                request.getDriverDocument().getPoliceVerificationUrl()
        );

        document.setAddressProofUrl(
                request.getDriverDocument().getAddressProofUrl()
        );

        document.setContractDocumentUrl(
                request.getDriverDocument().getContractDocumentUrl()
        );

        document.setMedicalCertificateUrl(
                request.getDriverDocument().getMedicalCertificateUrl()
        );

//        document.setLicenseValidity(
//                request.getDocumentLicenseValidity()
//        );

        document.setReferenceName(
                request.getDriverDocument().getReferenceName()
        );

        document.setReferenceMobile(
                request.getDriverDocument().getReferenceMobile()
        );

        document.setModifiedBy(actorId(user));

        return document;
    }

    @Override
    public DriverDTO.Detail getById(String uuid) {

        Driver entity = driverRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound")
                );

        return driverMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {

        Driver entity = driverRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound")
                );

        entity.setActive(active);

        entity.setModifiedBy(user.getRecordId());

        driverRepository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String vendorUuid) {
        if (vendorUuid == null || vendorUuid.isBlank()) {
            return getKeyValueList();
        }
        return driverRepository
                .findByVendor_UuidAndActiveTrueAndDeletedFalseOrderByFullNameAsc(vendorUuid)
                .stream()
                .map(d -> new KeyValueDto(d.getUuid(), d.getFullName()))
                .toList();
    }

    /**
     * Fleet owner and vehicle of the driver:
     *  - the vendor must be a VENDOR login
     *  - the vehicle must be active, belong to the same vendor (or both none) and have no other driver
     */
    private void setRelations(Driver entity, DriverDTO.SaveOrUpdate request) {
        SystemUser vendor = null;
        if (request.getVendorUuid() != null && !request.getVendorUuid().isBlank()) {
            vendor = systemUserRepository.findByUuidAndDeletedFalse(request.getVendorUuid())
                    .filter(u -> u.getUserType() == UserTypeEnum.VENDOR)
                    .orElseThrow(() -> new ResourceNotFoundException("vendor.notfound"));
        }
        entity.setVendor(vendor);

        Vehicle vehicle = null;
        if (request.getVehicleUuid() != null && !request.getVehicleUuid().isBlank()) {
            vehicle = vehicleRepository.findByUuidAndDeletedFalse(request.getVehicleUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("vehicle.notfound"));
            boolean sameVehicle = entity.getVehicle() != null
                    && entity.getVehicle().getRecordId().equals(vehicle.getRecordId());
            if (!vehicle.isActive() && !sameVehicle) {
                throw new BadRequestException("vehicle.inactive");
            }
            String vehicleVendor = vehicle.getVendor() == null ? null : vehicle.getVendor().getUuid();
            String driverVendor = vendor == null ? null : vendor.getUuid();
            if (!Objects.equals(vehicleVendor, driverVendor)) {
                throw new BadRequestException("driver.vehicle.vendor.mismatch");
            }
            Long vehicleId = vehicle.getRecordId();
            driverRepository.findFirstByVehicle_RecordIdAndDeletedFalse(vehicleId)
                    .filter(other -> entity.getRecordId() == null || !other.getRecordId().equals(entity.getRecordId()))
                    .ifPresent(other -> {
                        throw new DuplicateResourceException("vehicle.driver.assigned");
                    });
        }
        entity.setVehicle(vehicle);
    }

    @Override
    public List<KeyValueDto> getKeyValueList() {

        return driverRepository
                .findByActiveTrueAndDeletedFalseOrderByFullNameAsc()
                .stream()
                .map(driver -> new KeyValueDto(
                        driver.getUuid(),
                        driver.getFullName()
                ))
                .toList();
    }

    @Override
    public DataTableResponsePacket<DriverDTO.Detail> getListData(
            DriverDTO.ListOrSearchData request
    ) {

        Pageable pageable = createPageable(request);

        Specification<Driver> specification =
                createFilterOrSearch(request);

        Page<Driver> page =
                driverRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(driverMapper::toDetailDto)
        );
    }

    // =====================================================
    // PRIVATE METHODS
    // =====================================================

    private void validateDuplicate(
            DriverDTO.SaveOrUpdate request
    ) {

        driverRepository
                .findByMobileAndDeletedFalse(request.getMobile())
                .ifPresent(driver -> {
                    throw new DuplicateResourceException(
                            "driver.mobile.exists"
                    );
                });

        if (request.getUsername() != null
                && !request.getUsername().isBlank()) {

            driverRepository
                    .findByUsernameAndDeletedFalse(
                            request.getUsername()
                    )
                    .ifPresent(driver -> {
                        throw new DuplicateResourceException(
                                "driver.username.exists"
                        );
                    });
        }
    }

    private void validateDuplicateForUpdate(
            DriverDTO.SaveOrUpdate request,
            Driver entity
    ) {

        driverRepository
                .findByMobileAndDeletedFalse(request.getMobile())
                .ifPresent(driver -> {

                    if (!driver.getRecordId()
                            .equals(entity.getRecordId())) {

                        throw new DuplicateResourceException(
                                "driver.mobile.exists"
                        );
                    }
                });

        if (request.getUsername() != null
                && !request.getUsername().isBlank()) {

            driverRepository
                    .findByUsernameAndDeletedFalse(
                            request.getUsername()
                    )
                    .ifPresent(driver -> {

                        if (!driver.getRecordId()
                                .equals(entity.getRecordId())) {

                            throw new DuplicateResourceException(
                                    "driver.username.exists"
                            );
                        }
                    });
        }
    }

    private Pageable createPageable(
            DriverDTO.ListOrSearchData request
    ) {

        Sort sort = Sort.by(
                Sort.Direction.DESC,
                "createdAt"
        );

        if (request.getSort() != null
                && request.getSort().getField() != null
                && !request.getSort().getField().isBlank()) {

            Sort.Direction direction =
                    "asc".equalsIgnoreCase(
                            request.getSort().getDirection()
                    )
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            sort = Sort.by(
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

    private Specification<Driver> createFilterOrSearch(
            DriverDTO.ListOrSearchData request
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    cb.equal(root.get("deleted"), false)
            );

            // SEARCH

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search =
                        "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("fullName")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("mobile")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("username")),
                                        search
                                )
                        )
                );
            }

            // FILTERS

            if (request.getFilters() != null) {

                if (request.getFilters().getActive() != null) {

                    predicates.add(
                            cb.equal(
                                    root.get("active"),
                                    request.getFilters().getActive()
                            )
                    );
                }

                if (request.getFilters().getVerified() != null) {

                    predicates.add(
                            cb.equal(
                                    root.get("verified"),
                                    request.getFilters().getVerified()
                            )
                    );
                }

                if (request.getFilters().getVerificationStatus() != null) {
                    var st = request.getFilters().getVerificationStatus();
                    if (st == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.REJECTED) {
                        predicates.add(cb.equal(root.get("verificationStatus"), st));
                    } else if (st == com.bt.fretchain.primary.pojo.enums.DriverVerificationStatus.VERIFIED) {
                        predicates.add(cb.equal(root.get("verified"), true));
                    } else {
                        // pending = not verified and not rejected (older rows have no stored status)
                        predicates.add(cb.equal(root.get("verified"), false));
                        predicates.add(cb.or(cb.isNull(root.get("verificationStatus")),
                                cb.equal(root.get("verificationStatus"), st)));
                    }
                }

                if (request.getFilters().getAvailable() != null) {

                    predicates.add(
                            cb.equal(
                                    root.get("available"),
                                    request.getFilters().getAvailable()
                            )
                    );
                }

                String vehicleUuid = request.getFilters().getVehicleUuid() != null
                        ? request.getFilters().getVehicleUuid()
                        : request.getFilters().getTruckUuid();
                if (vehicleUuid != null && !vehicleUuid.isBlank()) {
                    predicates.add(cb.equal(root.get("vehicle").get("uuid"), vehicleUuid));
                }

                if (request.getFilters().getVendorUuid() != null
                        && !request.getFilters().getVendorUuid().isBlank()) {
                    predicates.add(cb.equal(root.get("vendor").get("uuid"), request.getFilters().getVendorUuid()));
                }
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}