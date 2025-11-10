package com.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {

    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String username;
}