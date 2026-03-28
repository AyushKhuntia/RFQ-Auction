package com.gocomat.rfq.service;

import com.gocomat.rfq.dto.BidRequest;
import com.gocomat.rfq.dto.BidUpdate;
import com.gocomat.rfq.model.*;
import com.gocomat.rfq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidRepository bidRepository;
    private final RfqRepository rfqRepository;
    private final SupplierRepository supplierRepository;
    private final AuctionConfigRepository auctionConfigRepository;
    private final ParticipationRepository participationRepository;
    private final ActivityLogService activityLogService;
    private final RfqService rfqService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Bid placeBid(BidRequest request) {
        Rfq rfq = rfqRepository.findById(request.getRfqId())
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        if (!"ACTIVE".equals(rfq.getStatus())) {
            throw new RuntimeException("Auction is no longer active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(rfq.getBidStartTime())) {
            throw new RuntimeException("Auction has not started yet");
        }
        if (now.isAfter(rfq.getBidCloseTime())) {
            throw new RuntimeException("Auction bidding time has ended");
        }

        // Check supplier participation
        if (!participationRepository.existsByRfqRfqIdAndSupplierSupplierId(request.getRfqId(),
                request.getSupplierId())) {
            throw new RuntimeException("Supplier has not joined this RFQ");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        // Calculate total amount
        double amount = (request.getFreightCharges() != null ? request.getFreightCharges() : 0)
                + (request.getOriginCharges() != null ? request.getOriginCharges() : 0)
                + (request.getDestinationCharges() != null ? request.getDestinationCharges() : 0);

        // Get L1 before this bid
        Optional<Bid> previousL1 = bidRepository.findFirstByRfqRfqIdOrderByAmountAsc(request.getRfqId());
        Long previousL1SupplierId = previousL1.map(b -> b.getSupplier().getSupplierId()).orElse(null);

        // Get previous rankings
        List<Bid> previousBids = bidRepository.findByRfqRfqIdOrderByAmountAsc(request.getRfqId());
        List<Long> previousRankOrder = getSupplierRankOrder(previousBids);

        // Save bid
        Bid bid = Bid.builder()
                .rfq(rfq)
                .supplier(supplier)
                .amount(amount)
                .freightCharges(request.getFreightCharges())
                .originCharges(request.getOriginCharges())
                .destinationCharges(request.getDestinationCharges())
                .transitTime(request.getTransitTime())
                .quoteValidity(request.getQuoteValidity())
                .build();

        bid = bidRepository.save(bid);

        // Log bid
        activityLogService.log(rfq, "BID_PLACED",
                supplier.getName() + " placed bid of ₹" + String.format("%.2f", amount));

        // Get new rankings
        List<Bid> newBids = bidRepository.findByRfqRfqIdOrderByAmountAsc(request.getRfqId());
        List<Long> newRankOrder = getSupplierRankOrder(newBids);

        // Check for rank changes
        boolean anyRankChanged = !previousRankOrder.equals(newRankOrder);
        boolean l1Changed = false;

        Optional<Bid> newL1 = bidRepository.findFirstByRfqRfqIdOrderByAmountAsc(request.getRfqId());
        Long newL1SupplierId = newL1.map(b -> b.getSupplier().getSupplierId()).orElse(null);

        if (previousL1SupplierId != null && newL1SupplierId != null && !previousL1SupplierId.equals(newL1SupplierId)) {
            l1Changed = true;
            activityLogService.log(rfq, "RANK_CHANGED",
                    "New lowest bidder (L1): " + supplier.getName());
        } else if (anyRankChanged) {
            activityLogService.log(rfq, "RANK_CHANGED", "Supplier rankings changed");
        }

        // Check auction extension
        checkAndExtendAuction(rfq, anyRankChanged, l1Changed);

        // Broadcast via WebSocket
        List<Map<String, Object>> rankings = rfqService.buildRankings(newBids);
        BidUpdate update = BidUpdate.builder()
                .type("NEW_BID")
                .rfqId(rfq.getRfqId())
                .newCloseTime(rfq.getBidCloseTime())
                .message(supplier.getName() + " placed a new bid")
                .rankings(rankings)
                .build();

        messagingTemplate.convertAndSend("/topic/rfq/" + rfq.getRfqId(), update);

        return bid;
    }

    private void checkAndExtendAuction(Rfq rfq, boolean anyRankChanged, boolean l1Changed) {
        AuctionConfig config = rfq.getAuctionConfig();
        if (config == null)
            return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime triggerStart = rfq.getBidCloseTime().minusMinutes(config.getTriggerWindowMinutes());

        // Check if we're within the trigger window
        if (now.isBefore(triggerStart)) {
            return; // Not in trigger window
        }

        boolean shouldExtend = false;
        String reason = "";

        switch (config.getTriggerType()) {
            case "BID_RECEIVED":
                shouldExtend = true;
                reason = "Bid received within last " + config.getTriggerWindowMinutes() + " minutes";
                break;
            case "ANY_RANK_CHANGE":
                if (anyRankChanged) {
                    shouldExtend = true;
                    reason = "Supplier rank changed within last " + config.getTriggerWindowMinutes() + " minutes";
                }
                break;
            case "L1_CHANGE":
                if (l1Changed) {
                    shouldExtend = true;
                    reason = "Lowest bidder (L1) changed within last " + config.getTriggerWindowMinutes() + " minutes";
                }
                break;
        }

        if (shouldExtend) {
            LocalDateTime newCloseTime = rfq.getBidCloseTime().plusMinutes(config.getExtensionDurationMinutes());

            // Never extend beyond forced close time
            if (newCloseTime.isAfter(rfq.getForcedCloseTime())) {
                newCloseTime = rfq.getForcedCloseTime();
            }

            if (newCloseTime.isAfter(rfq.getBidCloseTime())) {
                rfq.setBidCloseTime(newCloseTime);
                rfqRepository.save(rfq);

                activityLogService.log(rfq, "EXTENDED",
                        "Auction extended by " + config.getExtensionDurationMinutes() +
                                " minutes. Reason: " + reason + ". New close time: " + newCloseTime);

                // Broadcast time extension
                BidUpdate timeUpdate = BidUpdate.builder()
                        .type("TIME_EXTENSION")
                        .rfqId(rfq.getRfqId())
                        .newCloseTime(newCloseTime)
                        .message("⏰ Auction extended! " + reason)
                        .build();

                messagingTemplate.convertAndSend("/topic/rfq/" + rfq.getRfqId(), timeUpdate);

                log.info("Auction {} extended to {}", rfq.getRfqId(), newCloseTime);
            }
        }
    }

    private List<Long> getSupplierRankOrder(List<Bid> bids) {
        Map<Long, Double> lowestPerSupplier = new LinkedHashMap<>();
        for (Bid bid : bids) {
            Long sid = bid.getSupplier().getSupplierId();
            if (!lowestPerSupplier.containsKey(sid) || bid.getAmount() < lowestPerSupplier.get(sid)) {
                lowestPerSupplier.put(sid, bid.getAmount());
            }
        }
        return lowestPerSupplier.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<Bid> getBidsForRfq(Long rfqId) {
        return bidRepository.findByRfqRfqIdOrderByAmountAsc(rfqId);
    }
}
