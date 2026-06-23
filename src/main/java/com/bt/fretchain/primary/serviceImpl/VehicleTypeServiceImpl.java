package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.VehicleTypeDTO;
import com.bt.fretchain.primary.mapper.VehicleTypeMapper;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.VehicleType;
import com.bt.fretchain.primary.repository.VehicleTypeRepository;
import com.bt.fretchain.primary.service.VehicleTypeService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;

    @Override
    public VehicleTypeDTO.Detail save(
            VehicleTypeDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateDuplicate(request);

        VehicleType entity = vehicleTypeMapper.toEntity(request);

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        VehicleType saved = vehicleTypeRepository.save(entity);

        return vehicleTypeMapper.toDetailDto(saved);
    }

    @Override
    public VehicleTypeDTO.Detail update(
            String uuid,
            VehicleTypeDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        VehicleType entity = vehicleTypeRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        validateDuplicateForUpdate(request, entity);

        vehicleTypeMapper.updateEntityFromDto(request, entity);

        entity.setModifiedBy(user.getRecordId());

        return vehicleTypeMapper.toDetailDto(vehicleTypeRepository.save(entity));
    }

    @Override
    public VehicleTypeDTO.Detail getById(String uuid) {

        VehicleType entity = vehicleTypeRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        return vehicleTypeMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {
        VehicleType entity = vehicleTypeRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        vehicleTypeRepository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {

        return vehicleTypeRepository
                .findByActiveTrueAndDeletedFalseOrderBySortOrderAsc()
                .stream()
                .map(v -> new KeyValueDto(
                        v.getUuid(),
                        "fr".equalsIgnoreCase(lang)
                                ? v.getNameFr()
                                : v.getNameEn()
                ))
                .toList();
    }

    @Override
    public DataTableResponsePacket<VehicleTypeDTO.Detail> getListData(
            VehicleTypeDTO.ListOrSearchData request
    ) {
        Pageable pageable = pageRequestCreate(request);

        Specification<VehicleType> specification = createFilterOrSearch(request);

        Page<VehicleType> page = vehicleTypeRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(vehicleTypeMapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            VehicleTypeDTO.ListOrSearchData request
    ) {
        Sort sort = Sort.by(Sort.Direction.ASC, "sortOrder");

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

    private Specification<VehicleType> createFilterOrSearch(
            VehicleTypeDTO.ListOrSearchData request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleted"), false));

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search = "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("code")), search),
                                cb.like(cb.lower(root.get("nameEn")), search),
                                cb.like(cb.lower(root.get("nameFr")), search)
                        )
                );
            }

            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {

                predicates.add(
                        cb.equal(root.get("active"), request.getFilters().getActive())
                );
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

    private void validateDuplicate(VehicleTypeDTO.SaveOrUpdate request) {

        vehicleTypeRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(request.getNameEn().trim())
                .ifPresent(v -> {
                    throw new DuplicateResourceException("record.name.en.exists");
                });

        vehicleTypeRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(request.getNameFr().trim())
                .ifPresent(v -> {
                    throw new DuplicateResourceException("record.name.fr.exists");
                });
    }

    private void validateDuplicateForUpdate(
            VehicleTypeDTO.SaveOrUpdate request,
            VehicleType entity
    ) {
        vehicleTypeRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(request.getNameEn().trim())
                .ifPresent(v -> {
                    if (!v.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("record.name.en.exists");
                    }
                });

        vehicleTypeRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(request.getNameFr().trim())
                .ifPresent(v -> {
                    if (!v.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("record.name.fr.exists");
                    }
                });
    }
}