package com.ecommerce.controller;

import com.ecommerce.model.entity.Product;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.dto.response.ProductReviewResponse;
import com.ecommerce.dto.response.CategoryResponse;
import com.ecommerce.service.interfaces.ProductService;
import com.ecommerce.service.interfaces.CategoryService;
import com.ecommerce.service.interfaces.ProductReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints públicos accesibles sin autenticación")
public class PublicController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductReviewService productReviewService;

    @GetMapping("/health")
    @Operation(summary = "Verificar estado de la aplicación", description = "Endpoint para verificar que la aplicación está funcionando")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is running!");
    }

    @GetMapping("/test")
    @Operation(summary = "Endpoint de prueba", description = "Endpoint simple para probar la API")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test endpoint working!");
    }

    @GetMapping("/products")
    @Operation(summary = "Obtener productos públicos", description = "Obtiene una lista de productos activos sin requerir autenticación")
    public ResponseEntity<List<ProductResponse>> getPublicProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.getAllActiveProducts(pageable);
        List<Product> products = productPage.getContent();

        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(productResponses);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Obtener producto con reseñas", description = "Obtiene un producto específico por su ID junto con sus reseñas aprobadas sin requerir autenticación")
    public ResponseEntity<Map<String, Object>> getPublicProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        ProductResponse productResponse = convertToProductResponse(product);

        // Obtener reseñas del producto
        Pageable pageable = PageRequest.of(0, 50); // Obtener hasta 50 reseñas
        Page<ProductReviewResponse> reviews = productReviewService.getProductReviews(id, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("product", productResponse);
        response.put("reviews", reviews.getContent());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/search")
    @Operation(summary = "Buscar productos", description = "Busca productos por nombre sin requerir autenticación")
    public ResponseEntity<Page<ProductResponse>> searchPublicProducts(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        // Si no hay query o está vacío, devolver todos los productos activos
        if (query == null || query.trim().isEmpty()) {
            products = productService.getAllActiveProducts(pageable);
        } else {
            products = productService.searchProducts(query, pageable);
        }
        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
        Page<ProductResponse> responsePage = new PageImpl<>(productResponses, pageable, products.getTotalElements());
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/products/featured")
    @Operation(summary = "Obtener productos destacados", description = "Obtiene productos destacados sin requerir autenticación")
    public ResponseEntity<List<Product>> getFeaturedProducts() {
        List<Product> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    @Operation(summary = "Obtener categorías públicas", description = "Obtiene todas las categorías activas sin requerir autenticación")
    public ResponseEntity<List<CategoryResponse>> getPublicCategories() {
        List<com.ecommerce.model.entity.Category> categories = categoryService.getAllActiveCategories();
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categoryResponses);
    }

    private ProductResponse convertToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl()
                        : null)
                .category(product.getCategory() != null ? convertToCategoryResponse(product.getCategory()) : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private CategoryResponse convertToCategoryResponse(com.ecommerce.model.entity.Category category) {
        return CategoryResponse.builder()
                .id(category.getId().toString())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}