package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Public driver sign-up. A driver either picks a vendor (fleet owner) or picks none,
 * which means the driver works directly for the Fretchain platform (vendor = null).
 * The driver is created unverified: the platform (or nobody else) verifies the documents
 * before the driver can log in to the driver app.
 */
@Service
@RequiredArgsConstructor
public class DriverRegistrationService {

    private final DriverService driverService;
    private final VendorService vendorService;

    /** Approved, active vendors the driver can choose from. */
    @Transactional(readOnly = true)
    public List<KeyValueDto> vendors() {
        return vendorService.keyValue();
    }

    @Transactional
    public void register(DriverDTO.SaveOrUpdate req) {
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new BadRequestException("driver.password.required");
        }
        // vendor must be one of the selectable (approved, active) vendors; blank = Fretchain platform driver
        if (req.getVendorUuid() != null && !req.getVendorUuid().isBlank()
                && vendorService.keyValue().stream().noneMatch(v -> v.getKey().equals(req.getVendorUuid()))) {
            throw new BadRequestException("driver.vendor.notapproved");
        }
        // vehicles are assigned later by the vendor / platform
        req.setVehicleUuid(null);
        driverService.saveUnverified(req, null);
    }
}
