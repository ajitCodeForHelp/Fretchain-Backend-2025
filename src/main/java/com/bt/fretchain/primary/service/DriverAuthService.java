package com.bt.fretchain.primary.service;

import com.bt.fretchain.primary.dto.DriverAuthDTO;
import com.bt.fretchain.primary.pojo.Driver;

public interface DriverAuthService {

    DriverAuthDTO.AuthResponse loginDriver(
            String userName,
            String password,
            DriverAuthDTO.DeviceInfo device
    );

    /** The logged-in driver's profile (same shape as the login response, without a new token). */
    DriverAuthDTO.AuthResponse profile(Driver driver);

    /** Saves / refreshes the phone's FCM token and device details. */
    void updateDevice(Driver driver, DriverAuthDTO.DeviceInfo device);

    void updateLocation(Driver driver, Double latitude, Double longitude);

    DriverAuthDTO.AuthResponse setOnline(Driver driver, boolean online, Double latitude, Double longitude);

    /** Stops pushes to this phone and takes the driver offline. */
    void logout(Driver driver);
}
