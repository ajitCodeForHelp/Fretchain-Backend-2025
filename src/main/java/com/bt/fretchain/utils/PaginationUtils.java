package com.bt.fretchain.utils;

import com.bt.fretchain.bean.DataTableResponsePacket;
import org.springframework.data.domain.Page;

public class PaginationUtils {

    public static <T> DataTableResponsePacket<T> createResponse(
            Page<T> page
    ) {

        DataTableResponsePacket<T> response =
                new DataTableResponsePacket<>();

        response.setCurrentPage(page.getNumber());

        response.setPageSize(page.getSize());

        response.setTotalPages(page.getTotalPages());

        response.setTotalItems(page.getTotalElements());

        response.setCurrentItemCount(
                page.getNumberOfElements()
        );

        response.setFirst(page.isFirst());

        response.setLast(page.isLast());

        response.setHasNext(page.hasNext());

        response.setHasPrevious(page.hasPrevious());

        response.setData(page.getContent());

        return response;
    }
}