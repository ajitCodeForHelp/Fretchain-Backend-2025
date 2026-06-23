package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.CustomerFreightOrderDTO;

public interface CustomerFreightOrderService {

    CustomerFreightOrderDTO.EstimateResponse estimate(
            CustomerFreightOrderDTO.EstimateRequest request
    );

    CustomerFreightOrderDTO.SaveDraftResponse createDraft(
            CustomerFreightOrderDTO.CreateDraftRequest request
    );

    CustomerFreightOrderDTO.SaveDraftResponse updateDraft(
            String orderUuid,
            CustomerFreightOrderDTO.UpdateDraftRequest request
    );

    CustomerFreightOrderDTO.OrderDetailResponse orderDetail(String orderUuid);

    CustomerFreightOrderDTO.PlaceOrderResponse placeOrder(String orderUuid);
}