package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.FreightOrderDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreightOrderDocumentRepository extends JpaRepository<FreightOrderDocument, Long> {

    void deleteByFreightOrder(FreightOrder freightOrder);

    List<FreightOrderDocument> findByFreightOrder(FreightOrder freightOrder);


}