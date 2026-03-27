package com.gocomat.rfq.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "AUCTION_CONFIGS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "config_seq")
    @SequenceGenerator(name = "config_seq", sequenceName = "CONFIG_SEQ", allocationSize = 1)
    @Column(name = "CONFIG_ID")
    private Long configId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RFQ_ID", nullable = false, unique = true)
    @JsonIgnore
    private Rfq rfq;

    @Column(name = "TRIGGER_WINDOW_MINUTES", nullable = false)
    private Integer triggerWindowMinutes; // X

    @Column(name = "EXTENSION_DURATION_MINUTES", nullable = false)
    private Integer extensionDurationMinutes; // Y

    @Column(name = "TRIGGER_TYPE", nullable = false)
    private String triggerType; // BID_RECEIVED, ANY_RANK_CHANGE, L1_CHANGE
}
