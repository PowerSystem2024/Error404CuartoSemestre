package com.ecommerce.service.impl;

import com.ecommerce.dto.request.ShoppingCartRequest;
import com.ecommerce.dto.response.ShoppingCartItemResponse;
import com.ecommerce.dto.response.ShoppingCartResponse;
import com.ecommerce.model.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.interfaces.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShoppingCartServiceImpl.class);

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public ShoppingCartResponse getCartByUser(Long userId) {
        // log.info("Getting cart for user: {}", userId);
        ShoppingCart cart = getOrCreateCart(userId);
        // log.info("Cart loaded with {} items", cart.getItems().size());

        return mapToShoppingCartResponse(cart);
    }

    @Override
    public ShoppingCartResponse getCartByUser(String userId) {
        Long id = Long.valueOf(userId);
        ShoppingCart cart = getOrCreateCart(id);

        return mapToShoppingCartResponse(cart);
    }

    @Override
    @Transactional
    public ShoppingCartResponse addItemToCart(Long userId, ShoppingCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Product price is invalid: " + product.getPrice());
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        ShoppingCart cart = getOrCreateCart(userId);
        // log.info("Cart obtained - UserId: {}, Items: {}", userId,
        // cart.getItems().size());

        // Verificar si el producto ya está en el carrito
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Actualizar cantidad
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            // log.info("Item updated - ProductId: {}, Qty: {}", request.getProductId(),
            // item.getQuantity());
        } else {
            // Crear nuevo item
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();

            cart.getItems().add(newItem);
            // log.info("New item added - ProductId: {}, Qty: {}", request.getProductId(),
            // request.getQuantity());
        }

        cart.setUpdatedAt(LocalDateTime.now());
        // log.info("Saving cart - Items: {}", cart.getItems().size());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        // Guardar EXPLÍCITAMENTE cada CartItem para asegurar que se persistan
        for (CartItem item : cart.getItems()) {
            if (item.getId() == null) { // Solo guardar items nuevos
                item.setCart(savedCart);
                cartItemRepository.save(item);
                // log.info("CartItem saved - ProductId: {}", item.getProduct().getId());
            }
        }

        // log.info("Cart saved - Items: {}", savedCart.getItems().size());

        return mapToShoppingCartResponse(savedCart);
    }

    @Override
    @Transactional
    public ShoppingCartResponse removeItemFromCart(Long userId, String itemId) {
        ShoppingCart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(Long.valueOf(itemId)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart.setUpdatedAt(LocalDateTime.now());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        // log.info("Item removed from cart for user: {}", userId);
        return mapToShoppingCartResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        ShoppingCart cart = getOrCreateCart(userId);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());
        shoppingCartRepository.save(cart);

        // log.info("Cart cleared for user: {}", userId);
    }

    @Override
    public ShoppingCart getOrCreateCart(Long userId) {
        // log.info("Getting or creating cart for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Buscar cartosI activos del usuario ordenados por fecha de creación
        java.util.List<ShoppingCart> existingCarts = shoppingCartRepository.findByUserWithItems(user);

        if (!existingCarts.isEmpty()) {
            // Tomar el carrito más reciente
            ShoppingCart cart = existingCarts.get(0);

            // Log del estado del carrito
            // int itemCount = cart.getItems() != null ? cart.getItems().size() : 0;
            // log.info("Cart found - ID: {}, Items: {}", cart.getId(), itemCount);

            // Log detallado de items
            if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                // for (int i = 0; i < cart.getItems().size(); i++) {
                // log.debug("Item {}: ProductId={}, Qty={}", i, ...);
                // }
            }

            return cart;
        }

        // log.info("Creating new cart for user: {}", userId);
        ShoppingCart newCart = ShoppingCart.builder()
                .user(user)
                .build();

        return shoppingCartRepository.save(newCart);
    }

    private ShoppingCartResponse mapToShoppingCartResponse(ShoppingCart cart) {
        List<ShoppingCartItemResponse> items = cart.getItems().stream()
                .map(this::mapToShoppingCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(ShoppingCartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ShoppingCartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId().toString())
                .items(items)
                .totalAmount(totalAmount)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private ShoppingCartItemResponse mapToShoppingCartItemResponse(CartItem item) {
        // log.info("Mapping cart item for product: {}", item.getProduct().getName());

        // Las imágenes ya están cargadas desde el JOIN FETCH en
        // findByUserWithItems
        Product product = item.getProduct();
        // log.info("Product loaded: {}", product != null);
        if (product != null && product.getImages() != null) {
            // log.info("Product has {} images", product.getImages().size());
            // product.getImages().forEach(img -> log.info("Image URL: {}",
            // img.getImageUrl()));
        }

        String productImage = (product != null && product.getImages() != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        // log.info("Selected product image: {}", productImage);

        BigDecimal unitPrice = item.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid unit price for cart item {}: {}", item.getId(), unitPrice);
            throw new RuntimeException("Cart item has invalid price: " + unitPrice);
        }

        return ShoppingCartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId().toString())
                .productName(item.getProduct().getName())
                .productImage(productImage)
                .quantity(item.getQuantity())
                .price(unitPrice)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    @Override
    public ShoppingCartResponse updateCartItem(String userId, String itemId, Integer quantity) {
        Long id = Long.valueOf(userId);
        return updateCartItem(id, itemId, quantity);
    }

    @Override
    @Transactional
    public ShoppingCartResponse updateCartItem(Long userId, String itemId, Integer quantity) {
        ShoppingCart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(Long.valueOf(itemId)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            // Update quantity
            item.setQuantity(quantity);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        // log.info("Cart item updated for user: {}", userId);
        return mapToShoppingCartResponse(savedCart);
    }

    // Guest cart methods implementation
    @Override
    public ShoppingCartResponse getCartBySession(String sessionId) {
        ShoppingCart cart = getOrCreateGuestCart(sessionId);
        return mapToShoppingCartResponse(cart);
    }

    @Override
    @Transactional
    public ShoppingCartResponse addItemToGuestCart(String sessionId, ShoppingCartRequest request) {
        ShoppingCart cart = getOrCreateGuestCart(sessionId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        return mapToShoppingCartResponse(savedCart);
    }

    @Override
    @Transactional
    public ShoppingCartResponse updateGuestCartItem(String sessionId, String itemId, Integer quantity) {
        ShoppingCart cart = getOrCreateGuestCart(sessionId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().toString().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        cart.setUpdatedAt(LocalDateTime.now());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        return mapToShoppingCartResponse(savedCart);
    }

    @Override
    @Transactional
    public ShoppingCartResponse removeItemFromGuestCart(String sessionId, String itemId) {
        ShoppingCart cart = getOrCreateGuestCart(sessionId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().toString().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart.setUpdatedAt(LocalDateTime.now());
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        return mapToShoppingCartResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearGuestCart(String sessionId) {
        ShoppingCart cart = getOrCreateGuestCart(sessionId);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        cart.setUpdatedAt(LocalDateTime.now());
        shoppingCartRepository.save(cart);
    }

    @Override
    public ShoppingCart getOrCreateGuestCart(String sessionId) {
        Optional<ShoppingCart> existingCart = shoppingCartRepository.findBySessionId(sessionId);

        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        ShoppingCart newCart = ShoppingCart.builder()
                .sessionId(sessionId)
                .build();

        return shoppingCartRepository.save(newCart);
    }
}
