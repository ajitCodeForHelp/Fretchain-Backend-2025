package com.bt.fretchain.primary.controller.partner.restaurant;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.StoreOrderDTO;
import com.bt.fretchain.primary.pojo.RestaurantStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import com.bt.fretchain.primary.serviceImpl.RestaurantAccessService;
import com.bt.fretchain.primary.serviceImpl.StoreOrderPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Restaurant login: orders placed with the logged-in restaurant (ORDER_VIEW to see, ORDER_STATUS to change). */
@RestController
@RequestMapping("/partner/api/restaurant-order")
@RequiredArgsConstructor
public class PartnerRestaurantOrderController extends _BaseController {

    private final StoreOrderPartnerService service;
    private final RestaurantAccessService access;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@Valid @RequestBody StoreOrderDTO.PartnerListRequest request) {
        RestaurantStaff staff = access.require(getLoggedInUser(), RestaurantActivity.ORDER_VIEW);
        return ok(service.list(StoreType.RESTAURANT, staff.getRestaurant().getRecordId(), request), "record.list.fetched");
    }

    @GetMapping("/summary")
    public ResponseEntity<ResponseData> summary() {
        RestaurantStaff staff = access.require(getLoggedInUser(), RestaurantActivity.ORDER_VIEW);
        return ok(service.summary(StoreType.RESTAURANT, staff.getRestaurant().getRecordId()), "record.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> detail(@PathVariable String uuid) {
        RestaurantStaff staff = access.require(getLoggedInUser(), RestaurantActivity.ORDER_VIEW);
        boolean canChange = access.has(staff, RestaurantActivity.ORDER_STATUS);
        return ok(service.detail(StoreType.RESTAURANT, staff.getRestaurant().getRecordId(), uuid, canChange), "record.fetched");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @Valid @RequestBody StoreOrderDTO.StatusUpdate request) {
        SystemUser user = getLoggedInUser();
        RestaurantStaff staff = access.require(user, RestaurantActivity.ORDER_STATUS);
        return ok(service.updateStatus(StoreType.RESTAURANT, staff.getRestaurant().getRecordId(), uuid, request, user), "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
