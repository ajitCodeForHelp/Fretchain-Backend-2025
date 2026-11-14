package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import com.bt.fretchain.primary.repository.CustomerNotificationRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Tells the customer what happens to their order: store steps, driver steps, cancellation.
 * Every message is saved (in-app feed) and pushed to the customer's phone when it has a token.
 * A push that cannot be sent never breaks the order flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotifier {

    private final CustomerNotificationRepository notifications;
    private final FirebaseMessaging firebase;

    // ---------------------------------------------------------------- shop / food

    public void store(StoreOrder o, StoreOrderStatus status, String reason) {
        String store = o.getStoreName();
        String driver = o.getDriver() == null ? "Your driver" : o.getDriver().getFullName();
        String body = switch (status) {
            case ACCEPTED -> store + " accepted your order";
            case PREPARING -> store + " is preparing your order";
            case READY -> o.getDriver() == null ? "Your order is ready" : "Your order is ready for pickup";
            case OUT_FOR_DELIVERY -> driver + " picked up your order and is on the way";
            case DELIVERED -> "Your order was delivered. Enjoy!";
            case REJECTED -> store + " could not accept your order" + (reason == null || reason.isBlank() ? "" : ": " + reason);
            case CANCELLED -> "Your order was cancelled" + (reason == null || reason.isBlank() ? "" : ": " + reason);
            default -> null;
        };
        if (body != null) send(o.getCustomer(), "Order " + o.getOrderNumber(), body, type(o), o.getUuid());
    }

    /** A driver event that is not a status change (assigned, arrived). */
    public void storeText(StoreOrder o, String body) {
        send(o.getCustomer(), "Order " + o.getOrderNumber(), body, type(o), o.getUuid());
    }

    // ---------------------------------------------------------------- freight

    public void freight(FreightOrder o, OrderStatus status) {
        Driver d = o.getDriver();
        String who = d == null ? "Your driver" : d.getFullName();
        String body = switch (status) {
            case ACCEPTED -> who + (d != null && d.getVehicle() != null ? " (" + d.getVehicle().getVehicleNumber() + ")" : "") + " is assigned to your order";
            case PICKUP_STARTED -> who + " is on the way to the pickup";
            case PICKUP_ARRIVED -> who + " arrived at the pickup location";
            case LOADING_STARTED -> "Loading has started";
            case LOADING_COMPLETED -> "Loading is complete";
            case IN_TRANSIT -> "Your goods are on the way";
            case DROP_ARRIVED -> who + " arrived at the drop location";
            case UNLOADING_STARTED -> "Unloading has started";
            case UNLOADING_COMPLETED -> "Unloading is complete";
            case DELIVERED -> "Your order was delivered";
            case CANCELLED_BY_ADMIN, CANCELLED_BY_DRIVER -> "Your order was cancelled";
            default -> null;
        };
        if (body != null) send(o.getCustomer(), "Order " + o.getOrderNumber(), body, "FREIGHT", o.getUuid());
    }

    // ---------------------------------------------------------------- sending

    private static String type(StoreOrder o) {
        return o.getStoreType() == StoreType.SHOP ? "SHOP" : "FOOD";
    }

    private void send(SystemUser customer, String title, String body, String orderType, String orderUuid) {
        if (customer == null) {
            return;
        }
        try {
            CustomerNotification n = new CustomerNotification();
            n.setCustomer(customer);
            n.setTitle(title);
            n.setBody(body);
            n.setOrderType(orderType);
            n.setOrderUuid(orderUuid);
            String token = customer.getFcmDeviceToken();
            if (token != null && !token.isBlank()) {
                try {
                    firebase.send(Message.builder()
                            .setToken(token)
                            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(AndroidNotification.builder().setChannelId("updates").setSound("default").build()).build())
                            .putData("type", "ORDER_UPDATE")
                            .putData("orderType", orderType)
                            .putData("orderUuid", orderUuid == null ? "" : orderUuid)
                            .build());
                    n.setPushed(true);
                } catch (Exception e) {
                    log.warn("Customer push failed ({}): {}", orderType, e.getMessage());
                }
            }
            notifications.save(n);
        } catch (Exception e) {
            log.warn("Could not record customer notification: {}", e.getMessage());
        }
    }
}
