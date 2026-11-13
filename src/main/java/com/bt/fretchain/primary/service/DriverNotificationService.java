package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.OrderDriverAssignment;

public interface DriverNotificationService {

    void sendOrderAssignmentNotification(OrderDriverAssignment assignment);

    /**
     * A new order offer. Sent as a high-priority message with a notification part, so it rings with the app
     * open, in the background or closed. data: type=ORDER_OFFER, assignmentUuid, orderType, orderNumber, expiresAt.
     */
    void sendOffer(OrderDriverAssignment assignment, String orderType, String orderNumber, String body);

    /** The offer is no longer available (taken by another driver, expired, cancelled): the phone can dismiss it. */
    void sendOfferClosed(Driver driver, String assignmentUuid, String reason);
}
