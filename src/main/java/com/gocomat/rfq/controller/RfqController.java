package com.gocomat.rfq.controller;

import com.gocomat.rfq.dto.RfqCreateRequest;
import com.gocomat.rfq.model.ActivityLog;
import com.gocomat.rfq.model.Participation;
import com.gocomat.rfq.model.Rfq;
import com.gocomat.rfq.service.ActivityLogService;
import com.gocomat.rfq.service.RfqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rfq")
@RequiredArgsConstructor
public class RfqController {

    private final RfqService rfqService;
    private final ActivityLogService activityLogService;

    @PostMapping
    public ResponseEntity<?> createRfq(@RequestBody RfqCreateRequest request) {
        try {
            Rfq rfq = rfqService.createRfq(request);
            return ResponseEntity.ok(rfqService.getRfqDetails(rfq.getRfqId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Rfq>> getAllRfqs() {
        return ResponseEntity.ok(rfqService.getAllRfqs());
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Rfq>> getRfqsByBuyer(@PathVariable Long buyerId) {
        return ResponseEntity.ok(rfqService.getRfqsByBuyer(buyerId));
    }

    @GetMapping("/{rfqId}")
    public ResponseEntity<?> getRfqDetails(@PathVariable Long rfqId) {
        try {
            return ResponseEntity.ok(rfqService.getRfqDetails(rfqId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{rfqId}/join")
    public ResponseEntity<?> joinRfq(@PathVariable Long rfqId, @RequestParam Long supplierId) {
        try {
            Participation p = rfqService.joinRfq(rfqId, supplierId);
            return ResponseEntity
                    .ok(Map.of("message", "Successfully joined RFQ", "participationId", p.getParticipationId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{rfqId}/activity")
    public ResponseEntity<List<ActivityLog>> getActivityLog(@PathVariable Long rfqId) {
        return ResponseEntity.ok(activityLogService.getLogsForRfq(rfqId));
    }

    @GetMapping("/supplier/{supplierId}/available")
    public ResponseEntity<?> getAvailableRfqsForSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(rfqService.getAvailableRfqsForSupplier(supplierId));
    }

    @DeleteMapping("/{rfqId}")
    public ResponseEntity<?> deleteRfq(@PathVariable Long rfqId, @RequestParam Long buyerId) {
        try {
            rfqService.deleteRfq(rfqId, buyerId);
            return ResponseEntity.ok(Map.of("message", "RFQ deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
