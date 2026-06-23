package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.mapper.DriverMapper;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.DriverDocument;
import com.bt.fretchain.primary.pojo.SystemUser;
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

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final SystemUserRepository systemUserRepository;
    private final DriverMapper driverMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public DriverDTO.Detail save(
            DriverDTO.SaveOrUpdate request,
            SystemUser user
    ) {

        validateDuplicate(request);

        Driver entity = driverMapper.toEntity(request);

        entity.setJoiningDate(LocalDateTime.now());

        setEncodedPassword(
                entity,
                request.getPassword()
        );

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

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

        if (document == null) {

            document = new DriverDocument();

            document.setDriver(driver);

            document.setCreatedBy(user.getRecordId());
        }

        document.setIdentityNumber(
                request.getIdentityNumber()
        );

        document.setIdentityDocumentUrl(
                request.getIdentityDocumentUrl()
        );

        document.setLicenseImageUrl(
                request.getLicenseImageUrl()
        );

        document.setPoliceVerificationUrl(
                request.getPoliceVerificationUrl()
        );

        document.setAddressProofUrl(
                request.getAddressProofUrl()
        );

        document.setContractDocumentUrl(
                request.getContractDocumentUrl()
        );

        document.setMedicalCertificateUrl(
                request.getMedicalCertificateUrl()
        );

        document.setLicenseValidity(
                request.getDocumentLicenseValidity()
        );

        document.setReferenceName(
                request.getReferenceName()
        );

        document.setReferenceMobile(
                request.getReferenceMobile()
        );

        document.setModifiedBy(user.getRecordId());

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

                if (request.getFilters().getAvailable() != null) {

                    predicates.add(
                            cb.equal(
                                    root.get("available"),
                                    request.getFilters().getAvailable()
                            )
                    );
                }

                if (request.getFilters().getTruckUuid() != null
                        && !request.getFilters()
                        .getTruckUuid()
                        .isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("truck").get("uuid"),
                                    request.getFilters().getTruckUuid()
                            )
                    );
                }
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}