package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.CategoryDTO;
import com.bt.fretchain.primary.mapper.CategoryMapper;
import com.bt.fretchain.primary.pojo.Category;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.CategoryRepository;
import com.bt.fretchain.primary.service.CategoryService;
import com.bt.fretchain.utils.PaginationUtils;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryDTO.Detail save(
            CategoryDTO.SaveOrUpdate request,
            SystemUser user
    ) {

        validateDuplicate(request);

        Category entity = categoryMapper.toEntity(request);
        if (entity.getOrderType() == null) {
            // the column is mandatory: new categories are freight CONTAINER unless the admin picks SHOP / FOOD ...
            entity.setOrderType(com.bt.fretchain.primary.pojo.enums.FreightOrderType.CONTAINER);
        }

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        Category saved = categoryRepository.save(entity);

        return categoryMapper.toDetailDto(saved);
    }

    @Override
    public CategoryDTO.Detail update(
            String uuid,
            CategoryDTO.SaveOrUpdate request,
            SystemUser user
    ) {

        Category entity = categoryRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound")
                );

        validateDuplicateForUpdate(request, entity);

        com.bt.fretchain.primary.pojo.enums.FreightOrderType keepType = entity.getOrderType();
        categoryMapper.updateEntityFromDto(request, entity);
        if (entity.getOrderType() == null) {
            entity.setOrderType(keepType);      // not sent: keep the current type
        }

        entity.setModifiedBy(user.getRecordId());

        return categoryMapper.toDetailDto(categoryRepository.save(entity));
    }

    @Override
    public CategoryDTO.Detail getById(String id) {

        Category entity = categoryRepository.findByUuid(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound")
                );
        return categoryMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {

        Category entity = categoryRepository
                .findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound")
                );

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        categoryRepository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {

        return categoryRepository
                .findByActiveTrueAndDeletedFalseOrderByNameEnAsc()
                .stream()
                .map(c -> new KeyValueDto(
                        c.getUuid(),
                        "fr".equalsIgnoreCase(lang)
                                ? c.getNameFr()
                                : c.getNameEn()
                ))
                .toList();
    }
    private void validateDuplicate(CategoryDTO.SaveOrUpdate request) {

        categoryRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(request.getNameEn().trim())
                .ifPresent(c -> {
                    throw new DuplicateResourceException("record.name.en.exists");
                });

        categoryRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(request.getNameFr().trim())
                .ifPresent(c -> {
                    throw new DuplicateResourceException("record.name.fr.exists");
                });
    }

    private void validateDuplicateForUpdate(
            CategoryDTO.SaveOrUpdate request,
            Category entity
    ) {

        categoryRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(request.getNameEn().trim())
                .ifPresent(c -> {
                    if (!c.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("record.name.en.exists");
                    }
                });

        categoryRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(request.getNameFr().trim())
                .ifPresent(c -> {
                    if (!c.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("record.name.fr.exists");
                    }
                });
    }

    @Override
    public DataTableResponsePacket<CategoryDTO.Detail> getListData(
            CategoryDTO.ListOrSearchData request
    ) {

        Pageable pageable = pageRequestCreate(request);

        Specification<Category> specification = createFilterOrSearch(request);

        Page<Category> page = categoryRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(categoryMapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            CategoryDTO.ListOrSearchData request
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

    private Specification<Category> createFilterOrSearch(
            CategoryDTO.ListOrSearchData request
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // soft delete filter
            predicates.add(cb.equal(root.get("deleted"), false));

            // ======================
            // SEARCH (EN + FR)
            // ======================
            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search = "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("nameEn")), search),
                                cb.like(cb.lower(root.get("nameFr")), search)
                        )
                );
            }

            // ======================
            // ACTIVE FILTER
            // ======================
            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {

                predicates.add(
                        cb.equal(root.get("active"), request.getFilters().getActive())
                );
            }

            // ======================
            // DATE FILTER
            // ======================
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}