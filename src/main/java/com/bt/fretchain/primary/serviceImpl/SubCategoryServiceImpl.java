package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.SubCategoryDTO;
import com.bt.fretchain.primary.mapper.SubCategoryMapper;
import com.bt.fretchain.primary.pojo.Category;
import com.bt.fretchain.primary.pojo.SubCategory;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.CategoryRepository;
import com.bt.fretchain.primary.repository.SubCategoryRepository;
import com.bt.fretchain.primary.service.SubCategoryService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl
        implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    private final CategoryRepository categoryRepository;

    private final SubCategoryMapper subCategoryMapper;

    @Override
    public SubCategoryDTO.Detail save(SubCategoryDTO.SaveOrUpdate request, SystemUser user) {

        validateDuplicate(request);

        Category category =
                categoryRepository.findByUuidAndDeletedFalse(
                                request.getCategoryUuid()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "record.notfound"
                                ));

        SubCategory entity =
                subCategoryMapper.toEntity(request);

        entity.setCategory(category);

        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        SubCategory saved =
                subCategoryRepository.save(entity);

        return subCategoryMapper.toDetailDto(saved);
    }

    @Override
    public SubCategoryDTO.Detail update(String uuid, SubCategoryDTO.SaveOrUpdate request, SystemUser user) {

        SubCategory entity =
                subCategoryRepository
                        .findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "record.notfound"
                                ));

        validateDuplicateForUpdate(request, entity);

        Category category =
                categoryRepository.findByUuidAndDeletedFalse(
                                request.getCategoryUuid()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "record.notfound"
                                ));

        subCategoryMapper.updateEntityFromDto(
                request,
                entity
        );

        entity.setCategory(category);

        entity.setModifiedBy(user.getRecordId());

        return subCategoryMapper.toDetailDto(
                subCategoryRepository.save(entity)
        );
    }

    @Override
    public SubCategoryDTO.Detail getById(String id) {

        SubCategory entity =
                subCategoryRepository.findByUuid(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "record.notfound"
                                ));

        return subCategoryMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        SubCategory entity =
                subCategoryRepository
                        .findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "record.notfound"
                                ));

        entity.setActive(active);

        entity.setModifiedBy(user.getRecordId());

        subCategoryRepository.save(entity);
    }

    @Override
    public List<KeyValueDto> getKeyValueList(String lang) {

        return subCategoryRepository
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

    private void validateDuplicate(
            SubCategoryDTO.SaveOrUpdate request
    ) {

        subCategoryRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(
                        request.getNameEn().trim()
                )
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "record.name.en.exists"
                    );
                });

        subCategoryRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(
                        request.getNameFr().trim()
                )
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "record.name.fr.exists"
                    );
                });
    }

    private void validateDuplicateForUpdate(
            SubCategoryDTO.SaveOrUpdate request,
            SubCategory entity
    ) {

        subCategoryRepository
                .findByNameEnIgnoreCaseAndDeletedFalse(
                        request.getNameEn().trim()
                )
                .ifPresent(c -> {

                    if (!c.getRecordId()
                            .equals(entity.getRecordId())) {

                        throw new DuplicateResourceException(
                                "record.name.en.exists"
                        );
                    }
                });

        subCategoryRepository
                .findByNameFrIgnoreCaseAndDeletedFalse(
                        request.getNameFr().trim()
                )
                .ifPresent(c -> {

                    if (!c.getRecordId()
                            .equals(entity.getRecordId())) {

                        throw new DuplicateResourceException(
                                "record.name.fr.exists"
                        );
                    }
                });
    }

    @Override
    public DataTableResponsePacket<SubCategoryDTO.Detail> getListData(SubCategoryDTO.ListOrSearchData request) {

        Pageable pageable =
                pageRequestCreate(request);

        Specification<SubCategory> specification =
                createFilterOrSearch(request);

        Page<SubCategory> page =
                subCategoryRepository.findAll(
                        specification,
                        pageable
                );

        return PaginationUtils.createResponse(
                page.map(subCategoryMapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            SubCategoryDTO.ListOrSearchData request
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

    private Specification<SubCategory> createFilterOrSearch(SubCategoryDTO.ListOrSearchData request) {

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
                        "%" + request.getSearch()
                                .toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("nameEn")),
                                        search
                                ),
                                cb.like(
                                        cb.lower(root.get("nameFr")),
                                        search
                                )
                        )
                );
            }

            // ACTIVE FILTER
            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {

                predicates.add(
                        cb.equal(
                                root.get("active"),
                                request.getFilters().getActive()
                        )
                );
            }

            // CATEGORY FILTER
            if (request.getFilters() != null
                    && request.getFilters().getCategoryUuid() != null) {

                predicates.add(
                        cb.equal(
                                root.get("category").get("uuid"),
                                request.getFilters().getCategoryUuid()
                        )
                );
            }

            // DATE FILTER
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

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}