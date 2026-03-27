package com.gocomat.rfq.repository;

import com.gocomat.rfq.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByRfqRfqIdOrderByAmountAsc(Long rfqId);
    Optional<Bid> findFirstByRfqRfqIdOrderByAmountAsc(Long rfqId);
    List<Bid> findByRfqRfqIdAndSupplierSupplierIdOrderByCreatedAtDesc(Long rfqId, Long supplierId);
}
