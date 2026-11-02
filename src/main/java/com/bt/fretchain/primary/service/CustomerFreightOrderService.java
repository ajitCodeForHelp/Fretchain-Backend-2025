package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.CustomerFreightOrderDTO;
import com.bt.fretchain.primary.pojo.SystemUser;

public interface CustomerFreightOrderService {

    CustomerFreightOrderDTO.EstimateResponse estimate(
            CustomerFreightOrderDTO.EstimateRequest request
    );

    CustomerFreightOrderDTO.SaveDraftResponse createDraft(
            CustomerFreightOrderDTO.CreateDraftRequest request , SystemUser customer
            );

    CustomerFreightOrderDTO.SaveDraftResponse updateDraft(
            String orderUuid,
            CustomerFreightOrderDTO.UpdateDraftRequest request, SystemUser customer
    );

    CustomerFreightOrderDTO.OrderDetailResponse orderDetail(String orderUuid, SystemUser customer);

    CustomerFreightOrderDTO.PlaceOrderResponse placeOrder(String orderUuid, SystemUser customer);

    CustomerFreightOrderDTO.OrderDetailResponse getDraftOrder(
            SystemUser customer,
            String categoryUuid,
            String subCategoryUuid
    );
}