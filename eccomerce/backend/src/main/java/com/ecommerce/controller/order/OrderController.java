package com.ecommerce.controller.order;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.request.UpdateOrderDescriptionRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.security.jwt.JwtUtils;
import com.ecommerce.service.interfaces.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administración de Órdenes", description = "APIs para gestionar órdenes")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtils.getUserIdFromJwtToken(token);
            } catch (Exception e) {
                throw new RuntimeException("Error extracting user ID from token: " + e.getMessage());
            }
        }
        throw new RuntimeException("Token no encontrado");
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear orden", description = "Crea una nueva orden desde los items del frontend")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody Map<String, Object> requestMap,
            HttpServletRequest httpRequest) throws Exception {

        log.info("=== START: Order from Frontend ===");
        log.info("Source: HTTP POST /orders");
        log.info("User-Agent: {}", httpRequest.getHeader("User-Agent"));
        log.info("Content-Type: {}", httpRequest.getContentType());

        // Loguear el Map completo
        log.info("Map Keys: {}", requestMap.keySet());
        Object itemsFromMap = requestMap.get("items");
        log.info("Items in map: {}",
                itemsFromMap == null ? "NULL"
                        : (itemsFromMap instanceof java.util.List ? ((java.util.List<?>) itemsFromMap).size() + " items"
                                : "Not a list"));

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        OrderRequest request = mapper.convertValue(requestMap, OrderRequest.class);

        log.info("After deserialization:");
        log.info("  request.getItems() = {}", request.getItems() == null ? "NULL" : request.getItems().size());
        log.info("  request.getPaymentMethod() = {}", request.getPaymentMethod());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error("No items in request");
            return ResponseEntity.badRequest().build();
        }

        Long userId = getUserIdFromToken(httpRequest);
        log.info("📦 UserId: {}", userId);

        if (userId == null) {
            log.error("❌ UserId es null");
            return ResponseEntity.status(401).build();
        }

        OrderResponse response = orderService.createOrder(request, userId);
        log.info("✅ Orden creada: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener órdenes del usuario", description = "Obtiene todas las órdenes del usuario autenticado")
    public ResponseEntity<List<OrderResponse>> getUserOrders(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        List<OrderResponse> responses = orderService.getUserOrders(userId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{orderId}/description")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar descripción de orden", description = "Actualiza la descripción de una orden específica")
    public ResponseEntity<OrderResponse> updateOrderDescription(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderDescriptionRequest request,
            HttpServletRequest httpRequest) {

        log.info("📝 OrderController: Solicitud de actualización de descripción para orden ID: {}", orderId);
        log.info("📝 OrderController: Nueva descripción: {}", request.getDescription());

        Long userId = getUserIdFromToken(httpRequest);
        log.info("📝 OrderController: UserId extraído del token: {}", userId);

        if (userId == null) {
            log.error("📝 OrderController: UserId es null - token inválido");
            return ResponseEntity.status(401).build();
        }

        OrderResponse response = orderService.updateOrderDescription(orderId, request.getDescription(), userId);
        log.info("📝 OrderController: Descripción de orden actualizada exitosamente: {}", orderId);
        return ResponseEntity.ok(response);
    }

}
