package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository
        extends JpaRepository<Address, Long> {
}