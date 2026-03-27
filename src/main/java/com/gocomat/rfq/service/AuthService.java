package com.gocomat.rfq.service;

import com.gocomat.rfq.dto.LoginRequest;
import com.gocomat.rfq.dto.LoginResponse;
import com.gocomat.rfq.dto.RegisterRequest;
import com.gocomat.rfq.model.Buyer;
import com.gocomat.rfq.model.Supplier;
import com.gocomat.rfq.repository.BuyerRepository;
import com.gocomat.rfq.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final BuyerRepository buyerRepository;
    private final SupplierRepository supplierRepository;

    public LoginResponse login(LoginRequest request) {
        if ("BUYER".equalsIgnoreCase(request.getRole())) {
            Buyer buyer = buyerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Buyer not found with email: " + request.getEmail()));
            if (!buyer.getPassword().equals(request.getPassword())) {
                throw new RuntimeException("Invalid password");
            }
            return LoginResponse.builder()
                    .id(buyer.getBuyerId())
                    .name(buyer.getName())
                    .email(buyer.getEmail())
                    .role("BUYER")
                    .message("Login successful")
                    .build();
        } else if ("SUPPLIER".equalsIgnoreCase(request.getRole())) {
            Supplier supplier = supplierRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Supplier not found with email: " + request.getEmail()));
            if (!supplier.getPassword().equals(request.getPassword())) {
                throw new RuntimeException("Invalid password");
            }
            return LoginResponse.builder()
                    .id(supplier.getSupplierId())
                    .name(supplier.getName())
                    .email(supplier.getEmail())
                    .role("SUPPLIER")
                    .message("Login successful")
                    .build();
        }
        throw new RuntimeException("Invalid role: " + request.getRole());
    }

    public LoginResponse register(RegisterRequest request) {
        if ("BUYER".equalsIgnoreCase(request.getRole())) {
            if (buyerRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            Buyer buyer = Buyer.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .build();
            buyer = buyerRepository.save(buyer);
            return LoginResponse.builder()
                    .id(buyer.getBuyerId())
                    .name(buyer.getName())
                    .email(buyer.getEmail())
                    .role("BUYER")
                    .message("Registration successful")
                    .build();
        } else if ("SUPPLIER".equalsIgnoreCase(request.getRole())) {
            if (supplierRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            Supplier supplier = Supplier.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .rating(request.getRating() != null ? request.getRating() : 0.0)
                    .build();
            supplier = supplierRepository.save(supplier);
            return LoginResponse.builder()
                    .id(supplier.getSupplierId())
                    .name(supplier.getName())
                    .email(supplier.getEmail())
                    .role("SUPPLIER")
                    .message("Registration successful")
                    .build();
        }
        throw new RuntimeException("Invalid role: " + request.getRole());
    }
}
