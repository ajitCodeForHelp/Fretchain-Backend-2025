package com.bt.fretchain.primary.controller.partner.shop;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopSubCategoryDTO;
import com.bt.fretchain.primary.pojo.ShopStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.service.ShopSubCategoryService;
import com.bt.fretchain.primary.serviceImpl.ShopAccessService;
import com.bt.fretchain.primary.controller.partner.PartnerRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Shop login: own sub categories. The shop always comes from the logged-in user. */
@RestController
@RequestMapping("/partner/api/shop-subcategory")
@RequiredArgsConstructor
public class PartnerShopSubCategoryController extends _BaseController {

    private final ShopSubCategoryService service;
    private final ShopAccessService access;
    private final PartnerRequestValidator validator;

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<ResponseData> save(@RequestBody ShopSubCategoryDTO.SaveOrUpdate request) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.SUBCATEGORY_ADD);
        request.setShopUuid(staff.getShop().getUuid());
        return ok(service.save(validator.validate(request), user), "record.saved");
    }

    @PutMapping("/{uuid}")
    @Transactional
    public ResponseEntity<ResponseData> update(@PathVariable String uuid,
                                               @RequestBody ShopSubCategoryDTO.SaveOrUpdate request) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.SUBCATEGORY_EDIT);
        access.ensureSameShop(service.getById(uuid).getShopUuid(), staff);
        request.setShopUuid(staff.getShop().getUuid());
        return ok(service.update(uuid, validator.validate(request), user), "record.updated");
    }

    @GetMapping("/{uuid}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> getById(@PathVariable String uuid) {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.SUBCATEGORY_VIEW);
        ShopSubCategoryDTO.Detail detail = service.getById(uuid);
        access.ensureSameShop(detail.getShopUuid(), staff);
        return ok(detail, "record.fetched");
    }

    @PostMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> list(@RequestBody ShopSubCategoryDTO.ListOrSearchData request) {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.SUBCATEGORY_VIEW);
        request.setShopUuid(staff.getShop().getUuid());
        return ok(service.getListData(request), "record.list.fetched");
    }

    @PatchMapping("/{uuid}/status")
    @Transactional
    public ResponseEntity<ResponseData> updateStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.SUBCATEGORY_STATUS);
        access.ensureSameShop(service.getById(uuid).getShopUuid(), staff);
        service.updateStatus(uuid, active, user);
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
