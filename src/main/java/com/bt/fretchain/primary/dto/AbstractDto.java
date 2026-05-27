package com.bt.fretchain.primary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public abstract class AbstractDto {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static abstract class KeyValue {
        private String key;
        private String value;
        private String label;
    }

    @Setter
    @Getter
    public static abstract class Detail extends BasicDto {
    }

    @Setter
    @Getter
    public static class GetList extends BasePageRequest {
        private String search;
    }
}
