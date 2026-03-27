package com.gocomat.rfq.repository;

import com.gocomat.rfq.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByRfqRfqId(Long rfqId);
    List<Participation> findBySupplierSupplierId(Long supplierId);
    Optional<Participation> findByRfqRfqIdAndSupplierSupplierId(Long rfqId, Long supplierId);
    boolean existsByRfqRfqIdAndSupplierSupplierId(Long rfqId, Long supplierId);
}
