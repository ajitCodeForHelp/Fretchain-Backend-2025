package com.bt.fretchain.primary.mapper;

import com.bt.fretchain.primary.dto.BannerDTO;
import com.bt.fretchain.primary.dto.CustomerHomePageDTO;
import com.bt.fretchain.primary.pojo.Banner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BannerMapper {

    Banner toEntity(BannerDTO.SaveOrUpdate dto);

    BannerDTO.Detail toDetailDto(Banner entity);

    void updateEntityFromDto(
            BannerDTO.SaveOrUpdate dto,
            @MappingTarget Banner entity
    );

    CustomerHomePageDTO.BannerResponse toCustomerBannerResponse(
            Banner entity
    );
}