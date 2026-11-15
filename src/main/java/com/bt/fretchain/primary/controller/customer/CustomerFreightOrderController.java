package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.CustomerFreightOrderDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.CustomerFreightOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/api/freight-order")
@RequiredArgsConstructor
public class CustomerFreightOrderController extends _BaseController {

    private final CustomerFreightOrderService customerFreightOrderService;

    private final com.bt.fretchain.primary.serviceImpl.CustomerFreightTrackService trackService;

    @PostMapping("/estimate")
    public ResponseEntity<ResponseData> estimate(
            @Valid @RequestBody CustomerFreightOrderDTO.EstimateRequest request
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("fare.estimated")
                        .data(customerFreightOrderService.estimate(request))
                        .build()
        );
    }

    @PostMapping("/" +
            "save-draft")
    public ResponseEntity<ResponseData> saveDraft(
            @Valid @RequestBody CustomerFreightOrderDTO.CreateDraftRequest request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.draft.saved")
                        .data(customerFreightOrderService.createDraft(request, user))
                        .build()
        );
    }

    @PutMapping("/{orderUuid}/update-draft")
    public ResponseEntity<ResponseData> updateDraft(
            @PathVariable String orderUuid,
            @Valid @RequestBody CustomerFreightOrderDTO.UpdateDraftRequest request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.draft.updated")
                        .data(customerFreightOrderService.updateDraft(orderUuid, request, user))
                        .build()
        );
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<ResponseData> orderDetail(
            @PathVariable String orderUuid
    ) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.detail.fetched")
                        .data(customerFreightOrderService.orderDetail(orderUuid, user))
                        .build()
        );
    }

    @PostMapping("/{orderUuid}/place-order")
    public ResponseEntity<ResponseData> placeOrder(
            @PathVariable String orderUuid
    ) {

        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.placed")
                        .data(customerFreightOrderService.placeOrder(orderUuid, user))
                        .build()
        );
    }


    @GetMapping("/draft")
    public ResponseEntity<ResponseData> getDraftOrder(
            @RequestParam String categoryUuid,
            @RequestParam(required = false) String subCategoryUuid
    ) {

        SystemUser customer = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(
                                customerFreightOrderService.getDraftOrder(
                                        customer,
                                        categoryUuid,
                                        subCategoryUuid
                                )
                        )
                        .build()
        );
    }

    /** My freight orders: in progress (active=true) or past (active=false). Drafts are not listed. */
    @PostMapping("/my-orders")
    public ResponseEntity<ResponseData> myOrders(@jakarta.validation.Valid @RequestBody com.bt.fretchain.primary.dto.FreightTrackDTO.ListRequest request) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message("record.list.fetched")
                .data(trackService.list(request, getLoggedInUser())).build());
    }

    /** Live tracking: status, steps, driver, pickup and every drop, timeline. */
    @GetMapping("/{orderUuid}/track")
    public ResponseEntity<ResponseData> track(@PathVariable String orderUuid) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message("record.fetched")
                .data(trackService.track(orderUuid, getLoggedInUser())).build());
    }

    /** Cancel an order that has no driver yet. */
    @PostMapping("/{orderUuid}/cancel")
    public ResponseEntity<ResponseData> cancel(@PathVariable String orderUuid,
                                               @RequestBody(required = false) com.bt.fretchain.primary.dto.FreightTrackDTO.CancelRequest request) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message("status.updated")
                .data(trackService.cancel(orderUuid, request == null ? null : request.getReason(), getLoggedInUser())).build());
    }
}
