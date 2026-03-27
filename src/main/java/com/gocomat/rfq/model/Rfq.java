package com.gocomat.rfq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "RFQS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rfq_seq")
    @SequenceGenerator(name = "rfq_seq", sequenceName = "RFQ_SEQ", allocationSize = 1)
    @Column(name = "RFQ_ID")
    private Long rfqId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BUYER_ID", nullable = false)
    private Buyer buyer;

    @Column(name = "RFQ_NAME", nullable = false)
    private String rfqName;

    @Column(name = "BID_START_TIME", nullable = false)
    private LocalDateTime bidStartTime;

    @Column(name = "BID_CLOSE_TIME", nullable = false)
    private LocalDateTime bidCloseTime;

    @Column(name = "FORCED_CLOSE_TIME", nullable = false)
    private LocalDateTime forcedCloseTime;

    @Column(name = "SERVICE_DATE")
    private LocalDateTime serviceDate;

    @Column(name = "STATUS", nullable = false)
    private String status; // ACTIVE, CLOSED, FORCE_CLOSED

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "rfq", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private AuctionConfig auctionConfig;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }
}
