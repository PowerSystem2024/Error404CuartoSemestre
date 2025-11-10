package com.ecommerce.event;

public record EmailVerificationEvent(Long userId, String email) {
}

