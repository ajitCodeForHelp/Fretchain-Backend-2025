package com.bt.fretchain.primary.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BasePageRequest {

    private String search;

    private Integer pageNumber = 0 ;
    private Integer pageSize = 10 ;

}