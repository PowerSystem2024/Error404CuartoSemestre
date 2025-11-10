package com.ecommerce.repository;

import com.ecommerce.model.entity.Order;
import com.ecommerce.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends BaseRepository<Order, Long> {

        @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.deletedAt IS NULL AND o.active = true ORDER BY o.createdAt DESC")
        Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deletedAt IS NULL AND o.active = true")
        List<Order> findByStatus(OrderStatus status);

        @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL AND o.active = true")
        List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.deletedAt IS NULL AND o.active = true")
        Long countByStatus(@Param("status") OrderStatus status);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL AND o.active = true")
        Double getTotalRevenue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.orderNumber = :orderNumber AND o.deletedAt IS NULL AND o.active = true")
        boolean existsByOrderNumber(String orderNumber);

        @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND o.active = true ORDER BY o.id DESC")
        List<Order> findTopByOrderByIdDesc(Pageable pageable);
}
