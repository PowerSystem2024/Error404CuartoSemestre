package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;

    @Size(max = 255, message = "Address line 2 must be less than 255 characters")
    private String address2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;

    @Size(max = 100, message = "State must be less than 100 characters")
    private String state;

    @NotBlank(message = "Zip code is required")
    @Size(max = 20, message = "Zip code must be less than 20 characters")
    private String zipCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must be less than 100 characters")
    private String country;

    @Size(max = 20, message = "Phone must be less than 20 characters")
    private String phone;

    @Size(max = 500, message = "Instructions must be less than 500 characters")
    private String instructions;

    private Boolean isDefault;
}
