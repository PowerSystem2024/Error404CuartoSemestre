package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String type;
    private String firstName;
    private String lastName;
    private String fullName; // Computed field
    private String address;
    private String address2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String fullAddress; // Computed field
    private String phone;
    private String instructions;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}