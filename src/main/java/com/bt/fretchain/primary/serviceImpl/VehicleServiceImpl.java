package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.VehicleDTO;
import com.bt.fretchain.primary.mapper.VehicleMapper;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.Vehicle;
import com.bt.fretchain.primary.pojo.VehicleCategory;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.repository.VehicleCategoryRepository;
import com.bt.fretchain.primary.repository.VehicleRepository;
import com.bt.fretchain.primary.service.VehicleService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final SystemUserRepository systemUserRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    public VehicleDTO.Detail save(
            VehicleDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateDuplicate(request);

        Vehicle entity = vehicleMapper.toEntity(request);

        setRelations(entity, request);

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        Vehicle saved = vehicleRepository.save(entity);

        return vehicleMapper.toDetailDto(saved);
    }

    @Override
    public VehicleDTO.Detail update(
            String uuid,
            VehicleDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        Vehicle entity = vehicleRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        validateDuplicateForUpdate(request, entity);

        vehicleMapper.updateEntityFromDto(request, entity);

        setRelations(entity, request);

        entity.setModifiedBy(user.getRecordId());

        return vehicleMapper.toDetailDto(vehicleRepository.save(entity));
    }

    @Override
    public VehicleDTO.Detail getById(String uuid) {

        Vehicle entity = vehicleRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        return vehicleMapper.toDetailDto(entity);
    }

    @Override
    public DataTableResponsePacket<VehicleDTO.Detail> getListData(
            VehicleDTO.ListOrSearchData request
    ) {
        Pageable pageable = pageRequestCreate(request);

        Specification<Vehicle> specification = createFilterOrSearch(request);

        Page<Vehicle> page = vehicleRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(vehicleMapper::toDetailDto)
        );
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {
        Vehicle entity = vehicleRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        vehicleRepository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList() {

        return vehicleRepository
                .findByActiveTrueAndDeletedFalseOrderByVehicleNumberAsc()
                .stream()
                .map(v -> new KeyValueDto(
                        v.getUuid(),
                        v.getVehicleNumber()
                ))
                .toList();
    }

    private void setRelations(
            Vehicle entity,
            VehicleDTO.SaveOrUpdate request
    ) {
        VehicleCategory vehicleCategory = vehicleCategoryRepository
                .findByUuidAndDeletedFalse(request.getVehicleCategoryUuid())
                .orElseThrow(() -> new ResourceNotFoundException("vehicle.category.notfound"));

        entity.setVehicleCategory(vehicleCategory);

        if (request.getVendorUuid() != null
                && !request.getVendorUuid().isBlank()) {

            SystemUser vendor = systemUserRepository
                    .findByUuidAndDeletedFalse(request.getVendorUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("vendor.notfound"));

            entity.setVendor(vendor);
        } else {
            entity.setVendor(null);
        }
    }

    private Pageable pageRequestCreate(
            VehicleDTO.ListOrSearchData request
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        if (request.getSort() != null
                && request.getSort().getField() != null
                && !request.getSort().getField().isBlank()) {

            Sort.Direction direction =
                    "asc".equalsIgnoreCase(request.getSort().getDirection())
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            sort = Sort.by(direction, request.getSort().getField());
        }

        return PageRequest.of(
                request.getPageNumber(),
                request.getPageSize(),
                sort
        );
    }

    private Specification<Vehicle> createFilterOrSearch(
            VehicleDTO.ListOrSearchData request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            root.fetch("vehicleCategory", JoinType.LEFT);
            root.fetch("vendor", JoinType.LEFT);
            root.fetch("driver", JoinType.LEFT);

            query.distinct(true);

            predicates.add(cb.equal(root.get("deleted"), false));

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search = "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("vehicleNumber")), search),
                                cb.like(cb.lower(root.get("rcNumber")), search),
                                cb.like(cb.lower(root.get("fitnessCertificateNumber")), search),
                                cb.like(cb.lower(root.get("chassisNumber")), search)
                        )
                );
            }

            if (request.getFilters() != null) {

                if (request.getFilters().getActive() != null) {
                    predicates.add(
                            cb.equal(root.get("active"), request.getFilters().getActive())
                    );
                }

                if (request.getFilters().getVerified() != null) {
                    predicates.add(
                            cb.equal(root.get("verified"), request.getFilters().getVerified())
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

                if (request.getFilters().getVendorUuid() != null
                        && !request.getFilters().getVendorUuid().isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("vendor").get("uuid"),
                                    request.getFilters().getVendorUuid()
                            )
                    );
                }
            }

            if (request.getDateFilter() != null) {

                if (request.getDateFilter().getFrom() != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(
                                    root.get("createdAt"),
                                    request.getDateFilter().getFrom().atStartOfDay()
                            )
                    );
                }

                if (request.getDateFilter().getTo() != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(
                                    root.get("createdAt"),
                                    request.getDateFilter().getTo().atTime(23, 59, 59)
                            )
                    );
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateDuplicate(
            VehicleDTO.SaveOrUpdate request
    ) {
        vehicleRepository
                .findByVehicleNumberIgnoreCaseAndDeletedFalse(
                        request.getVehicleNumber().trim()
                )
                .ifPresent(v -> {
                    throw new DuplicateResourceException("vehicle.number.exists");
                });

        if (request.getRcNumber() != null
                && !request.getRcNumber().isBlank()) {

            vehicleRepository
                    .findByRcNumberIgnoreCaseAndDeletedFalse(
                            request.getRcNumber().trim()
                    )
                    .ifPresent(v -> {
                        throw new DuplicateResourceException("vehicle.rc.number.exists");
                    });
        }
    }

    private void validateDuplicateForUpdate(
            VehicleDTO.SaveOrUpdate request,
            Vehicle entity
    ) {
        vehicleRepository
                .findByVehicleNumberIgnoreCaseAndDeletedFalse(
                        request.getVehicleNumber().trim()
                )
                .ifPresent(v -> {
                    if (!v.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("vehicle.number.exists");
                    }
                });

        if (request.getRcNumber() != null
                && !request.getRcNumber().isBlank()) {

            vehicleRepository
                    .findByRcNumberIgnoreCaseAndDeletedFalse(
                            request.getRcNumber().trim()
                    )
                    .ifPresent(v -> {
                        if (!v.getRecordId().equals(entity.getRecordId())) {
                            throw new DuplicateResourceException("vehicle.rc.number.exists");
                        }
                    });
        }
    }
}