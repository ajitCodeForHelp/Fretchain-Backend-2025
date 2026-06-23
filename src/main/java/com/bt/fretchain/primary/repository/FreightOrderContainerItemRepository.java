package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrderContainerDetail;
import com.bt.fretchain.primary.pojo.FreightOrderContainerItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreightOrderContainerItemRepository extends JpaRepository<FreightOrderContainerItem, Long> {

    void deleteByContainerDetail(FreightOrderContainerDetail freightOrderContainerDetail);

    List<FreightOrderContainerItem> findByContainerDetail(FreightOrderContainerDetail freightOrderContainerDetail);

}