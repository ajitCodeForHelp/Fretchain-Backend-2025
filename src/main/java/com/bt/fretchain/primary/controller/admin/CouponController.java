package com.bt.fretchain.primary.controller.admin;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.CouponDTO;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/coupon")
@RequiredArgsConstructor
public class CouponController extends _BaseController {

    private final CouponService couponService;

    @PostMapping("/save")
    public ResponseEntity<ResponseData> save(
            @Valid @RequestBody CouponDTO.SaveOrUpdate request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.saved")
                        .data(couponService.save(request, user))
                        .build()
        );
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ResponseData> update(
            @PathVariable String uuid,
            @Valid @RequestBody CouponDTO.SaveOrUpdate request
    ) {
        SystemUser user = getLoggedInUser();

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.updated")
                        .data(couponService.update(uuid, request, user))
                        .build()
        );
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseData> getById(
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.fetched")
                        .data(couponService.getById(uuid))
                        .build()
        );
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseData> list(
            @Valid @RequestBody CouponDTO.ListOrSearchData request
    ) {
        DataTableResponsePacket<CouponDTO.Detail> data =
                couponService.getListData(request);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("record.list.fetched")
                        .data(data)
                        .build()
        );
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ResponseData> updateStatus(
            @PathVariable String uuid,
            @RequestParam Boolean active
    ) {
        SystemUser user = getLoggedInUser();

        couponService.updateStatus(uuid, active, user);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(200)
                        .message("status.updated")
                        .build()
        );
    }
}