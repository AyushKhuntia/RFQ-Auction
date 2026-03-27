package com.gocomat.rfq.repository;

import com.gocomat.rfq.model.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface RfqRepository extends JpaRepository<Rfq, Long> {
    List<Rfq> findByBuyerBuyerId(Long buyerId);
    List<Rfq> findByStatus(String status);
    List<Rfq> findByStatusAndBidCloseTimeBefore(String status, LocalDateTime time);
    List<Rfq> findByStatusAndForcedCloseTimeBefore(String status, LocalDateTime time);
}
