package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.VehicleCategoryDTO;
import com.bt.fretchain.primary.mapper.VehicleCategoryMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.repository.CategoryRepository;
import com.bt.fretchain.primary.repository.SubCategoryRepository;
import com.bt.fretchain.primary.repository.VehicleCategoryRepository;
import com.bt.fretchain.primary.repository.VehicleTypeRepository;
import com.bt.fretchain.primary.service.VehicleCategoryService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleCategoryServiceImpl implements VehicleCategoryService {

    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final VehicleCategoryMapper vehicleCategoryMapper;

    @Override
    public VehicleCategoryDTO.Detail save(
            VehicleCategoryDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateDuplicate(request);

        VehicleCategory entity = vehicleCategoryMapper.toEntity(request);

        setRelations(entity, request);

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        VehicleCategory saved = vehicleCategoryRepository.save(entity);

        return vehicleCategoryMapper.toDetailDto(saved);
    }

    @Override
    public VehicleCategoryDTO.Detail update(
            String uuid,
            VehicleCategoryDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        VehicleCategory entity = vehicleCategoryRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        validateDuplicateForUpdate(request, entity);

        vehicleCategoryMapper.updateEntityFromDto(request, entity);

        setRelations(entity, request);

        entity.setModifiedBy(user.getRecordId());

        return vehicleCategoryMapper.toDetailDto(vehicleCategoryRepository.save(entity));
    }

    @Override
    public VehicleCategoryDTO.Detail getById(String uuid) {

        VehicleCategory entity = vehicleCategoryRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        return vehicleCategoryMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {
        VehicleCategory entity = vehicleCategoryRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        vehicleCategoryRepository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {

        return vehicleCategoryRepository
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
    public DataTableResponsePacket<VehicleCategoryDTO.Detail> getListData(
            VehicleCategoryDTO.ListOrSearchData request
    ) {
        Pageable pageable = pageRequestCreate(request);

        Specification<VehicleCategory> specification = createFilterOrSearch(request);

        Page<VehicleCategory> page =
                vehicleCategoryRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(vehicleCategoryMapper::toDetailDto)
        );
    }

    private void setRelations(
            VehicleCategory entity,
            VehicleCategoryDTO.SaveOrUpdate request
    ) {
        VehicleType vehicleType = vehicleTypeRepository
                .findByUuidAndDeletedFalse(request.getVehicleTypeUuid())
                .orElseThrow(() -> new ResourceNotFoundException("vehicle.type.notfound"));

        entity.setVehicleType(vehicleType);

        if (request.getCategoryUuid() != null
                && !request.getCategoryUuid().isBlank()) {

            Category category = categoryRepository
                    .findByUuidAndDeletedFalse(request.getCategoryUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("category.notfound"));

            entity.setCategory(category);
        } else {
            entity.setCategory(null);
        }

        if (request.getSubCategoryUuid() != null
                && !request.getSubCategoryUuid().isBlank()) {

            SubCategory subCategory = subCategoryRepository
                    .findByUuidAndDeletedFalse(request.getSubCategoryUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("subcategory.notfound"));

            entity.setSubCategory(subCategory);
        } else {
            entity.setSubCategory(null);
        }
    }

    private Pageable pageRequestCreate(
            VehicleCategoryDTO.ListOrSearchData request
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

    private Specification<VehicleCategory> createFilterOrSearch(
            VehicleCategoryDTO.ListOrSearchData request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            root.fetch("vehicleType", JoinType.LEFT);
            root.fetch("category", JoinType.LEFT);
            root.fetch("subCategory", JoinType.LEFT);

            query.distinct(true);

            predicates.add(cb.equal(root.get("deleted"), false));

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search = "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("nameEn")), search),
                                cb.like(cb.lower(root.get("nameFr")), search),
                                cb.like(cb.lower(root.get("subTitleEn")), search),
                                cb.like(cb.lower(root.get("subTitleFr")), search)
                        )
                );
            }

            if (request.getFilters() != null) {

                if (request.getFilters().getActive() != null) {
                    predicates.add(
                            cb.equal(root.get("active"), request.getFilters().getActive())
                    );
                }

                if (request.getFilters().getVehicleTypeUuid() != null
                        && !request.getFilters().getVehicleTypeUuid().isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("vehicleType").get("uuid"),
                                    request.getFilters().getVehicleTypeUuid()
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
            VehicleCategoryDTO.SaveOrUpdate request
    ) {
        vehicleCategoryRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(request.getNameEn().trim())
                .ifPresent(v -> {
                    throw new DuplicateResourceException("record.name.en.exists");
                });

        vehicleCategoryRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(request.getNameFr().trim())
                .ifPresent(v -> {
                    throw new DuplicateResourceException("record.name.fr.exists");
                });
    }

    private void validateDuplicateForUpdate(
            VehicleCategoryDTO.SaveOrUpdate request,
            VehicleCategory entity
    ) {
        vehicleCategoryRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(request.getNameEn().trim())
                .ifPresent(v -> {
                    if (!v.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("record.name.en.exists");
                    }
                });

        vehicleCategoryRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(request.getNameFr().trim())
                .ifPresent(v -> {
                    if (!v.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("record.name.fr.exists");
                    }
                });
    }
}