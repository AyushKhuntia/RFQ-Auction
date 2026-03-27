package com.gocomat.rfq.repository;

import com.gocomat.rfq.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByRfqRfqIdOrderByCreatedAtDesc(Long rfqId);
}
