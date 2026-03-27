package com.gocomat.rfq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BUYERS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Buyer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "buyer_seq")
    @SequenceGenerator(name = "buyer_seq", sequenceName = "BUYER_SEQ", allocationSize = 1)
    @Column(name = "BUYER_ID")
    private Long buyerId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
