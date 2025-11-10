package com.ecommerce.controller.payment;

import com.ecommerce.dto.request.ShoppingCartRequest;
import com.ecommerce.dto.response.ShoppingCartResponse;
import com.ecommerce.security.userdetails.UserDetailsImpl;
import com.ecommerce.service.interfaces.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Administración del Carrito", description = "APIs para gestionar el carrito de compras")
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener carrito de compras", description = "Obtiene el carrito de compras del usuario actual")
    public ResponseEntity<ShoppingCartResponse> getCart(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        ShoppingCartResponse cart = shoppingCartService.getCartByUser(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Agregar producto al carrito", description = "Agrega un producto al carrito de compras")
    public ResponseEntity<ShoppingCartResponse> addItemToCart(
            @RequestBody ShoppingCartRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        ShoppingCartResponse cart = shoppingCartService.addItemToCart(userId, request);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar item del carrito", description = "Actualiza la cantidad de un item en el carrito")
    public ResponseEntity<ShoppingCartResponse> updateCartItem(
            @PathVariable String itemId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        ShoppingCartResponse cart = shoppingCartService.updateCartItem(userId, itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar item del carrito", description = "Elimina un item del carrito de compras")
    public ResponseEntity<ShoppingCartResponse> removeItemFromCart(
            @PathVariable String itemId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        ShoppingCartResponse cart = shoppingCartService.removeItemFromCart(userId, itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vaciar carrito", description = "Elimina todos los items del carrito de compras")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        shoppingCartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }

    // Guest cart endpoints (no authentication required)
    @GetMapping("/guest-cart")
    @Operation(summary = "Obtener carrito de invitado", description = "Obtiene el carrito de compras de un usuario invitado usando sessionId")
    public ResponseEntity<ShoppingCartResponse> getGuestCart(@RequestParam String sessionId) {
        ShoppingCartResponse cart = shoppingCartService.getCartBySession(sessionId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/guest-cart/items")
    @Operation(summary = "Agregar producto al carrito de invitado", description = "Agrega un producto al carrito de compras de un usuario invitado")
    public ResponseEntity<ShoppingCartResponse> addItemToGuestCart(
            @RequestBody ShoppingCartRequest request,
            @RequestParam String sessionId) {
        ShoppingCartResponse cart = shoppingCartService.addItemToGuestCart(sessionId, request);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/guest-cart/items/{itemId}")
    @Operation(summary = "Actualizar item del carrito de invitado", description = "Actualiza la cantidad de un item en el carrito de un usuario invitado")
    public ResponseEntity<ShoppingCartResponse> updateGuestCartItem(
            @PathVariable String itemId,
            @RequestParam Integer quantity,
            @RequestParam String sessionId) {
        ShoppingCartResponse cart = shoppingCartService.updateGuestCartItem(sessionId, itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/guest-cart/items/{itemId}")
    @Operation(summary = "Eliminar item del carrito de invitado", description = "Elimina un item del carrito de compras de un usuario invitado")
    public ResponseEntity<ShoppingCartResponse> removeItemFromGuestCart(
            @PathVariable String itemId,
            @RequestParam String sessionId) {
        ShoppingCartResponse cart = shoppingCartService.removeItemFromGuestCart(sessionId, itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/guest-cart")
    @Operation(summary = "Vaciar carrito de invitado", description = "Elimina todos los items del carrito de compras de un usuario invitado")
    public ResponseEntity<Void> clearGuestCart(@RequestParam String sessionId) {
        shoppingCartService.clearGuestCart(sessionId);
        return ResponseEntity.ok().build();
    }
}
