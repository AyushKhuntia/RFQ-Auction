package com.gocomat.rfq.repository;

import com.gocomat.rfq.model.AuctionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuctionConfigRepository extends JpaRepository<AuctionConfig, Long> {
    Optional<AuctionConfig> findByRfqRfqId(Long rfqId);
}
