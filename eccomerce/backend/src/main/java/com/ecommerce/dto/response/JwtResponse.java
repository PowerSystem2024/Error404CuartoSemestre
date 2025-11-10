package com.ecommerce.dto.response;

import lombok.Data;

@Data
public class JwtResponse {

    private String token;

    public JwtResponse(String accessToken) {
        this.token = accessToken;
    }
}
