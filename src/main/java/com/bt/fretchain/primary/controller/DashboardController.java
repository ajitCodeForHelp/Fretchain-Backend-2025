package com.bt.fretchain.primary.controller;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.serviceImpl.AdminDashboardService;
import com.bt.fretchain.primary.serviceImpl.StoreDashboardService;
import com.bt.fretchain.primary.serviceImpl.VendorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-driven dashboards. What each login sees is decided here by its permissions:
 *   /admin/api/dashboard                  admins + admin staff with DASHBOARD_VIEW (widgets per menu permission)
 *   /partner/api/shop-dashboard           shop owner / admin / staff
 *   /partner/api/restaurant-dashboard     restaurant owner / admin / staff
 *   /partner/api/vendor-dashboard         vendor (fleet owner)
 * Role checks happen in PanelAuthorizationInterceptor.
 */
@RestController
@RequiredArgsConstructor
public class DashboardController extends _BaseController {

    private final AdminDashboardService admin;
    private final StoreDashboardService stores;
    private final VendorDashboardService vendors;

    @GetMapping("/admin/api/dashboard")
    public ResponseEntity<ResponseData> admin(@RequestParam(required = false) Integer days) {
        return ok(admin.build(getLoggedInUser(), days));
    }

    @GetMapping("/partner/api/shop-dashboard")
    public ResponseEntity<ResponseData> shop() {
        return ok(stores.shop(getLoggedInUser()));
    }

    @GetMapping("/partner/api/restaurant-dashboard")
    public ResponseEntity<ResponseData> restaurant() {
        return ok(stores.restaurant(getLoggedInUser()));
    }

    @GetMapping("/partner/api/vendor-dashboard")
    public ResponseEntity<ResponseData> vendor(@RequestParam(required = false) Integer days) {
        return ok(vendors.build(getLoggedInUser(), days));
    }

    private ResponseEntity<ResponseData> ok(Object data) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message("record.fetched").data(data).build());
    }
}
