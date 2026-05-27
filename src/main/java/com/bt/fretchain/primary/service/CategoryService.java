package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.primary.dto.CategoryDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

import java.util.List;

public interface CategoryService {

    CategoryDTO.Detail save(
            CategoryDTO.SaveOrUpdate request,
            SystemUser user
    );

    CategoryDTO.Detail update(
            String uuid,
            CategoryDTO.SaveOrUpdate request,
            SystemUser user
    );

    DataTableResponsePacket<CategoryDTO.Detail> getListData(
            CategoryDTO.ListOrSearchData request
    );

    CategoryDTO.Detail getById(String uuid);

    void updateStatus(
            String uuid,
            Boolean active,
            SystemUser user
    );

    List<KeyValueDto> getKeyValueList(String lang);
}