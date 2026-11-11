package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository
        extends JpaRepository<Address, Long> {

    /** Default address first, then the newest. */
    List<Address> findByCustomer_RecordIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(Long customerId);

    Optional<Address> findByUuidAndDeletedFalse(String uuid);
}
