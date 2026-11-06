package com.bt.fretchain.primary.pojo.enums;

/** Approval status of a vendor (fleet owner). Vendors created by the admin start APPROVED; public sign-ups start PENDING. */
public enum VendorStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}
