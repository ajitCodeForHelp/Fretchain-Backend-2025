package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.OrderDriverAssignment;
import com.bt.fretchain.primary.service.DriverNotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverNotificationServiceImpl
        implements DriverNotificationService {

    private final FirebaseMessaging firebaseMessaging;


    @Override
    public void sendOrderAssignmentNotification(
            OrderDriverAssignment assignment
    ) {

        Driver driver =
                assignment.getDriver();

        if (driver.getDeviceToken() == null
                || driver.getDeviceToken().isBlank()) {

            return;
        }


        Message message =
                Message.builder()

                        .setToken(
                                driver.getDeviceToken()
                        )

                        .setNotification(
                                Notification.builder()
                                        .setTitle(
                                                "New Order Assignment"
                                        )
                                        .setBody(
                                                "You have received a new delivery assignment"
                                        )
                                        .build()
                        )

                        .putData(
                                "type",
                                "DRIVER_ASSIGNMENT"
                        )

                        .putData(
                                "assignmentUuid",
                                assignment.getUuid()
                        )

                        .putData(
                                "orderUuid",
                                assignment
                                        .getFreightOrder()
                                        .getUuid()
                        )

                        .build();


        try {

            FirebaseMessaging
                    .getInstance()
                    .send(message);

        } catch (FirebaseMessagingException e) {
  //TODO ADD LOGS FUNCTIONALITY
//            log.error(
//                    "Failed to send driver assignment notification",
//                    e
//            );
        }
    }
}