package com.bt.fretchain.primary.controller.partner.shop;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.StoreOrderDTO;
import com.bt.fretchain.primary.pojo.ShopStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import com.bt.fretchain.primary.serviceImpl.ShopAccessService;
import com.bt.fretchain.primary.serviceImpl.StoreOrderPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Shop login: orders placed with the logged-in shop (ORDER_VIEW to see, ORDER_STATUS to change). */
@RestController
@RequestMapping("/partner/api/shop-order")
@RequiredArgsConstructor
public class PartnerShopOrderController extends _BaseController {

    private final StoreOrderPartnerService service;
    private final ShopAccessService access;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@Valid @RequestBody StoreOrderDTO.PartnerListRequest request) {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.ORDER_VIEW);
        return ok(service.list(StoreType.SHOP, staff.getShop().getRecordId(), request), "record.list.fetched");
    }

    @GetMapping("/summary")
    public ResponseEntity<ResponseData> summary() {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.ORDER_VIEW);
        return ok(service.summary(StoreType.SHOP, staff.getShop().getRecordId()), "record.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> detail(@PathVariable String uuid) {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.ORDER_VIEW);
        boolean canChange = access.has(staff, ShopActivity.ORDER_STATUS);
        return ok(service.detail(StoreType.SHOP, staff.getShop().getRecordId(), uuid, canChange), "record.fetched");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @Valid @RequestBody StoreOrderDTO.StatusUpdate request) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.ORDER_STATUS);
        return ok(service.updateStatus(StoreType.SHOP, staff.getShop().getRecordId(), uuid, request, user), "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
