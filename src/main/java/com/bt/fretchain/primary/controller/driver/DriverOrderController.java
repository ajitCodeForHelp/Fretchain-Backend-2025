package com.bt.fretchain.primary.controller.driver;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.serviceImpl.DriverOrderService;
import com.bt.fretchain.primary.serviceImpl.OfferService;
import com.bt.fretchain.security.JwtUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** The driver's accepted orders: details, next step, history. */
@RestController
@RequestMapping("/driver/api/orders")
@RequiredArgsConstructor
public class DriverOrderController {

    private final DriverOrderService orders;
    private final OfferService offers;
    private final JwtUserDetailsService userDetails;

    /** Orders in progress, each with its steps and the next button. */
    @GetMapping("/current")
    public ResponseEntity<ResponseData> current() {
        return ok(offers.currentFor(userDetails.getLoggedInDriver()), "record.list.fetched");
    }

    @GetMapping("/history")
    public ResponseEntity<ResponseData> history(@RequestParam(defaultValue = "30") int limit) {
        return ok(orders.history(userDetails.getLoggedInDriver(), Math.min(Math.max(limit, 1), 100)), "record.list.fetched");
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<ResponseData> get(@PathVariable String orderUuid) {
        return ok(orders.get(userDetails.getLoggedInDriver(), orderUuid), "record.fetched");
    }

    /** Presses the next step. Body: { "step": "PICKED_UP" } (the step the screen showed; stale taps are refused). */
    @PostMapping("/{orderUuid}/next")
    public ResponseEntity<ResponseData> next(@PathVariable String orderUuid, @RequestBody(required = false) Map<String, String> body) {
        return ok(orders.advance(userDetails.getLoggedInDriver(), orderUuid, body == null ? null : body.get("step")), "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
