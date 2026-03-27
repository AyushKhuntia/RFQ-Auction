package com.gocomat.rfq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BIDS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bid_seq")
    @SequenceGenerator(name = "bid_seq", sequenceName = "BID_SEQ", allocationSize = 1)
    @Column(name = "BID_ID")
    private Long bidId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RFQ_ID", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SUPPLIER_ID", nullable = false)
    private Supplier supplier;

    @Column(name = "AMOUNT", nullable = false)
    private Double amount;

    @Column(name = "FREIGHT_CHARGES")
    private Double freightCharges;

    @Column(name = "ORIGIN_CHARGES")
    private Double originCharges;

    @Column(name = "DESTINATION_CHARGES")
    private Double destinationCharges;

    @Column(name = "TRANSIT_TIME")
    private String transitTime;

    @Column(name = "QUOTE_VALIDITY")
    private String quoteValidity;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
