package com.bt.fretchain.primary.pojo.enums;

/** How shop / food orders get a driver. */
public enum AssignmentMode {
    /** Staff pick a driver from the eligible list. */
    MANUAL,
    /** The nearest eligible driver is chosen automatically (staff can still reassign). */
    AUTO
}
