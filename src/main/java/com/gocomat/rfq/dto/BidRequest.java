package com.gocomat.rfq.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BidRequest {
    private Long rfqId;
    private Long supplierId;
    private Double freightCharges;
    private Double originCharges;
    private Double destinationCharges;
    private String transitTime;
    private String quoteValidity;
}
