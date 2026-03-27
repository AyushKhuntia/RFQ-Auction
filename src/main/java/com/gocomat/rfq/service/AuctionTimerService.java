package com.gocomat.rfq.service;

import com.gocomat.rfq.dto.BidUpdate;
import com.gocomat.rfq.model.Rfq;
import com.gocomat.rfq.repository.RfqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionTimerService {

    private final RfqRepository rfqRepository;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 15000) // Check every 15 seconds
    @Transactional
    public void checkAuctionTimers() {
        LocalDateTime now = LocalDateTime.now();

        // Close auctions past forced close time
        List<Rfq> forceCloseList = rfqRepository.findByStatusAndForcedCloseTimeBefore("ACTIVE", now);
        for (Rfq rfq : forceCloseList) {
            rfq.setStatus("FORCE_CLOSED");
            rfqRepository.save(rfq);
            activityLogService.log(rfq, "FORCE_CLOSED", "Auction force closed at " + now);

            BidUpdate update = BidUpdate.builder()
                    .type("AUCTION_CLOSED")
                    .rfqId(rfq.getRfqId())
                    .message("🔒 Auction has been force closed")
                    .build();
            messagingTemplate.convertAndSend("/topic/rfq/" + rfq.getRfqId(), update);
            log.info("RFQ {} force closed", rfq.getRfqId());
        }

        // Close auctions past bid close time (that haven't been force closed)
        List<Rfq> closeList = rfqRepository.findByStatusAndBidCloseTimeBefore("ACTIVE", now);
        for (Rfq rfq : closeList) {
            // Only close if also past forced close time OR if bid close time has passed without extension
            if (now.isAfter(rfq.getBidCloseTime())) {
                rfq.setStatus("CLOSED");
                rfqRepository.save(rfq);
                activityLogService.log(rfq, "CLOSED", "Auction closed at " + now);

                BidUpdate update = BidUpdate.builder()
                        .type("AUCTION_CLOSED")
                        .rfqId(rfq.getRfqId())
                        .message("✅ Auction has been closed")
                        .build();
                messagingTemplate.convertAndSend("/topic/rfq/" + rfq.getRfqId(), update);
                log.info("RFQ {} closed", rfq.getRfqId());
            }
        }
    }
}
