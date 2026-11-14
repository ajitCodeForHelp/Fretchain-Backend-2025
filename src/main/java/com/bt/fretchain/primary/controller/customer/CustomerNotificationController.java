package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.pojo.CustomerNotification;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.CustomerNotificationRepository;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Customer: register the phone for push messages, and the in-app notification feed. */
@RestController
@RequestMapping("/customer/api/notification")
@RequiredArgsConstructor
public class CustomerNotificationController extends _BaseController {

    private final CustomerNotificationRepository notifications;
    private final SystemUserRepository users;

    /** Called after login and whenever Firebase gives the phone a new token. Body: { "fcmToken": "..." } */
    @PostMapping("/device")
    @Transactional
    public ResponseEntity<ResponseData> register(@RequestBody Map<String, String> body) {
        SystemUser me = getLoggedInUser();
        String token = body == null ? null : body.get("fcmToken");
        if (token != null && !token.isBlank()) {
            // a phone belongs to one account at a time
            users.findAll().stream()
                    .filter(u -> token.equals(u.getFcmDeviceToken()) && !u.getRecordId().equals(me.getRecordId()))
                    .forEach(u -> { u.setFcmDeviceToken(null); users.save(u); });
            me.setFcmDeviceToken(token.trim());
            users.save(me);
        }
        return ok(null, "record.updated");
    }

    /** Logout: stop pushes to this phone. */
    @DeleteMapping("/device")
    @Transactional
    public ResponseEntity<ResponseData> unregister() {
        SystemUser me = getLoggedInUser();
        me.setFcmDeviceToken(null);
        users.save(me);
        return ok(null, "record.updated");
    }

    @GetMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> list(@RequestParam(defaultValue = "30") int limit) {
        SystemUser me = getLoggedInUser();
        List<CustomerNotification> rows = notifications.findByCustomerRecordIdAndDeletedFalseOrderByCreatedAtDesc(
                me.getRecordId(), PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
        List<Map<String, Object>> out = rows.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", n.getUuid());
            m.put("title", n.getTitle());
            m.put("body", n.getBody());
            m.put("orderType", n.getOrderType());
            m.put("orderUuid", n.getOrderUuid());
            m.put("read", n.getReadAt() != null);
            m.put("createdAt", n.getCreatedAt());
            return m;
        }).toList();
        return ok(out, "record.list.fetched");
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseData> unread() {
        return ok(notifications.countByCustomerRecordIdAndDeletedFalseAndReadAtIsNull(getLoggedInUser().getRecordId()), "record.fetched");
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<ResponseData> readAll() {
        SystemUser me = getLoggedInUser();
        notifications.findByCustomerRecordIdAndDeletedFalseOrderByCreatedAtDesc(me.getRecordId(), PageRequest.of(0, 200)).stream()
                .filter(n -> n.getReadAt() == null).forEach(n -> { n.setReadAt(LocalDateTime.now()); notifications.save(n); });
        return ok(null, "record.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
