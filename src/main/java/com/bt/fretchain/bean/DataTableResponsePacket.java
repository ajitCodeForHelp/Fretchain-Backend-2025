package com.bt.fretchain.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTableResponsePacket<T> {

    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalItems;
    private List<T> data;
}
