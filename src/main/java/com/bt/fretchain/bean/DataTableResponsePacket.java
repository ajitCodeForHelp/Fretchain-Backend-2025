package com.bt.fretchain.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTableResponsePacket<T> {

    // Pagination Info
    private int currentPage;
    private int pageSize;

    private int totalPages;
    private long totalItems;

    // Extra Useful Fields
    private int currentItemCount;

    private boolean first;
    private boolean last;

    private boolean hasNext;
    private boolean hasPrevious;

    // Actual Data
    private List<T> data;
}
