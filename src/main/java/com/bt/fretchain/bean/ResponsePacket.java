package com.bt.fretchain.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponsePacket<T> {
    private int errorCode;
    private String message;
    private T responsePacket;

    public ResponsePacket(int errorCode, String message, T responsePacket) {
        this.errorCode = errorCode;
        this.message = message;
        this.responsePacket = responsePacket;
    }
}
