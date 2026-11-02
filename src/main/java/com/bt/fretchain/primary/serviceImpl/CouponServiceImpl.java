package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.DuplicateResourceException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.CouponDTO;
import com.bt.fretchain.primary.mapper.CouponMapper;
import com.bt.fretchain.primary.pojo.Coupon;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.CouponDiscountType;
import com.bt.fretchain.primary.repository.CouponRepository;
import com.bt.fretchain.primary.service.CouponService;
import com.bt.fretchain.utils.PaginationUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    private final CouponMapper couponMapper;

    @Override
    public CouponDTO.Detail save(
            CouponDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateDuplicate(request);
        validateCoupon(request);

        Coupon entity = couponMapper.toEntity(request);

        entity.setCouponCode(request.getCouponCode().trim().toUpperCase());
        entity.setUsedCount(0);
        entity.setCreatedBy(user.getRecordId());
        entity.setModifiedBy(user.getRecordId());

        return couponMapper.toDetailDto(
                couponRepository.save(entity)
        );
    }

    @Override
    public CouponDTO.Detail update(
            String uuid,
            CouponDTO.SaveOrUpdate request,
            SystemUser user
    ) {
        validateCoupon(request);

        Coupon entity =
                couponRepository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        validateDuplicateForUpdate(request, entity);

        couponMapper.updateEntityFromDto(request, entity);

        entity.setCouponCode(request.getCouponCode().trim().toUpperCase());
        entity.setModifiedBy(user.getRecordId());

        return couponMapper.toDetailDto(
                couponRepository.save(entity)
        );
    }

    @Override
    public CouponDTO.Detail getById(String uuid) {

        Coupon entity =
                couponRepository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        return couponMapper.toDetailDto(entity);
    }

    @Override
    public void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    ) {
        Coupon entity =
                couponRepository.findByUuidAndDeletedFalse(uuid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("record.notfound")
                        );

        entity.setActive(active);
        entity.setModifiedBy(user.getRecordId());

        couponRepository.save(entity);
    }

    @Override
    public DataTableResponsePacket<CouponDTO.Detail> getListData(
            CouponDTO.ListOrSearchData request
    ) {
        Pageable pageable = pageRequestCreate(request);

        Specification<Coupon> specification =
                createFilterOrSearch(request);

        Page<Coupon> page =
                couponRepository.findAll(specification, pageable);

        return PaginationUtils.createResponse(
                page.map(couponMapper::toDetailDto)
        );
    }

    private void validateDuplicate(
            CouponDTO.SaveOrUpdate request
    ) {
        couponRepository
                .findByCouponCodeIgnoreCaseAndDeletedFalse(
                        request.getCouponCode().trim()
                )
                .ifPresent(c -> {
                    throw new DuplicateResourceException("coupon.code.exists");
                });
    }

    private void validateDuplicateForUpdate(
            CouponDTO.SaveOrUpdate request,
            Coupon entity
    ) {
        couponRepository
                .findByCouponCodeIgnoreCaseAndDeletedFalse(
                        request.getCouponCode().trim()
                )
                .ifPresent(c -> {
                    if (!c.getRecordId().equals(entity.getRecordId())) {
                        throw new DuplicateResourceException("coupon.code.exists");
                    }
                });
    }

    private void validateCoupon(
            CouponDTO.SaveOrUpdate request
    ) {
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {

            throw new BadRequestException("date.range.invalid");
        }

        if (request.getDiscountValue() != null
                && request.getDiscountValue()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new BadRequestException("discount.value.invalid");
        }

        if (request.getDiscountType() == CouponDiscountType.PERCENTAGE
                && request.getDiscountValue() != null
                && request.getDiscountValue()
                .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new BadRequestException("discount.percentage.invalid");
        }

        if (request.getUsageLimit() != null
                && request.getUsageLimit() < 0) {

            throw new BadRequestException("usage.limit.invalid");
        }

        if (request.getPerCustomerLimit() != null
                && request.getPerCustomerLimit() < 1) {

            throw new BadRequestException("per.customer.limit.invalid");
        }
    }

    private Pageable pageRequestCreate(
            CouponDTO.ListOrSearchData request
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

    private Specification<Coupon> createFilterOrSearch(
            CouponDTO.ListOrSearchData request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleted"), false));

            if (request.getSearch() != null
                    && !request.getSearch().trim().isEmpty()) {

                String search =
                        "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("couponCode")), search),
                                cb.like(cb.lower(root.get("titleEn")), search),
                                cb.like(cb.lower(root.get("titleFr")), search)
                        )
                );
            }

            if (request.getFilters() != null) {

                if (request.getFilters().getActive() != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("active"),
                                    request.getFilters().getActive()
                            )
                    );
                }

                if (request.getFilters().getDiscountType() != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("discountType"),
                                    request.getFilters().getDiscountType()
                            )
                    );
                }
            }

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