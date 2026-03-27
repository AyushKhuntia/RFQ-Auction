package com.gocomat.rfq.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RfqCreateRequest {
    private Long buyerId;
    private String rfqName;
    private LocalDateTime bidStartTime;
    private LocalDateTime bidCloseTime;
    private LocalDateTime forcedCloseTime;
    private LocalDateTime serviceDate;

    // Auction config
    private Integer triggerWindowMinutes;
    private Integer extensionDurationMinutes;
    private String triggerType; // BID_RECEIVED, ANY_RANK_CHANGE, L1_CHANGE
}
