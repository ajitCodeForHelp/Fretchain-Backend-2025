package com.bt.fretchain.messaging;


import java.util.ArrayList;
import java.util.List;

public enum NotificationTypeEnum {
    OrderRequest("OrderRequest"),
    Order("Order"),
    Chat("Chat"),
    CancelOrder("CancelOrder"),
    Referral("Referral"),
    Coupon("Coupon"),
    ReleaseDriver("ReleaseDriver"),
    ;

    String type;

    NotificationTypeEnum(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static List<NotificationTypeEnum> getTypes() {
        List<NotificationTypeEnum> list = new ArrayList<>();
        return list;
    }
}