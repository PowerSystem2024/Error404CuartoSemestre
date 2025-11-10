package com.ecommerce.service.impl;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.request.OrderItemRequest;
import com.ecommerce.dto.response.AddressResponse;
import com.ecommerce.dto.response.OrderItemResponse;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderStatisticsResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.*;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.interfaces.AddressService;
import com.ecommerce.service.interfaces.OrderService;
import com.ecommerce.service.interfaces.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final UserRepository userRepository;
        private final ProfileRepository profileRepository;
        private final ShoppingCartService shoppingCartService;
        private final AddressService addressService;

        @Override
        public OrderResponse createOrder(OrderRequest request, Long userId) {
                log.info("🛒 OrderService: Creando orden para usuario: {}", userId);
                log.info("� OrderRequest recibido: items={}, shippingAddressId={}, billingAddressId={}, paymentMethod={}",
                                request.getItems() != null ? request.getItems().size() : "NULL",
                                request.getShippingAddressId(),
                                request.getBillingAddressId(),
                                request.getPaymentMethod());

                // DEBUG: Loguear cada item del request si existen
                if (request.getItems() != null && !request.getItems().isEmpty()) {
                        for (int i = 0; i < request.getItems().size(); i++) {
                                OrderItemRequest item = request.getItems().get(i);
                                log.info("  Item {}: ProductId={}, Quantity={}, Price={}",
                                                i, item.getProductId(), item.getQuantity(), item.getPrice());
                        }
                } else {
                        log.warn("⚠️ OrderRequest NO tiene items - se usará el carrito backend");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado: " + userId));

                // Obtener direcciones de envío y facturación
                Address shippingAddress = null;
                Address billingAddress = null;

                if (request.getShippingAddressId() != null) {
                        shippingAddress = addressService.findById(request.getShippingAddressId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Dirección de envío no encontrada"));
                        if (!shippingAddress.getUser().getId().equals(userId)) {
                                throw new IllegalArgumentException("La dirección de envío no pertenece al usuario");
                        }
                }

                if (request.getBillingAddressId() != null) {
                        billingAddress = addressService.findById(request.getBillingAddressId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Dirección de facturación no encontrada"));
                        if (!billingAddress.getUser().getId().equals(userId)) {
                                throw new IllegalArgumentException(
                                                "La dirección de facturación no pertenece al usuario");
                        }
                }

                // IMPORTANTE: Si el request trae items, usarlos en lugar del carrito
                // Esto evita acumulación de items viejos en el carrito
                List<CartItem> itemsForOrder;
                String itemsSource = "BACKEND_CARRITO"; // SIEMPRE del backend

                // log.info("=== ORDER SERVICE ===");
                // log.info("IMPORTANTE: Ignorando items del frontend");
                // log.info("SIEMPRE usando carrito del backend");

                // SIEMPRE obtener el carrito del backend - NO confiar en items del frontend
                ShoppingCart cart = shoppingCartService.getOrCreateCart(userId);
                // log.info("Carrito obtenido - UserId: {}, Items: {}", userId,
                // cart.getItems() != null ? cart.getItems().size() : 0);

                // Log detallado de cada item del carrito
                if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                        // log.info("Items en carrito:");
                        // for (int i = 0; i < cart.getItems().size(); i++) {
                        // log.info("Item {}: ProductId={}, Name={}, Qty={}", ...);
                        // }
                } else {
                        log.error("Cart is empty for user: {}", userId);
                        throw new IllegalArgumentException("El carrito de compras está vacío");
                }

                itemsForOrder = cart.getItems();

                // Calcular el total
                BigDecimal totalAmount = itemsForOrder.stream()
                                .map(item -> {
                                        BigDecimal unitPrice = item.getUnitPrice();
                                        Integer quantity = item.getQuantity();
                                        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                                                log.warn("Item con precio inválido - Product ID: {}, UnitPrice: {}",
                                                                item.getProduct().getId(), unitPrice);
                                                return BigDecimal.ZERO;
                                        }
                                        if (quantity == null || quantity <= 0) {
                                                log.warn("Item con cantidad inválida - Product ID: {}, Quantity: {}",
                                                                item.getProduct().getId(), quantity);
                                                return BigDecimal.ZERO;
                                        }
                                        return unitPrice.multiply(BigDecimal.valueOf(quantity));
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Verificar que el total no sea cero
                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        log.error("Total de orden calculado es cero o negativo - UserId: {}, Items: {}",
                                        userId, itemsForOrder.size());
                        throw new IllegalArgumentException("El total de la orden no puede ser cero");
                }

                // Generar número de orden único
                String orderNumber = generateOrderNumber();

                // Crear la orden - IMPORTANTE: guardar el SOURCE en description para
                // recuperarlo después
                Order order = Order.builder()
                                .orderNumber(orderNumber)
                                .user(user)
                                .status(OrderStatus.PENDING)
                                .totalAmount(totalAmount)
                                .shippingAddress(shippingAddress)
                                .billingAddress(billingAddress)
                                .paymentMethod(request.getPaymentMethod())
                                .description("[SOURCE:" + itemsSource + "]") // Guardar SOURCE para rastrabilidad
                                .build();

                // Crear los items de la orden desde los items seleccionados
                List<OrderItem> orderItems = itemsForOrder.stream()
                                .map(cartItem -> OrderItem.builder()
                                                .order(order)
                                                .product(cartItem.getProduct())
                                                .quantity(cartItem.getQuantity())
                                                .unitPrice(cartItem.getUnitPrice())
                                                .build())
                                .collect(Collectors.toList());

                // log.info("Created orderItems list with size: {}", orderItems.size());
                // for (int i = 0; i < orderItems.size(); i++) {
                // log.info("OrderItem[{}]: Product='{}', Quantity={}", i, ...);
                // }

                order.setItems(orderItems);
                // log.info("Order items set. Size={}", order.getItems().size());

                Order savedOrder = orderRepository.save(order);
                // log.info("After save - Items saved: {}", savedOrder.getItems().size());
                // for (int i = 0; i < savedOrder.getItems().size(); i++) {
                // log.info("Saved OrderItem[{}]: Product='{}', Quantity={}", i, ...);
                // }

                // NO LIMPIAR EL CARRITO AQUÍ - Se limpiará cuando el pago sea APROBADO
                // El carrito se necesita después para MercadoPago
                // log.info("Cart will be cleaned when payment is approved");

                // No crear la preferencia aquí - el frontend la creará después usando el
                // endpoint /create-preference
                // Esto evita duplicar la llamada a createPaymentPreference

                OrderResponse response = mapToOrderResponse(savedOrder);
                // Retornar null para paymentUrl - el frontend lo obtendrá del siguiente paso
                return response;
        }

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponse> getUserOrders(Long userId) {
                log.debug("Obteniendo órdenes del usuario: {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado: " + userId));

                List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged())
                                .getContent();
                List<OrderResponse> responses = orders.stream()
                                .map(this::mapToOrderResponse)
                                .collect(Collectors.toList());

                log.debug("Órdenes obtenidas - Usuario: {}, Cantidad: {}", userId, responses.size());

                return responses;
        }

        @Transactional(readOnly = true)
        public OrderResponse getOrderById(Long id, Long userId) {
                log.debug("Obteniendo orden por ID: {} para usuario: {}", id, userId);

                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));

                // Verificar que el usuario sea el propietario de la orden
                if (!order.getUser().getId().equals(userId)) {
                        throw new IllegalArgumentException("No tienes permisos para acceder a esta orden");
                }

                log.debug("Orden encontrada - ID: {}, Usuario: {}", id, userId);

                return mapToOrderResponse(order);
        }

        @Override
        @Transactional(readOnly = true)
        public OrderResponse getOrderById(Long id, String userId) {
                log.debug("Obteniendo orden por ID: {} para usuario: {}", id, userId);

                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));

                // Verificar que el usuario sea el propietario de la orden
                if (!order.getUser().getId().toString().equals(userId)) {
                        throw new IllegalArgumentException("No tienes permisos para acceder a esta orden");
                }

                log.debug("Orden encontrada - ID: {}, Usuario: {}", id, userId);

                return mapToOrderResponse(order);
        }

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponse> getAllOrders() {
                log.debug("Obteniendo todas las órdenes (admin)");

                List<Order> orders = orderRepository.findAll();
                List<OrderResponse> responses = orders.stream()
                                .map(this::mapToOrderResponse)
                                .collect(Collectors.toList());

                log.debug("Todas las órdenes obtenidas - Cantidad: {}", responses.size());

                return responses;
        }

        @Override
        public OrderResponse updateOrderStatus(Long id, String status) {
                log.debug("Actualizando estado de orden - ID: {}, Nuevo estado: {}", id, status);

                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));

                try {
                        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
                        order.setStatus(newStatus);
                        order.setUpdatedAt(LocalDateTime.now());

                        Order savedOrder = orderRepository.save(order);

                        // log.info("Estado de orden actualizado - ID: {}, Estado anterior: {}, Estado
                        // nuevo: {}",
                        // id, order.getStatus(), newStatus);

                        return mapToOrderResponse(savedOrder);
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Estado de orden inválido: " + status);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<Order> getOrdersByStatus(OrderStatus status) {
                log.debug("Obteniendo órdenes por estado: {}", status);

                List<Order> orders = orderRepository.findByStatus(status);

                log.debug("Órdenes por estado obtenidas - Estado: {}, Cantidad: {}", status, orders.size());

                return orders;
        }

        @Override
        @Transactional(readOnly = true)
        public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
                log.debug("Obteniendo órdenes por rango de fechas - Inicio: {}, Fin: {}", startDate, endDate);

                List<Order> orders = orderRepository.findByDateRange(startDate, endDate);

                log.debug("Órdenes por rango de fechas obtenidas - Cantidad: {}", orders.size());

                return orders;
        }

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponse> getOrdersByStatusAsResponse(OrderStatus status) {
                log.debug("Obteniendo órdenes por estado como OrderResponse: {}", status);

                List<Order> orders = getOrdersByStatus(status);
                List<OrderResponse> responses = orders.stream()
                                .map(this::mapToOrderResponse)
                                .collect(Collectors.toList());

                log.debug("Órdenes por estado como OrderResponse obtenidas - Estado: {}, Cantidad: {}", status,
                                responses.size());

                return responses;
        }

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponse> getOrdersByDateRangeAsResponse(LocalDateTime startDate, LocalDateTime endDate) {
                log.debug("Obteniendo órdenes por rango de fechas como OrderResponse - Inicio: {}, Fin: {}", startDate,
                                endDate);

                List<Order> orders = getOrdersByDateRange(startDate, endDate);
                List<OrderResponse> responses = orders.stream()
                                .map(this::mapToOrderResponse)
                                .collect(Collectors.toList());

                log.debug("Órdenes por rango de fechas como OrderResponse obtenidas - Cantidad: {}", responses.size());

                return responses;
        }

        @Override
        @Transactional(readOnly = true)
        public OrderStatisticsResponse getOrderStatistics() {
                log.debug("Obteniendo estadísticas de órdenes");

                List<Order> allOrders = orderRepository.findAll();

                // Estadísticas básicas
                Long totalOrders = (long) allOrders.size();
                BigDecimal totalRevenue = allOrders.stream()
                                .map(Order::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Órdenes por estado
                Map<String, Long> ordersByStatus = allOrders.stream()
                                .collect(Collectors.groupingBy(
                                                order -> order.getStatus().name(),
                                                Collectors.counting()));

                // Ingresos por estado
                Map<String, BigDecimal> revenueByStatus = allOrders.stream()
                                .collect(Collectors.groupingBy(
                                                order -> order.getStatus().name(),
                                                Collectors.mapping(Order::getTotalAmount,
                                                                Collectors.reducing(BigDecimal.ZERO,
                                                                                BigDecimal::add))));

                // Estadísticas temporales
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime today = now.toLocalDate().atStartOfDay();
                LocalDateTime weekStart = now.minusDays(7);
                LocalDateTime monthStart = now.minusDays(30);

                Long ordersToday = allOrders.stream()
                                .filter(order -> order.getCreatedAt().isAfter(today))
                                .count();

                Long ordersThisWeek = allOrders.stream()
                                .filter(order -> order.getCreatedAt().isAfter(weekStart))
                                .count();

                Long ordersThisMonth = allOrders.stream()
                                .filter(order -> order.getCreatedAt().isAfter(monthStart))
                                .count();

                BigDecimal revenueToday = allOrders.stream()
                                .filter(order -> order.getCreatedAt().isAfter(today))
                                .map(Order::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal revenueThisWeek = allOrders.stream()
                                .filter(order -> order.getCreatedAt().isAfter(weekStart))
                                .map(Order::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal revenueThisMonth = allOrders.stream()
                                .filter(order -> order.getCreatedAt().isAfter(monthStart))
                                .map(Order::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Valor promedio de orden
                Double averageOrderValue = totalOrders > 0
                                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                                                .doubleValue()
                                : 0.0;

                OrderStatisticsResponse statistics = OrderStatisticsResponse.builder()
                                .totalOrders(totalOrders)
                                .totalRevenue(totalRevenue)
                                .ordersByStatus(ordersByStatus)
                                .revenueByStatus(revenueByStatus)
                                .ordersToday(ordersToday)
                                .ordersThisWeek(ordersThisWeek)
                                .ordersThisMonth(ordersThisMonth)
                                .revenueToday(revenueToday)
                                .revenueThisWeek(revenueThisWeek)
                                .revenueThisMonth(revenueThisMonth)
                                .averageOrderValue(averageOrderValue)
                                .build();

                log.debug("Estadísticas de órdenes obtenidas - Total órdenes: {}, Ingresos totales: {}", totalOrders,
                                totalRevenue);

                return statistics;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<Order> getUserOrdersPaged(Long userId, Pageable pageable) {
                log.debug("Obteniendo órdenes paginadas del usuario - UserID: {}, Página: {}, Tamaño: {}",
                                userId, pageable.getPageNumber(), pageable.getPageSize());

                Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

                log.debug("Órdenes paginadas obtenidas - UserID: {}, Cantidad: {}, Total: {}",
                                userId, orders.getNumberOfElements(), orders.getTotalElements());

                return orders;
        }

        @Override
        @Transactional(readOnly = true)
        public Long countOrdersByStatus(OrderStatus status) {
                return orderRepository.countByStatus(status);
        }

        @Override
        @Transactional(readOnly = true)
        public Double getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
                log.debug("Obteniendo ingresos totales - Inicio: {}, Fin: {}", startDate, endDate);

                Double revenue = orderRepository.getTotalRevenue(startDate, endDate);

                log.debug("Ingresos totales obtenidos: {}", revenue);

                return revenue != null ? revenue : 0.0;
        }

        @Override
        @Transactional(readOnly = true)
        public boolean existsByOrderNumber(String orderNumber) {
                return orderRepository.existsByOrderNumber(orderNumber);
        }

        @Override
        @Transactional(readOnly = true)
        public Order getOrderEntityById(Long id) {
                return orderRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));
        }

        private OrderResponse mapToOrderResponse(Order order) {
                List<OrderItemResponse> items = order.getItems().stream()
                                .map(this::mapToOrderItemResponse)
                                .collect(Collectors.toList());

                // Calcular el total desde los items como fallback si el totalAmount guardado es
                // null o cero
                BigDecimal calculatedTotal = items.stream()
                                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalAmount = order.getTotalAmount();
                if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                        totalAmount = calculatedTotal;
                        // Opcionalmente actualizar la orden en la base de datos
                        if (order.getId() != null) {
                                order.setTotalAmount(totalAmount);
                                orderRepository.save(order);
                        }
                }

                Profile profile = profileRepository.findByUserId(order.getUser().getId()).orElse(null);
                UserResponse user = UserResponse.builder()
                                .id(order.getUser().getId())
                                .email(order.getUser().getEmail())
                                .firstName(profile != null ? profile.getFirstName() : "")
                                .lastName(profile != null ? profile.getLastName() : "")
                                .role(order.getUser().getRole())
                                .build();

                // Mapear direcciones de envío y facturación
                AddressResponse shippingAddress = order.getShippingAddress() != null
                                ? mapToAddressResponse(order.getShippingAddress())
                                : null;
                AddressResponse billingAddress = order.getBillingAddress() != null
                                ? mapToAddressResponse(order.getBillingAddress())
                                : null;

                return OrderResponse.builder()
                                .id(order.getId())
                                .status(order.getStatus().name())
                                .totalAmount(totalAmount)
                                .createdAt(order.getCreatedAt())
                                .updatedAt(order.getUpdatedAt())
                                .items(items)
                                .user(user)
                                .shippingAddress(shippingAddress)
                                .billingAddress(billingAddress)
                                .description(order.getDescription())
                                .build();
        }

        private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
                String imageUrl = orderItem.getProduct().getImages() != null
                                && !orderItem.getProduct().getImages().isEmpty()
                                                ? orderItem.getProduct().getImages().get(0).getImageUrl()
                                                : null;

                ProductResponse product = ProductResponse.builder()
                                .id(orderItem.getProduct().getId())
                                .name(orderItem.getProduct().getName())
                                .description(orderItem.getProduct().getDescription())
                                .price(orderItem.getProduct().getPrice())
                                .stockQuantity(orderItem.getProduct().getStockQuantity())
                                .sku(orderItem.getProduct().getSku())
                                .imageUrl(imageUrl)
                                .build();

                return OrderItemResponse.builder()
                                .id(orderItem.getId())
                                .product(product)
                                .quantity(orderItem.getQuantity())
                                .price(orderItem.getUnitPrice())
                                .subtotal(orderItem.getUnitPrice()
                                                .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                                .build();
        }

        @Override
        public void deleteOrder(Long id) {
                log.debug("Cancelando orden (soft delete) - ID: {}", id);

                Order order = getOrderEntityById(id);

                order.setStatus(OrderStatus.CANCELLED);
                order.setDeletedAt(LocalDateTime.now());
                order.setDeletedBy(getCurrentUserEmail());
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                // log.info("Orden cancelada exitosamente - ID: {}, Número: '{}', Cancelada por:
                // {}",
                // order.getId(), order.getOrderNumber(), order.getDeletedBy());
        }

        @Override
        public void hardDeleteOrder(Long id) {
                log.warn("Eliminando permanentemente orden - ID: {}", id);

                Order order = getOrderEntityById(id);

                orderRepository.delete(order);

                log.warn("Orden eliminada permanentemente - ID: {}, Número: '{}', Eliminada por: {}",
                                order.getId(), order.getOrderNumber(), getCurrentUserEmail());
        }

        @Override
        public void hardDeleteUserOrders(Long userId, String adminIdentifier) {
                log.warn("Eliminando permanentemente todas las órdenes del usuario - UserID: {}, Admin: {}", userId,
                                adminIdentifier);

                // Obtener todas las órdenes del usuario
                List<Order> userOrders = orderRepository
                                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                                .getContent();

                if (userOrders.isEmpty()) {
                        // log.info("No se encontraron órdenes para el usuario - UserID: {}", userId);
                        return;
                }

                // Eliminar cada orden con auditoría
                for (Order order : userOrders) {
                        // Actualizar campos de auditoría
                        order.setDeletedAt(LocalDateTime.now());
                        order.setDeletedBy(adminIdentifier);
                        orderRepository.save(order);

                        // Eliminar permanentemente
                        orderRepository.delete(order);

                        log.warn("Orden eliminada permanentemente - OrderID: {}, OrderNumber: {}, UserID: {}",
                                        order.getId(), order.getOrderNumber(), userId);
                }

                log.warn("Todas las órdenes del usuario eliminadas - UserID: {}, Total: {}", userId, userOrders.size());
        }

        public void restoreOrder(Long id) {
                // log.info("Restaurando orden - ID: {}", id);

                Order order = getOrderEntityById(id);

                if (order.getStatus() != OrderStatus.CANCELLED) {
                        throw new IllegalStateException("Solo se pueden restaurar órdenes canceladas");
                }

                order.setStatus(OrderStatus.PENDING);
                order.setDeletedAt(null);
                order.setDeletedBy(null);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                // log.info("Orden restaurada exitosamente - ID: {}, Número: '{}', Restaurada
                // por: {}",
                // order.getId(), order.getOrderNumber(), getCurrentUserEmail());
        }

        /**
         * Obtiene el email del usuario actualmente autenticado
         */
        private String getCurrentUserEmail() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()
                                && !"anonymousUser".equals(authentication.getPrincipal())) {
                        return authentication.getName();
                }
                return "system"; // Usuario por defecto si no hay autenticación
        }

        /**
         * Genera un número de orden único en formato ORD-000001, ORD-000002, etc.
         */
        private String generateOrderNumber() {
                // Obtener el último número de orden
                List<Order> lastOrders = orderRepository.findTopByOrderByIdDesc(Pageable.ofSize(1));
                String lastOrderNumber = lastOrders.isEmpty() ? "ORD-000000" : lastOrders.get(0).getOrderNumber();

                // Extraer el número secuencial
                String numericPart = lastOrderNumber.replace("ORD-", "");
                int nextNumber = Integer.parseInt(numericPart) + 1;

                // Formatear con ceros a la izquierda
                return String.format("ORD-%06d", nextNumber);
        }

        @Override
        public OrderResponse updateOrderDescription(Long orderId, String description, Long userId) {
                // log.info("📝 OrderService: Actualizando descripción de orden {} para usuario
                // {}", orderId, userId);

                // Buscar la orden
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Orden no encontrada con ID: " + orderId));

                // Verificar que la orden pertenece al usuario (o es admin)
                if (!order.getUser().getId().equals(userId)) {
                        // Aquí podríamos agregar lógica para verificar si es admin
                        log.warn("⚠️ Usuario {} intentó actualizar orden {} que no le pertenece", userId, orderId);
                        throw new ResourceNotFoundException("Orden no encontrada");
                }

                // Actualizar la descripción
                order.setDescription(description);
                order.setUpdatedAt(LocalDateTime.now());

                Order savedOrder = orderRepository.save(order);
                // log.info("✅ Descripción de orden {} actualizada exitosamente", orderId);

                // Convertir a response
                return mapToOrderResponse(savedOrder);
        }

        private AddressResponse mapToAddressResponse(Address address) {
                return AddressResponse.builder()
                                .id(address.getId())
                                .type(address.getType())
                                .firstName(address.getFirstName())
                                .lastName(address.getLastName())
                                .fullName(address.getFullName())
                                .address(address.getAddress())
                                .address2(address.getAddress2())
                                .city(address.getCity())
                                .zipCode(address.getZipCode())
                                .country(address.getCountry())
                                .fullAddress(address.getFullAddress())
                                .phone(address.getPhone())
                                .instructions(address.getInstructions())
                                .isDefault(address.getIsDefault())
                                .createdAt(address.getCreatedAt())
                                .updatedAt(address.getUpdatedAt())
                                .build();
        }
}