package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.bean.KeyValueDto;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.DriverDTO;
import com.bt.fretchain.primary.dto.VehicleDTO;
import com.bt.fretchain.primary.service.DriverService;
import com.bt.fretchain.primary.service.VehicleCategoryService;
import com.bt.fretchain.primary.service.VehicleService;
import com.bt.fretchain.primary.pojo.SystemUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Vendor panel: a vendor manages only its own fleet. The vendor is always taken from the
 * login (never from the request), and records of other vendors are reported as "not found".
 * Verification of vehicles stays with the admin.
 */
@Service
@RequiredArgsConstructor
public class VendorFleetService {

    private final VendorService vendorService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final VehicleCategoryService vehicleCategoryService;

    // ---------------------------------------------------------------- vehicles

    @Transactional(readOnly = true)
    public DataTableResponsePacket<VehicleDTO.Detail> vehicles(SystemUser user, VehicleDTO.ListOrSearchData req) {
        String vendor = vendorUuid(user);
        if (req.getFilters() == null) {
            req.setFilters(new VehicleDTO.Filters());
        }
        req.getFilters().setVendorUuid(vendor);
        return vehicleService.getListData(req);
    }

    @Transactional(readOnly = true)
    public VehicleDTO.Detail vehicle(SystemUser user, String uuid) {
        return ownVehicle(vendorUuid(user), uuid);
    }

    @Transactional
    public VehicleDTO.Detail saveVehicle(SystemUser user, VehicleDTO.SaveOrUpdate req) {
        req.setVendorUuid(writeVendorUuid(user));
        req.setVerified(false);   // the admin verifies vehicles
        return vehicleService.save(req, user);
    }

    @Transactional
    public VehicleDTO.Detail updateVehicle(SystemUser user, String uuid, VehicleDTO.SaveOrUpdate req) {
        String vendor = writeVendorUuid(user);
        VehicleDTO.Detail existing = ownVehicle(vendor, uuid);
        req.setVendorUuid(vendor);
        req.setVerified(existing.getVerified());
        return vehicleService.update(uuid, req, user);
    }

    @Transactional
    public void vehicleStatus(SystemUser user, String uuid, boolean active) {
        ownVehicle(vendorUuid(user), uuid);
        vehicleService.updateStatus(uuid, active, user);
    }

    @Transactional(readOnly = true)
    public List<KeyValueDto> vehicleKeyValue(SystemUser user) {
        return vehicleService.getKeyValueList(vendorUuid(user));
    }

    @Transactional(readOnly = true)
    public List<KeyValueDto> vehicleCategories(SystemUser user, String lang) {
        vendorUuid(user);
        return vehicleCategoryService.getKeyValueList(lang);
    }

    // ---------------------------------------------------------------- drivers

    @Transactional(readOnly = true)
    public DataTableResponsePacket<DriverDTO.Detail> drivers(SystemUser user, DriverDTO.ListOrSearchData req) {
        String vendor = vendorUuid(user);
        if (req.getFilters() == null) {
            req.setFilters(new DriverDTO.Filters());
        }
        req.getFilters().setVendorUuid(vendor);
        return driverService.getListData(req);
    }

    @Transactional(readOnly = true)
    public DriverDTO.Detail driver(SystemUser user, String uuid) {
        return ownDriver(vendorUuid(user), uuid);
    }

    @Transactional
    public DriverDTO.Detail saveDriver(SystemUser user, DriverDTO.SaveOrUpdate req) {
        req.setVendorUuid(writeVendorUuid(user));
        return driverService.saveUnverified(req, user);
    }

    @Transactional
    public DriverDTO.Detail updateDriver(SystemUser user, String uuid, DriverDTO.SaveOrUpdate req) {
        String vendor = writeVendorUuid(user);
        ownDriver(vendor, uuid);
        req.setVendorUuid(vendor);
        return driverService.update(uuid, req, user);
    }

    @Transactional
    public void driverStatus(SystemUser user, String uuid, boolean active) {
        ownDriver(vendorUuid(user), uuid);
        driverService.updateStatus(uuid, active, user);
    }

    @Transactional(readOnly = true)
    public List<KeyValueDto> driverKeyValue(SystemUser user) {
        return driverService.getKeyValueList(vendorUuid(user));
    }

    // ---------------------------------------------------------------- helpers

    /** Writes need an approved vendor. */
    private String writeVendorUuid(SystemUser user) {
        return vendorService.currentApproved(user).getUser().getUuid();
    }

    /** Vehicles and drivers reference the vendor's login user. */
    private String vendorUuid(SystemUser user) {
        return vendorService.current(user).getUser().getUuid();
    }

    private VehicleDTO.Detail ownVehicle(String vendor, String uuid) {
        VehicleDTO.Detail d = vehicleService.getById(uuid);
        if (!Objects.equals(vendor, d.getVendorUuid())) {
            throw new ResourceNotFoundException("record.notfound");
        }
        return d;
    }

    private DriverDTO.Detail ownDriver(String vendor, String uuid) {
        DriverDTO.Detail d = driverService.getById(uuid);
        if (!Objects.equals(vendor, d.getVendorUuid())) {
            throw new ResourceNotFoundException("record.notfound");
        }
        return d;
    }
}
