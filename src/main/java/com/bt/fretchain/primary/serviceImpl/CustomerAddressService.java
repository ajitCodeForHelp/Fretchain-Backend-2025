package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.ForbiddenException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.CustomerAddressDTO.*;
import com.bt.fretchain.primary.pojo.Address;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.primary.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Customer address book: the places a customer orders to (the app lets them pick one as the delivery location). */
@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private static final int MAX_ADDRESSES = 20;

    private final AddressRepository repository;

    @Transactional(readOnly = true)
    public List<Detail> list(SystemUser customer) {
        requireCustomer(customer);
        return repository.findByCustomer_RecordIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(customer.getRecordId())
                .stream().map(this::toDetail).toList();
    }

    @Transactional
    public Detail save(SaveOrUpdate req, SystemUser customer) {
        requireCustomer(customer);
        List<Address> mine = repository.findByCustomer_RecordIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(customer.getRecordId());
        if (mine.size() >= MAX_ADDRESSES) {
            throw new com.bt.fretchain.exception.BadRequestException("address.limit");
        }
        Address a = new Address();
        a.setCustomer(customer);
        apply(a, req);
        a.setCreatedBy(customer.getRecordId());
        a.setModifiedBy(customer.getRecordId());
        // the first address is the default; a new default replaces the old one
        boolean makeDefault = mine.isEmpty() || Boolean.TRUE.equals(req.getDefaultAddress());
        if (makeDefault) {
            mine.forEach(x -> x.setDefaultAddress(false));
            repository.saveAll(mine);
        }
        a.setDefaultAddress(makeDefault);
        return toDetail(repository.save(a));
    }

    @Transactional
    public Detail update(String uuid, SaveOrUpdate req, SystemUser customer) {
        Address a = own(uuid, customer);
        apply(a, req);
        a.setModifiedBy(customer.getRecordId());
        if (Boolean.TRUE.equals(req.getDefaultAddress())) {
            return setDefault(uuid, customer);
        }
        return toDetail(repository.save(a));
    }

    @Transactional
    public Detail setDefault(String uuid, SystemUser customer) {
        Address target = own(uuid, customer);
        for (Address x : repository.findByCustomer_RecordIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(customer.getRecordId())) {
            x.setDefaultAddress(x.getRecordId().equals(target.getRecordId()));
            repository.save(x);
        }
        return toDetail(own(uuid, customer));
    }

    @Transactional
    public void delete(String uuid, SystemUser customer) {
        Address a = own(uuid, customer);
        boolean wasDefault = Boolean.TRUE.equals(a.getDefaultAddress());
        a.setDeleted(true);
        a.setDefaultAddress(false);
        a.setModifiedBy(customer.getRecordId());
        repository.save(a);
        if (wasDefault) {
            // the newest remaining address becomes the default
            repository.findByCustomer_RecordIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(customer.getRecordId())
                    .stream().findFirst().ifPresent(x -> {
                        x.setDefaultAddress(true);
                        repository.save(x);
                    });
        }
    }

    private Address own(String uuid, SystemUser customer) {
        requireCustomer(customer);
        return repository.findByUuidAndDeletedFalse(uuid)
                .filter(a -> a.getCustomer().getRecordId().equals(customer.getRecordId()))
                .orElseThrow(() -> new ResourceNotFoundException("record.notfound"));
    }

    private static void apply(Address a, SaveOrUpdate req) {
        a.setAddressType(blankToNull(req.getAddressType()) == null ? "OTHER" : req.getAddressType().trim().toUpperCase());
        a.setAddressName(blankToNull(req.getAddressName()));
        a.setContactName(blankToNull(req.getContactName()));
        a.setContactNumber(blankToNull(req.getContactNumber()));
        a.setFullAddress(req.getFullAddress().trim());
        a.setLandmark(blankToNull(req.getLandmark()));
        a.setLatitude(req.getLatitude());
        a.setLongitude(req.getLongitude());
    }

    private Detail toDetail(Address a) {
        Detail d = new Detail();
        d.setUuid(a.getUuid());
        d.setAddressType(a.getAddressType());
        d.setAddressName(a.getAddressName());
        d.setContactName(a.getContactName());
        d.setContactNumber(a.getContactNumber());
        d.setFullAddress(a.getFullAddress());
        d.setLandmark(a.getLandmark());
        d.setLatitude(a.getLatitude());
        d.setLongitude(a.getLongitude());
        d.setDefaultAddress(Boolean.TRUE.equals(a.getDefaultAddress()));
        return d;
    }

    private static void requireCustomer(SystemUser user) {
        if (user == null || user.getUserType() != UserTypeEnum.CUSTOMER) {
            throw new ForbiddenException("access.denied");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
