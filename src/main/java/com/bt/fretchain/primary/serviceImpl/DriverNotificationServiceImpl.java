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

    @Override
    public void sendOffer(OrderDriverAssignment assignment, String orderType, String orderNumber, String body) {
        Driver driver = assignment.getDriver();
        if (driver.getDeviceToken() == null || driver.getDeviceToken().isBlank()) {
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(driver.getDeviceToken())
                    .setNotification(Notification.builder().setTitle("New order " + orderNumber).setBody(body).build())
                    .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                            .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                            .setTtl(120_000L)
                            .setNotification(com.google.firebase.messaging.AndroidNotification.builder()
                                    .setChannelId("orders")
                                    .setSound("default")
                                    .setPriority(com.google.firebase.messaging.AndroidNotification.Priority.MAX)
                                    .build())
                            .build())
                    .putData("type", "ORDER_OFFER")
                    .putData("assignmentUuid", assignment.getUuid())
                    .putData("orderType", orderType)
                    .putData("orderNumber", orderNumber == null ? "" : orderNumber)
                    .putData("expiresAt", assignment.getExpiresAt() == null ? "" : assignment.getExpiresAt().toString())
                    .build();
            firebaseMessaging.send(message);
            assignment.setNotificationSent(true);
            assignment.setNotificationSentAt(java.time.LocalDateTime.now());
        } catch (Exception e) {
            // a failed push must never break dispatching: the driver still sees the offer when the app opens
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Offer push failed for driver {}: {}", driver.getUuid(), e.getMessage());
        }
    }

    @Override
    public void sendOfferClosed(Driver driver, String assignmentUuid, String reason) {
        if (driver.getDeviceToken() == null || driver.getDeviceToken().isBlank()) {
            return;
        }
        try {
            firebaseMessaging.send(Message.builder()
                    .setToken(driver.getDeviceToken())
                    .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                            .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH).build())
                    .putData("type", "ORDER_OFFER_CLOSED")
                    .putData("assignmentUuid", assignmentUuid)
                    .putData("reason", reason == null ? "" : reason)
                    .build());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Offer-closed push failed: {}", e.getMessage());
        }
    }
}
