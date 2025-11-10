package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.ShoppingCartRequest;
import com.ecommerce.dto.response.ShoppingCartResponse;
import com.ecommerce.model.entity.ShoppingCart;

public interface ShoppingCartService {

    ShoppingCartResponse getCartByUser(String userId);

    ShoppingCartResponse addItemToCart(Long userId, ShoppingCartRequest request);

    ShoppingCartResponse updateCartItem(String userId, String itemId, Integer quantity);

    ShoppingCartResponse removeItemFromCart(Long userId, String itemId);

    void clearCart(Long userId);

    ShoppingCart getOrCreateCart(Long userId);

    ShoppingCartResponse updateCartItem(Long userId, String itemId, Integer quantity);

    ShoppingCartResponse getCartByUser(Long userId);

    // Guest cart methods
    ShoppingCartResponse getCartBySession(String sessionId);

    ShoppingCartResponse addItemToGuestCart(String sessionId, ShoppingCartRequest request);

    ShoppingCartResponse updateGuestCartItem(String sessionId, String itemId, Integer quantity);

    ShoppingCartResponse removeItemFromGuestCart(String sessionId, String itemId);

    void clearGuestCart(String sessionId);

    ShoppingCart getOrCreateGuestCart(String sessionId);
}
