package com.gocomat.rfq.repository;

import com.gocomat.rfq.model.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    Optional<Buyer> findByEmail(String email);
    boolean existsByEmail(String email);
}
