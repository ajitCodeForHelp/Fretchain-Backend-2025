package com.bt.fretchain.primary.controller.customer;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.dto.CustomerFreightOrderDTO;
import com.bt.fretchain.primary.service.CustomerFreightOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/api/freight-order")
@RequiredArgsConstructor
public class CustomerFreightOrderController {

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

    @PostMapping("/save-draft")
    public ResponseEntity<ResponseData> saveDraft(
            @Valid @RequestBody CustomerFreightOrderDTO.CreateDraftRequest request
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.draft.saved")
                        .data(customerFreightOrderService.createDraft(request))
                        .build()
        );
    }

    @PutMapping("/{orderUuid}/update-draft")
    public ResponseEntity<ResponseData> updateDraft(
            @PathVariable String orderUuid,
            @Valid @RequestBody CustomerFreightOrderDTO.UpdateDraftRequest request
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.draft.updated")
                        .data(customerFreightOrderService.updateDraft(orderUuid, request))
                        .build()
        );
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<ResponseData> orderDetail(
            @PathVariable String orderUuid
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.detail.fetched")
                        .data(customerFreightOrderService.orderDetail(orderUuid))
                        .build()
        );
    }

    @PostMapping("/{orderUuid}/place-order")
    public ResponseEntity<ResponseData> placeOrder(
            @PathVariable String orderUuid
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("order.placed")
                        .data(customerFreightOrderService.placeOrder(orderUuid))
                        .build()
        );
    }
}