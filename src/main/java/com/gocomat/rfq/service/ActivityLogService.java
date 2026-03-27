package com.gocomat.rfq.service;

import com.gocomat.rfq.model.ActivityLog;
import com.gocomat.rfq.model.Rfq;
import com.gocomat.rfq.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void log(Rfq rfq, String eventType, String description) {
        ActivityLog log = ActivityLog.builder()
                .rfq(rfq)
                .eventType(eventType)
                .description(description)
                .build();
        activityLogRepository.save(log);
    }

    public List<ActivityLog> getLogsForRfq(Long rfqId) {
        return activityLogRepository.findByRfqRfqIdOrderByCreatedAtDesc(rfqId);
    }
}
