package com.ecommerce.controller.catalog;

import com.ecommerce.model.entity.Product;
import com.ecommerce.service.interfaces.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productos", description = "Endpoints para gestión de productos")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Obtener productos", description = "Obtiene una lista paginada de productos activos")
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String userEmail = getCurrentUserEmail();

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userEmail != null ? userEmail : "anonymous");

        log.info("Fetching products - Page: {}, Size: {}, User: {}", page, size, userEmail);

        try {
            long startTime = System.currentTimeMillis();
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> products = productService.getAllActiveProducts(pageable);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Products fetched successfully - Count: {}, Total: {}, Duration: {}ms",
                    products.getNumberOfElements(), products.getTotalElements(), duration);

            return ResponseEntity.ok(products);

        } catch (Exception e) {
            log.error("Error fetching products - Page: {}, Size: {}, Error: {}", page, size, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Obtiene los detalles de un producto específico")
    public ResponseEntity<Product> getProduct(@PathVariable Long id, HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String userEmail = getCurrentUserEmail();

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userEmail != null ? userEmail : "anonymous");

        log.info("Fetching product by ID: {} - User: {}", id, userEmail);

        try {
            long startTime = System.currentTimeMillis();
            Product product = productService.getProductById(id);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Product found - ID: {}, Name: {}, Duration: {}ms", id, product.getName(), duration);
            return ResponseEntity.ok(product);

        } catch (Exception e) {
            log.error("Error fetching product - ID: {}, Error: {}", id, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos", description = "Busca productos por nombre o descripción")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String userEmail = getCurrentUserEmail();

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userEmail != null ? userEmail : "anonymous");

        log.info("Searching products - Query: '{}', Page: {}, Size: {}, User: {}", query, page, size, userEmail);

        try {
            long startTime = System.currentTimeMillis();
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> products = productService.searchProducts(query, pageable);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Product search completed - Query: '{}', Results: {}, Total: {}, Duration: {}ms",
                    query, products.getNumberOfElements(), products.getTotalElements(), duration);

            return ResponseEntity.ok(products);

        } catch (Exception e) {
            log.error("Error searching products - Query: '{}', Error: {}", query, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Obtener productos por categoría", description = "Obtiene productos de una categoría específica")
    public ResponseEntity<Page<Product>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String userEmail = getCurrentUserEmail();

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userEmail != null ? userEmail : "anonymous");

        log.info("Fetching products by category - CategoryID: {}, Page: {}, Size: {}, User: {}", categoryId, page, size,
                userEmail);

        try {
            long startTime = System.currentTimeMillis();
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> products = productService.getProductsByCategory(categoryId, pageable);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Products by category fetched - CategoryID: {}, Count: {}, Total: {}, Duration: {}ms",
                    categoryId, products.getNumberOfElements(), products.getTotalElements(), duration);

            return ResponseEntity.ok(products);

        } catch (Exception e) {
            log.error("Error fetching products by category - CategoryID: {}, Error: {}", categoryId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/featured")
    @Operation(summary = "Obtener productos destacados", description = "Obtiene la lista de productos destacados")
    public ResponseEntity<List<Product>> getFeaturedProducts(HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String userEmail = getCurrentUserEmail();

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userEmail != null ? userEmail : "anonymous");

        log.info("Fetching featured products - User: {}", userEmail);

        try {
            long startTime = System.currentTimeMillis();
            List<Product> products = productService.getFeaturedProducts();
            long duration = System.currentTimeMillis() - startTime;

            log.info("Featured products fetched - Count: {}, Duration: {}ms", products.size(), duration);

            return ResponseEntity.ok(products);

        } catch (Exception e) {
            log.error("Error fetching featured products - Error: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    
}
