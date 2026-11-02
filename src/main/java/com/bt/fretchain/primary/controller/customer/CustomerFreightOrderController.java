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
}