package com.gocomat.rfq.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String role; // BUYER or SUPPLIER
    private Double rating; // only for suppliers
}
