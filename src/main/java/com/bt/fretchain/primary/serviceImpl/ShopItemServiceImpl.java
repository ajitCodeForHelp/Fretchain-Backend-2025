package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.ShopItemDTO;
import com.bt.fretchain.primary.mapper.ShopItemMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.repository.ShopCategoryRepository;
import com.bt.fretchain.primary.repository.ShopItemRepository;
import com.bt.fretchain.primary.repository.ShopMasterRepository;
import com.bt.fretchain.primary.repository.ShopSubCategoryRepository;
import com.bt.fretchain.primary.service.ShopItemService;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShopItemServiceImpl implements ShopItemService {

    private final ShopItemRepository repository;
    private final ShopMasterRepository shopRepository;
    private final ShopCategoryRepository categoryRepository;
    private final ShopSubCategoryRepository subCategoryRepository;
    private final ShopItemMapper mapper;

    @Override
    public ShopItemDTO.Detail save(
            ShopItemDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(
                request.getShopUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("shop.notfound"));

        ShopCategory category = categoryRepository.findByUuidAndDeletedFalse(
                request.getCategoryUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("category.notfound"));

        validateCategoryBelongsToShop(category, shop);
        validatePrice(request);

        ShopItem entity = mapper.toEntity(request);
        entity.setShop(shop);
        entity.setCategory(category);
        entity.setSubCategory(resolveSubCategory(request, shop, category));
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public ShopItemDTO.Detail update(
            String uuid,
            ShopItemDTO.SaveOrUpdate request,
            SystemUser user) {

        ShopItem entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(
                request.getShopUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("shop.notfound"));

        ShopCategory category = categoryRepository.findByUuidAndDeletedFalse(
                request.getCategoryUuid()
        ).orElseThrow(() ->
                new ResourceNotFoundException("category.notfound"));

        validateCategoryBelongsToShop(category, shop);
        validatePrice(request);

        mapper.updateEntityFromDto(request, entity);
        entity.setShop(shop);
        entity.setCategory(category);
        entity.setSubCategory(resolveSubCategory(request, shop, category));
        entity.setModifiedBy(user.getRecordId());

        return mapper.toDetailDto(repository.save(entity));
    }

    @Override
    public ShopItemDTO.Detail getById(String uuid) {
        return mapper.toDetailDto(
                repository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound"))
        );
    }

    @Override
    public void updateStatus(String uuid, Boolean active, SystemUser user) {
        ShopItem entity = repository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("record.notfound"));

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());
        repository.save(entity);
    }

    private ShopSubCategory resolveSubCategory(
            ShopItemDTO.SaveOrUpdate request,
            ShopMaster shop,
            ShopCategory category) {

        if (request.getSubCategoryUuid() == null
                || request.getSubCategoryUuid().isBlank()) {
            return null;
        }

        ShopSubCategory subCategory =
                subCategoryRepository.findByUuidAndDeletedFalse(
                        request.getSubCategoryUuid()
                ).orElseThrow(() ->
                        new ResourceNotFoundException("subcategory.notfound"));

        if (!subCategory.getShop().getRecordId().equals(shop.getRecordId())
                || !subCategory.getCategory().getRecordId()
                        .equals(category.getRecordId())) {
            throw new IllegalArgumentException(
                    "Subcategory must belong to the selected shop and category");
        }

        return subCategory;
    }

    private void validateCategoryBelongsToShop(
            ShopCategory category, ShopMaster shop) {

        if (!category.getShop().getRecordId()
                .equals(shop.getRecordId())) {
            throw new IllegalArgumentException(
                    "Category does not belong to the selected shop");
        }
    }

    private void validatePrice(ShopItemDTO.SaveOrUpdate request) {
        if (request.getPrice() == null
                || request.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        if (request.getDiscountPrice() != null
                && (request.getDiscountPrice().signum() < 0
                || request.getDiscountPrice()
                        .compareTo(request.getPrice()) > 0)) {
            throw new IllegalArgumentException(
                    "Discount price must be between zero and the regular price");
        }
    }
    @Override
    public DataTableResponsePacket<ShopItemDTO.Detail> getListData(
            ShopItemDTO.ListOrSearchData request) {

        Pageable pageable = pageRequestCreate(request);

        Specification<ShopItem> specification =
                createFilterOrSearch(request);

        Page<ShopItem> page =
                repository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(mapper::toDetailDto)
        );
    }

    private Pageable pageRequestCreate(
            ShopItemDTO.ListOrSearchData request) {

        Set<String> allowedFields = Set.of(
                "createdAt", "nameEn", "nameFr",
                "price", "stockQuantity", "displayOrder"
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

    private Specification<ShopItem> createFilterOrSearch(
            ShopItemDTO.ListOrSearchData request) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if (request.getSearch() != null
                    && !request.getSearch().isBlank()) {

                String search = "%"
                        + request.getSearch().trim().toLowerCase()
                        + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nameEn")), search),
                        cb.like(cb.lower(root.get("nameFr")), search),
                        cb.like(cb.lower(root.get("sku")), search),
                        cb.like(cb.lower(root.get("itemCode")), search)
                ));
            }

            if (request.getShopUuid() != null
                    && !request.getShopUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("shop").get("uuid"),
                        request.getShopUuid()
                ));
            }

            if (request.getCategoryUuid() != null
                    && !request.getCategoryUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("category").get("uuid"),
                        request.getCategoryUuid()
                ));
            }

            if (request.getSubCategoryUuid() != null
                    && !request.getSubCategoryUuid().isBlank()) {

                predicates.add(cb.equal(
                        root.get("subCategory").get("uuid"),
                        request.getSubCategoryUuid()
                ));
            }

            if (request.getAvailable() != null) {
                predicates.add(cb.equal(
                        root.get("available"),
                        request.getAvailable()
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