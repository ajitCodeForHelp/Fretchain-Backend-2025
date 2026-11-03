package com.bt.fretchain.primary.controller.partner.restaurant;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.controller._BaseController;
import com.bt.fretchain.primary.dto.RestaurantSubCategoryDTO;
import com.bt.fretchain.primary.pojo.RestaurantStaff;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.service.RestaurantSubCategoryService;
import com.bt.fretchain.primary.serviceImpl.RestaurantAccessService;
import com.bt.fretchain.primary.controller.partner.PartnerRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Restaurant login: own sub categories. The restaurant always comes from the logged-in user. */
@RestController
@RequestMapping("/partner/api/restaurant-subcategory")
@RequiredArgsConstructor
public class PartnerRestaurantSubCategoryController extends _BaseController {

    private final RestaurantSubCategoryService service;
    private final RestaurantAccessService access;
    private final PartnerRequestValidator validator;

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<ResponseData> save(@RequestBody RestaurantSubCategoryDTO.SaveOrUpdate request) {
        SystemUser user = getLoggedInUser();
        RestaurantStaff staff = access.require(user, RestaurantActivity.SUBCATEGORY_ADD);
        request.setRestaurantUuid(staff.getRestaurant().getUuid());
        return ok(service.save(validator.validate(request), user), "record.saved");
    }

    @PutMapping("/{uuid}")
    @Transactional
    public ResponseEntity<ResponseData> update(@PathVariable String uuid,
                                               @RequestBody RestaurantSubCategoryDTO.SaveOrUpdate request) {
        SystemUser user = getLoggedInUser();
        RestaurantStaff staff = access.require(user, RestaurantActivity.SUBCATEGORY_EDIT);
        access.ensureSameRestaurant(service.getById(uuid).getRestaurantUuid(), staff);
        request.setRestaurantUuid(staff.getRestaurant().getUuid());
        return ok(service.update(uuid, validator.validate(request), user), "record.updated");
    }

    @GetMapping("/{uuid}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> getById(@PathVariable String uuid) {
        RestaurantStaff staff = access.require(getLoggedInUser(), RestaurantActivity.SUBCATEGORY_VIEW);
        RestaurantSubCategoryDTO.Detail detail = service.getById(uuid);
        access.ensureSameRestaurant(detail.getRestaurantUuid(), staff);
        return ok(detail, "record.fetched");
    }

    @PostMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseData> list(@RequestBody RestaurantSubCategoryDTO.ListOrSearchData request) {
        // also used by the item form's dropdowns: allowed for anyone who may add / edit items
        RestaurantStaff staff = access.requireAny(getLoggedInUser(),
                RestaurantActivity.SUBCATEGORY_VIEW, RestaurantActivity.ITEM_ADD, RestaurantActivity.ITEM_EDIT);
        request.setRestaurantUuid(staff.getRestaurant().getUuid());
        return ok(service.getListData(request), "record.list.fetched");
    }

    @PatchMapping("/{uuid}/status")
    @Transactional
    public ResponseEntity<ResponseData> updateStatus(@PathVariable String uuid, @RequestParam Boolean active) {
        SystemUser user = getLoggedInUser();
        RestaurantStaff staff = access.require(user, RestaurantActivity.SUBCATEGORY_STATUS);
        access.ensureSameRestaurant(service.getById(uuid).getRestaurantUuid(), staff);
        service.updateStatus(uuid, active, user);
        return ok(null, "status.updated");
    }

    private ResponseEntity<ResponseData> ok(Object data, String message) {
        return ResponseEntity.ok(ResponseData.builder().statusCode(200).message(message).data(data).build());
    }
}
