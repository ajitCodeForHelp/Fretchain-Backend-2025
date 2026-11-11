package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.DriverAuthDTO;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.repository.DriverRepository;
import com.bt.fretchain.primary.service.DriverAuthService;
import com.bt.fretchain.security.JwtTokenUtil;
import com.bt.fretchain.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAuthServiceImpl
        implements DriverAuthService {

    private final DriverRepository driverRepository;

    private final JwtTokenUtil jwtTokenUtil;


    @Override
    public DriverAuthDTO.AuthResponse loginDriver(
            String userName,
            String password,
            DriverAuthDTO.DeviceInfo device
    ) {

        Driver driver = findDriver(userName);

        validateDriver(driver, password);

        /*
         * Update last login
         */
        driver.setLastLogin(LocalDateTime.now());

        /*
         * FCM device + location details, so new orders can be pushed to this phone
         */
        applyDevice(driver, device);

        driverRepository.save(driver);

        return generateAuthResponse(driver);
    }

    @Override
    public DriverAuthDTO.AuthResponse profile(Driver driver) {
        DriverAuthDTO.AuthResponse response = generateAuthResponse(driver);
        response.setToken(null);
        return response;
    }

    @Override
    public void updateDevice(Driver driver, DriverAuthDTO.DeviceInfo device) {
        applyDevice(driver, device);
        driverRepository.save(driver);
    }

    @Override
    public void updateLocation(Driver driver, Double latitude, Double longitude) {
        setLocation(driver, latitude, longitude);
        driverRepository.save(driver);
    }

    @Override
    public DriverAuthDTO.AuthResponse setOnline(Driver driver, boolean online, Double latitude, Double longitude) {
        driver.setOnlineStatus(online);
        if (online) {
            setLocation(driver, latitude, longitude);
        }
        driverRepository.save(driver);
        DriverAuthDTO.AuthResponse response = generateAuthResponse(driver);
        response.setToken(null);
        return response;
    }

    @Override
    public void logout(Driver driver) {
        driver.setDeviceToken(null);
        driver.setOnlineStatus(false);
        driverRepository.save(driver);
    }

    private void applyDevice(Driver driver, DriverAuthDTO.DeviceInfo device) {
        if (device == null) {
            return;
        }
        String token = device.getFcmToken() == null ? null : device.getFcmToken().trim();
        if (token != null && !token.isEmpty()) {
            /*
             * A phone belongs to one driver at a time: if someone else logged in on it earlier,
             * stop pushing their orders to this phone.
             */
            driverRepository.findAll((root, q, cb) -> cb.and(
                    cb.equal(root.get("deviceToken"), token),
                    cb.notEqual(root.get("uuid"), driver.getUuid())
            )).forEach(other -> {
                other.setDeviceToken(null);
                driverRepository.save(other);
            });
            driver.setDeviceToken(token);
        }
        if (device.getDeviceId() != null) driver.setDeviceId(cut(device.getDeviceId(), 120));
        if (device.getPlatform() != null) driver.setDevicePlatform(cut(device.getPlatform().toLowerCase(), 20));
        if (device.getDeviceModel() != null) driver.setDeviceModel(cut(device.getDeviceModel(), 120));
        if (device.getAppVersion() != null) driver.setAppVersion(cut(device.getAppVersion(), 40));
        driver.setDeviceUpdatedAt(LocalDateTime.now());
        setLocation(driver, device.getLatitude(), device.getLongitude());
    }

    private void setLocation(Driver driver, Double lat, Double lng) {
        if (lat == null || lng == null
                || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return;
        }
        driver.setCurrentLat(lat);
        driver.setCurrentLng(lng);
        driver.setLastLocationUpdatedAt(LocalDateTime.now());
    }

    private static String cut(String v, int max) {
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }


    private Driver findDriver(String userName) {

        String loginValue = userName.trim();

        /*
         * First try username
         */
        Driver driver =
                driverRepository
                        .findByUsernameAndDeletedFalse(loginValue)
                        .orElse(null);

        if (driver != null) {
            return driver;
        }

        /*
         * If username not found,
         * try mobile number.
         */
        return driverRepository
                .findByMobileAndDeletedFalse(loginValue)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "driver.not.found"
                        )
                );
    }


    private void validateDriver(
            Driver driver,
            String password
    ) {

        /*
         * Check password
         */
        if (TextUtils.isEmpty(driver.getPassword())
                || !TextUtils.matchPassword(
                        password,
                        driver.getPassword()
                )) {

            throw new BadRequestException(
                    "Invalid Credential"
            );
        }


        /*
         * Driver must be active
         */
        if (!Boolean.TRUE.equals(driver.isActive())) {

            throw new BadRequestException(
                    "driver.account.inactive"
            );
        }


        /*
         * Driver must be verified
         */
        if (!Boolean.TRUE.equals(driver.getVerified())) {

            throw new BadRequestException(
                    "driver.not.verified"
            );
        }

        /*
         * Generate unique key if missing
         */
//        if (TextUtils.isEmpty(driver.getUniqueKey())) {
//
//            driver.setUniqueKey(
//                    TextUtils.getUniqueKey()
//            );
//        }
    }


    private DriverAuthDTO.AuthResponse generateAuthResponse(
            Driver driver
    ) {

        DriverAuthDTO.AuthResponse response =
                new DriverAuthDTO.AuthResponse();


        /*
         * Generate JWT
         */
        String token =
                UUID.randomUUID().toString();


        Map<String, Object> bodyPart =
                new LinkedHashMap<>();

        bodyPart.put(
                "username",
                driver.getUsername()
        );

        bodyPart.put(
                "u-id",
                driver.getUuid()
        );

        /*
         * Important:
         * Driver needs its own user type/role.
         */
        bodyPart.put(
                "u-ty",
                "DRIVER"
        );

        bodyPart.put(
                "roles",
                List.of("DRIVER")
        );

        bodyPart.put(
                "d-id",
                driver.getUuid()
        );

        bodyPart.put(
                "token",
                token
        );


        response.setToken(
                jwtTokenUtil.generateToken(
                        bodyPart,
                        driver.getFullName()
                )
        );


        /*
         * Driver information
         */
        response.setDriverUuid(
                driver.getUuid()
        );

        response.setFullName(
                driver.getFullName()
        );

        response.setMobile(
                driver.getMobile()
        );

        response.setEmail(
                driver.getEmail()
        );

        response.setProfileImageUrl(
                driver.getProfileImageUrl()
        );

        response.setVerified(
                driver.getVerified()
        );

        response.setAvailable(
                driver.getAvailable()
        );

        response.setOnlineStatus(
                driver.getOnlineStatus()
        );

        response.setRating(
                driver.getOverallRating()
        );


        /*
         * Vehicle information
         */
        if (driver.getVehicle() != null) {

            response.setVehicleUuid(
                    driver.getVehicle().getUuid()
            );

            response.setVehicleNumber(
                    driver.getVehicle().getVehicleNumber()
            );

            if (driver.getVehicle()
                    .getVehicleCategory() != null) {

                response.setVehicleCategoryUuid(
                        driver.getVehicle()
                                .getVehicleCategory()
                                .getUuid()
                );

                response.setVehicleCategoryNameEn(
                        driver.getVehicle()
                                .getVehicleCategory()
                                .getNameEn()
                );

                response.setVehicleCategoryNameFr(
                        driver.getVehicle()
                                .getVehicleCategory()
                                .getNameFr()
                );
            }
        }

        return response;
    }
}