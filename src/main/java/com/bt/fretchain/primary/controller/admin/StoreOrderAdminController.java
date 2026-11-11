package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.StoreOrderDTO;
import com.bt.fretchain.primary.pojo.enums.PlatformActivity;
import com.bt.fretchain.primary.pojo.enums.StoreType;
import com.bt.fretchain.primary.serviceImpl.PlatformAccessService;
import com.bt.fretchain.primary.serviceImpl.StoreOrderPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: shop and restaurant orders of every store.
 * Under the Orders menu: ORDER_VIEW to see, ORDER_STATUS to move an order (checked by PanelAuthorizationInterceptor).
 */
@RestController
@RequestMapping("/admin/api/store-order")
@RequiredArgsConstructor
public class StoreOrderAdminController extends _BaseController {

    private final StoreOrderPartnerService service;
    private final PlatformAccessService access;

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(@Valid @RequestBody StoreOrderDTO.AdminListRequest request) {
        return ok(service.adminList(request, access.has(getLoggedInUser(), PlatformActivity.ORDER_STATUS)), "record.list.fetched");
    }

    @GetMapping("/summary")
    public ResponseEntity<ResponseData> summary(@RequestParam(required = false) StoreType storeType) {
        return ok(service.adminSummary(storeType), "record.fetched");
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> detail(@PathVariable String uuid) {
        boolean canChange = access.has(getLoggedInUser(), PlatformActivity.ORDER_STATUS);
        return ok(service.adminDetail(uuid, canChange), "record.fetched");
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> status(@PathVariable String uuid, @Valid @RequestBody StoreOrderDTO.StatusUpdate request) {
        return ok(service.adminUpdateStatus(uuid, request, getLoggedInUser()), "status.updated");
    }

    /** Ready drivers around the store, nearest first. */
    @GetMapping("/{uuid}/drivers")
    public ResponseEntity<ResponseData> drivers(@PathVariable String uuid, @RequestParam(required = false) Double radiusKm) {
        return ok(service.adminDriverCandidates(uuid, radiusKm), "record.fetched");
    }

    /** Assign (or, with an empty driverUuid, remove) the delivery driver. */
    @PatchMapping("/{uuid}/driver")
    public ResponseEntity<ResponseData> assign(@PathVariable String uuid, @RequestBody StoreOrderDTO.AssignDriver request) {
        return ok(service.adminAssignDriver(uuid, request.getDriverUuid(), getLoggedInUser()), "status.updated");
    }

    /** Assign the nearest ready driver. */
    @PatchMapping("/{uuid}/driver/auto")
    public ResponseEntity<ResponseData> autoAssign(@PathVariable String uuid) {
        return ok(service.adminAutoAssign(uuid, getLoggedInUser()), "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
