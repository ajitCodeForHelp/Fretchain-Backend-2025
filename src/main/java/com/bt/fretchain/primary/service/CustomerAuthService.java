package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.CustomerAuthDTO;

public interface CustomerAuthService {

    void sendOtp(CustomerAuthDTO.SendOtp request);

    CustomerAuthDTO.AuthResponse verifyOtp(CustomerAuthDTO.VerifyOtp request);
}