package com.bt.fretchain.primary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BasicDto {

    protected String uuid;
    protected Boolean active;
    protected LocalDateTime createdAt;
    protected LocalDateTime modifiedAt;
    protected Boolean deleted;

}
