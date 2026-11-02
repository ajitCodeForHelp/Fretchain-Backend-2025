package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.DriverAuthDTO;

public interface DriverAuthService {

    DriverAuthDTO.AuthResponse loginDriver(
            String userName,
            String password
    );
}