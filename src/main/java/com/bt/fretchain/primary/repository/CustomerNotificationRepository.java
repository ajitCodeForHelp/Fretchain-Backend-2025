package com.bt.fretchain.primary.repository;

import com.bt.fretchain.primary.pojo.CustomerNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

    List<CustomerNotification> findByCustomerRecordIdAndDeletedFalseOrderByCreatedAtDesc(Long customerId, Pageable page);

    long countByCustomerRecordIdAndDeletedFalseAndReadAtIsNull(Long customerId);
}
