package com.gocomat.rfq.service;

import com.gocomat.rfq.dto.RfqCreateRequest;
import com.gocomat.rfq.model.*;
import com.gocomat.rfq.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RfqService {

    private final RfqRepository rfqRepository;
    private final BuyerRepository buyerRepository;
    private final AuctionConfigRepository auctionConfigRepository;
    private final ParticipationRepository participationRepository;
    private final SupplierRepository supplierRepository;
    private final BidRepository bidRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public Rfq createRfq(RfqCreateRequest request) {
        // Validation
        if (request.getForcedCloseTime().isBefore(request.getBidCloseTime()) ||
            request.getForcedCloseTime().isEqual(request.getBidCloseTime())) {
            throw new RuntimeException("Forced Bid Close Time must be after Bid Close Time");
        }

        Buyer buyer = buyerRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        Rfq rfq = Rfq.builder()
                .buyer(buyer)
                .rfqName(request.getRfqName())
                .bidStartTime(request.getBidStartTime())
                .bidCloseTime(request.getBidCloseTime())
                .forcedCloseTime(request.getForcedCloseTime())
                .serviceDate(request.getServiceDate())
                .status("ACTIVE")
                .build();

        rfq = rfqRepository.save(rfq);

        AuctionConfig config = AuctionConfig.builder()
                .rfq(rfq)
                .triggerWindowMinutes(request.getTriggerWindowMinutes())
                .extensionDurationMinutes(request.getExtensionDurationMinutes())
                .triggerType(request.getTriggerType())
                .build();

        auctionConfigRepository.save(config);
        rfq.setAuctionConfig(config);

        return rfq;
    }

    public List<Rfq> getAllRfqs() {
        return rfqRepository.findAll();
    }

    public List<Rfq> getRfqsByBuyer(Long buyerId) {
        return rfqRepository.findByBuyerBuyerId(buyerId);
    }

    public Rfq getRfqById(Long rfqId) {
        return rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));
    }

    public Map<String, Object> getRfqDetails(Long rfqId) {
        Rfq rfq = getRfqById(rfqId);
        List<Bid> bids = bidRepository.findByRfqRfqIdOrderByAmountAsc(rfqId);
        AuctionConfig config = rfq.getAuctionConfig();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("rfqId", rfq.getRfqId());
        details.put("rfqName", rfq.getRfqName());
        details.put("buyerName", rfq.getBuyer().getName());
        details.put("bidStartTime", rfq.getBidStartTime());
        details.put("bidCloseTime", rfq.getBidCloseTime());
        details.put("forcedCloseTime", rfq.getForcedCloseTime());
        details.put("serviceDate", rfq.getServiceDate());
        details.put("status", rfq.getStatus());

        if (config != null) {
            Map<String, Object> configMap = new LinkedHashMap<>();
            configMap.put("triggerWindowMinutes", config.getTriggerWindowMinutes());
            configMap.put("extensionDurationMinutes", config.getExtensionDurationMinutes());
            configMap.put("triggerType", config.getTriggerType());
            details.put("auctionConfig", configMap);
        }

        // Build rankings
        List<Map<String, Object>> rankings = buildRankings(bids);
        details.put("bids", rankings);

        // Lowest bid
        if (!bids.isEmpty()) {
            details.put("lowestBid", bids.get(0).getAmount());
        }

        return details;
    }

    public List<Map<String, Object>> buildRankings(List<Bid> bids) {
        // Group by supplier, keep lowest bid per supplier
        Map<Long, Bid> lowestPerSupplier = new LinkedHashMap<>();
        for (Bid bid : bids) {
            Long sid = bid.getSupplier().getSupplierId();
            if (!lowestPerSupplier.containsKey(sid) || bid.getAmount() < lowestPerSupplier.get(sid).getAmount()) {
                lowestPerSupplier.put(sid, bid);
            }
        }

        List<Bid> sortedBids = lowestPerSupplier.values().stream()
                .sorted(Comparator.comparingDouble(Bid::getAmount))
                .collect(Collectors.toList());

        List<Map<String, Object>> rankings = new ArrayList<>();
        int rank = 1;
        for (Bid bid : sortedBids) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", "L" + rank);
            entry.put("supplierId", bid.getSupplier().getSupplierId());
            entry.put("supplierName", bid.getSupplier().getName());
            entry.put("amount", bid.getAmount());
            entry.put("freightCharges", bid.getFreightCharges());
            entry.put("originCharges", bid.getOriginCharges());
            entry.put("destinationCharges", bid.getDestinationCharges());
            entry.put("transitTime", bid.getTransitTime());
            entry.put("quoteValidity", bid.getQuoteValidity());
            entry.put("bidTime", bid.getCreatedAt());
            rankings.add(entry);
            rank++;
        }
        return rankings;
    }

    @Transactional
    public Participation joinRfq(Long rfqId, Long supplierId) {
        if (participationRepository.existsByRfqRfqIdAndSupplierSupplierId(rfqId, supplierId)) {
            throw new RuntimeException("Supplier already joined this RFQ");
        }

        Rfq rfq = getRfqById(rfqId);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        Participation participation = Participation.builder()
                .rfq(rfq)
                .supplier(supplier)
                .status("ACTIVE")
                .build();

        return participationRepository.save(participation);
    }

    public List<Map<String, Object>> getAvailableRfqsForSupplier(Long supplierId) {
        List<Rfq> allRfqs = rfqRepository.findByStatus("ACTIVE");
        List<Participation> myParticipations = participationRepository.findBySupplierSupplierId(supplierId);
        Set<Long> joinedRfqIds = myParticipations.stream()
                .map(p -> p.getRfq().getRfqId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Rfq rfq : allRfqs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rfqId", rfq.getRfqId());
            map.put("rfqName", rfq.getRfqName());
            map.put("buyerName", rfq.getBuyer().getName());
            map.put("bidStartTime", rfq.getBidStartTime());
            map.put("bidCloseTime", rfq.getBidCloseTime());
            map.put("forcedCloseTime", rfq.getForcedCloseTime());
            map.put("status", rfq.getStatus());
            map.put("joined", joinedRfqIds.contains(rfq.getRfqId()));

            // Get lowest bid
            List<Bid> bids = bidRepository.findByRfqRfqIdOrderByAmountAsc(rfq.getRfqId());
            map.put("lowestBid", bids.isEmpty() ? null : bids.get(0).getAmount());
            map.put("totalBids", bids.size());

            result.add(map);
        }
        return result;
    }

    @Transactional
    public void deleteRfq(Long rfqId, Long buyerId) {
        Rfq rfq = getRfqById(rfqId);

        // Only the buyer who created it can delete
        if (!rfq.getBuyer().getBuyerId().equals(buyerId)) {
            throw new RuntimeException("Only the buyer who created this RFQ can delete it");
        }

        // Delete in order: activity logs → bids → participations → auction config → RFQ
        activityLogRepository.deleteAll(activityLogRepository.findByRfqRfqIdOrderByCreatedAtDesc(rfqId));
        bidRepository.deleteAll(bidRepository.findByRfqRfqIdOrderByAmountAsc(rfqId));
        participationRepository.deleteAll(participationRepository.findByRfqRfqId(rfqId));
        auctionConfigRepository.findByRfqRfqId(rfqId).ifPresent(auctionConfigRepository::delete);
        rfqRepository.delete(rfq);
    }
}
