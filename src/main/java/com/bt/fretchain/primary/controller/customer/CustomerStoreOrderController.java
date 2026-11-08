package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.StoreOrderDTO;
import com.bt.fretchain.primary.serviceImpl.StoreOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Customer shop / food orders. Browsing (/customer/api/shop, /customer/api/restaurant) is public;
 * everything here needs a customer login (coupons, checkout, tracking).
 */
@RestController
@RequestMapping("/customer/api/store-order")
@RequiredArgsConstructor
public class CustomerStoreOrderController extends _BaseController {

    private final StoreOrderService service;

    /** Price the cart: lines, fees, coupon discount, delivery time and any problems to fix. */
    @PostMapping("/quote")
    public ResponseEntity<ResponseData> quote(@Valid @RequestBody StoreOrderDTO.QuoteRequest request) {
        return ok(service.quote(request, getLoggedInUser()), "record.fetched");
    }

    @GetMapping("/coupons")
    public ResponseEntity<ResponseData> coupons() {
        return ok(service.availableCoupons(getLoggedInUser()), "record.fetched");
    }

    @PostMapping("/place")
    public ResponseEntity<ResponseData> place(@Valid @RequestBody StoreOrderDTO.PlaceRequest request) {
        return ok(service.place(request, getLoggedInUser()), "storeorder.placed");
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@Valid @RequestBody StoreOrderDTO.ListRequest request) {
        return ok(service.myOrders(request, getLoggedInUser()), "record.list.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> detail(@PathVariable String uuid) {
        return ok(service.myOrder(uuid, getLoggedInUser()), "record.fetched");
    }

    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<ResponseData> cancel(@PathVariable String uuid,
                                               @RequestBody(required = false) StoreOrderDTO.CancelRequest request) {
        return ok(service.cancel(uuid, request == null ? null : request.getReason(), getLoggedInUser()), "storeorder.cancelled");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
