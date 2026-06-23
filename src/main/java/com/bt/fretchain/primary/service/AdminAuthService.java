package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.AdminAuthDto;

public interface AdminAuthService {

    AdminAuthDto.UserDetails loginAdmin(
            String userName,
            String password
    );
}
