package com.gocomat.rfq.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BidUpdate {
    private String type; // NEW_BID, RANK_CHANGE, TIME_EXTENSION, AUCTION_CLOSED
    private Long rfqId;
    private LocalDateTime newCloseTime;
    private String message;
    private List<Map<String, Object>> rankings;
}
