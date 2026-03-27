package com.gocomat.rfq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PARTICIPATIONS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "participation_seq")
    @SequenceGenerator(name = "participation_seq", sequenceName = "PARTICIPATION_SEQ", allocationSize = 1)
    @Column(name = "PARTICIPATION_ID")
    private Long participationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RFQ_ID", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SUPPLIER_ID", nullable = false)
    private Supplier supplier;

    @Column(name = "JOINED_AT")
    private LocalDateTime joinedAt;

    @Column(name = "STATUS")
    private String status; // ACTIVE, DROPPED

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }
}
