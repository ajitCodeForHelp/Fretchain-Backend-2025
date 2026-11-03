package com.bt.fretchain.primary.controller.partner.shop;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.ShopCategoryDTO;
import com.bt.fretchain.primary.pojo.ShopStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.service.ShopCategoryService;
import com.bt.fretchain.primary.serviceImpl.ShopAccessService;
import com.bt.fretchain.primary.controller.partner.PartnerRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Shop login: own categories. The shop always comes from the logged-in user. */
@RestController
@RequestMapping("/partner/api/shop-category")
@RequiredArgsConstructor
public class PartnerShopCategoryController extends _BaseController {

    private final ShopCategoryService service;
    private final ShopAccessService access;
    private final PartnerRequestValidator validator;

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<ResponseData> save(@RequestBody ShopCategoryDTO.SaveOrUpdate request) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.CATEGORY_ADD);
        request.setShopUuid(staff.getShop().getUuid());
        return ok(service.save(validator.validate(request), user), "record.saved");
    }

    @PutMapping("/{uuid}")
    @Transactional
    public ResponseEntity<ResponseData> update(@PathVariable String uuid,
                                               @RequestBody ShopCategoryDTO.SaveOrUpdate request) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.CATEGORY_EDIT);
        access.ensureSameShop(service.getById(uuid).getShopUuid(), staff);
        request.setShopUuid(staff.getShop().getUuid());
        return ok(service.update(uuid, validator.validate(request), user), "record.updated");
    }

    @GetMapping("/{uuid}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> getById(@PathVariable String uuid) {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.CATEGORY_VIEW);
        ShopCategoryDTO.Detail detail = service.getById(uuid);
        access.ensureSameShop(detail.getShopUuid(), staff);
        return ok(detail, "record.fetched");
    }

    @PostMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> list(@RequestBody ShopCategoryDTO.ListOrSearchData request) {
        ShopStaff staff = access.require(getLoggedInUser(), ShopActivity.CATEGORY_VIEW);
        request.setShopUuid(staff.getShop().getUuid());
        return ok(service.getListData(request), "record.list.fetched");
    }

    @PatchMapping("/{uuid}/status")
    @Transactional
    public ResponseEntity<ResponseData> updateStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        SystemUser user = getLoggedInUser();
        ShopStaff staff = access.require(user, ShopActivity.CATEGORY_STATUS);
        access.ensureSameShop(service.getById(uuid).getShopUuid(), staff);
        service.updateStatus(uuid, active, user);
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
