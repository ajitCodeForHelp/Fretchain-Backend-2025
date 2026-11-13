package com.bt.fretchain.primary.pojo.enums;

/**
 * Review state of a driver account. Admin-created drivers start VERIFIED; vendor-created and public
 * sign-ups start PENDING until the platform admin or the driver's fleet owner approves them.
 * A REJECTED driver carries the reviewer's reason and can be corrected and re-submitted (back to PENDING).
 */
public enum DriverVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
