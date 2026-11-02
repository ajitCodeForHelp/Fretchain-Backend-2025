package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopCategoryDTO;
import com.bt.fretchain.primary.mapper.ShopCategoryMapper;
import com.bt.fretchain.primary.pojo.ShopCategory;
import com.bt.fretchain.primary.pojo.ShopGlobalCategory;
import com.bt.fretchain.primary.pojo.ShopMaster;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.ShopCategoryRepository;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.service.ShopCategoryService;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShopCategoryServiceImpl implements ShopCategoryService {

    private final ShopCategoryRepository repository;
    private final ShopMasterRepository shopRepository;
    private final ShopCategoryMapper mapper;

    @Override
    @Transactional
    public ShopCategoryDTO.Detail save(
            ShopCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(
                request.getShopUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("shop.notfound"));

        validateName(request, shop.getRecordId(), null);

        ShopCategory entity = mapper.toEntity(request);
        entity.setShop(shop);
        entity.setGlobalCategory(resolveGlobalCategory(request, shop));
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ShopCategoryDTO.Detail update(
            String uuid,
            ShopCategoryDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(
                request.getShopUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("shop.notfound"));

        validateName(request, shop.getRecordId(), entity.getRecordId());

        mapper.updateEntityFromDto(request, entity);
        entity.setShop(shop);
        entity.setGlobalCategory(resolveGlobalCategory(request, shop));
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public ShopCategoryDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        ShopCategory entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    private void validateName(
            ShopCategoryDTO.SaveOrUpdate request,
            Long shopId,
            Long currentId) {

        String name = request.getNameEn().trim();

        boolean exists = currentId == null
                ? repository.existsByShop_RecordIdAndNameEnIgnoreCaseAndDeletedFalse(
                        shopId, name)
                : repository.existsByShop_RecordIdAndNameEnIgnoreCaseAndDeletedFalseAndRecordIdNot(
                        shopId, name, currentId);

        if (exists) {
            throw new DuplicateResourceException("shop.category.name.exists");
        }
    }

    /**
     * Optional link of a shop section to a global category. The global category
     * must be one the shop is assigned to (keeps customer filtering consistent).
     */
    private ShopGlobalCategory resolveGlobalCategory(
            ShopCategoryDTO.SaveOrUpdate request, ShopMaster shop) {

        if (request.getGlobalCategoryUuid() == null
                || request.getGlobalCategoryUuid().isBlank()) {
            return null;
        }

        return shop.getGlobalCategories().stream()
                .filter(g -> !g.isDeleted()
                        && g.getUuid().equals(request.getGlobalCategoryUuid().trim()))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException("shop.global.category.notassigned"));
    }

    @Override
    public DataTableResponsePacket<ShopCategoryDTO.Detail> getListData(
            ShopCategoryDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<ShopCategory> specification =
                createFilterOrSearch(request);

        Page<ShopCategory> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            ShopCategoryDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr", "displayOrder"
        );

        String field = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (request.getSort() != null
                && request.getSort().getField() != null
                && allowedFields.contains(request.getSort().getField())) {

            field = request.getSort().getField();

            if ("asc".equalsIgnoreCase(
                    request.getSort().getDirection())) {
                direction = Sort.Direction.ASC;
            }
        }

        int pageNumber = Math.max(
                0, request.getPageNumber() == null
                        ? 0 : request.getPageNumber());

        int pageSize = request.getPageSize() == null
                ? 10 : Math.max(1, Math.min(100, request.getPageSize()));

        return PageRequest.of(
                pageNumber, pageSize, Sort.by(direction, field));
    }

    private Specification<ShopCategory> createFilterOrSearch(
            ShopCategoryDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if (request.getSearch() != null
                    && !request.getSearch().isBlank()) {

                String search = SearchUtils.containsPattern(request.getSearch());
                char esc = SearchUtils.LIKE_ESCAPE;

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), search, esc),
                        cb.like(cb.lower(root.get("nameFr")), search, esc)
                ));
            }

            if (request.getShopUuid() != null
                    && !request.getShopUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("shop").get("uuid"),
                        request.getShopUuid()
                ));
            }

            if (request.getFilters() != null
                    && request.getFilters().getActive() != null) {

                predicates.add(cb.equal(
                        root.get("active"),
                        request.getFilters().getActive()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}