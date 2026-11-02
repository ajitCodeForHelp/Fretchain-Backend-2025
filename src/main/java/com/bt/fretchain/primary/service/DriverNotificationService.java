package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.pojo.OrderDriverAssignment;

public interface DriverNotificationService {
    void sendOrderAssignmentNotification(
            OrderDriverAssignment assignment
    );
}