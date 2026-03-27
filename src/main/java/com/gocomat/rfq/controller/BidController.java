package com.gocomat.rfq.controller;

import com.gocomat.rfq.dto.BidRequest;
import com.gocomat.rfq.model.Bid;
import com.gocomat.rfq.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bid")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
        try {
            Bid bid = bidService.placeBid(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Bid placed successfully",
                    "bidId", bid.getBidId(),
                    "amount", bid.getAmount()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rfq/{rfqId}")
    public ResponseEntity<List<Bid>> getBidsForRfq(@PathVariable Long rfqId) {
        return ResponseEntity.ok(bidService.getBidsForRfq(rfqId));
    }
}
