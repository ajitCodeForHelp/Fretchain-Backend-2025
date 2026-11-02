package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.CouponDTO;
import com.bt.fretchain.primary.pojo.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    Coupon toEntity(CouponDTO.SaveOrUpdate dto);

    CouponDTO.Detail toDetailDto(Coupon entity);

    void updateEntityFromDto(
            CouponDTO.SaveOrUpdate dto,
            @MappingTarget Coupon entity
    );
}