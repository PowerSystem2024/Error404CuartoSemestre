package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderStatisticsResponse;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    /**
     * Crea una nueva orden desde el carrito de compras
     */
    OrderResponse createOrder(OrderRequest request, Long userId);

    /**
     * Obtiene todas las órdenes de un usuario
     */
    List<OrderResponse> getUserOrders(Long userId);

    /**
     * Obtiene una orden específica por ID (solo del usuario)
     */
    OrderResponse getOrderById(Long id, String userId);

    /**
     * Obtiene todas las órdenes (solo admin)
     */
    List<OrderResponse> getAllOrders();

    /**
     * Actualiza el estado de una orden
     */
    OrderResponse updateOrderStatus(Long id, String status);

    /**
     * Obtiene órdenes por estado
     */
    List<Order> getOrdersByStatus(OrderStatus status);

    /**
     * Obtiene órdenes por rango de fechas
     */
    List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Obtiene órdenes de un usuario con paginación
     */
    Page<Order> getUserOrdersPaged(Long userId, Pageable pageable);

    /**
     * Cuenta órdenes por estado
     */
    Long countOrdersByStatus(OrderStatus status);

    /**
     * Obtiene ingresos totales en un período
     */
    Double getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Verifica si existe una orden por número
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Obtiene órdenes por estado (como OrderResponse)
     */
    List<OrderResponse> getOrdersByStatusAsResponse(OrderStatus status);

    /**
     * Obtiene órdenes por rango de fechas (como OrderResponse)
     */
    List<OrderResponse> getOrdersByDateRangeAsResponse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Obtiene estadísticas de órdenes
     */
    OrderStatisticsResponse getOrderStatistics();

    /**
     * Obtiene una orden por ID (solo entidad)
     */
    Order getOrderEntityById(Long id);

    /**
     * Elimina una orden (soft delete - cambia status a CANCELLED)
     */
    void deleteOrder(Long id);

    /**
     * Elimina permanentemente una orden (hard delete)
     */
    void hardDeleteOrder(Long id);

    /**
     * Elimina permanentemente todas las órdenes de un usuario (para eliminación en
     * cascada)
     */
    void hardDeleteUserOrders(Long userId, String adminIdentifier);

    /**
     * Actualiza la descripción de una orden
     */
    OrderResponse updateOrderDescription(Long orderId, String description, Long userId);

    void restoreOrder(Long id);
}