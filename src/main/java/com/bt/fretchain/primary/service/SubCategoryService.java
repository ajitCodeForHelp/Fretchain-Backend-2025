package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.SubCategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface SubCategoryService {

    SubCategoryDTO.Detail save(
            SubCategoryDTO.SaveOrUpdate request,
            SystemUser user
    );

    SubCategoryDTO.Detail update(
            String uuid,
            SubCategoryDTO.SaveOrUpdate request,
            SystemUser user
    );

    DataTableResponsePacket<SubCategoryDTO.Detail> getListData(
            SubCategoryDTO.ListOrSearchData request
    );

    SubCategoryDTO.Detail getById(String id);

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );

    List<KeyValueDto> getKeyValueList(String lang);
}