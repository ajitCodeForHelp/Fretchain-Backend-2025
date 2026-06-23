package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderAddress;
import com.bt.fretchain.primary.pojo.enums.AddressRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreightOrderAddressRepository
        extends JpaRepository<FreightOrderAddress, Long> {

    List<FreightOrderAddress>
    findByFreightOrderAndAddressRole(
            FreightOrder freightOrder,
            AddressRole addressRole
    );

    void deleteByFreightOrder(FreightOrder freightOrder);

    List<FreightOrderAddress> findByFreightOrderOrderBySequenceNoAsc(FreightOrder freightOrder);

}