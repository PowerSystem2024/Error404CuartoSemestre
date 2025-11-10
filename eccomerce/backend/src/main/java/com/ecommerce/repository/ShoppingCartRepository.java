package com.ecommerce.repository;

import com.ecommerce.model.entity.ShoppingCart;
import com.ecommerce.model.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends BaseRepository<ShoppingCart, Long> {

    @Query("SELECT DISTINCT c FROM ShoppingCart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product p WHERE c.user = :user AND c.deletedAt IS NULL AND c.active = true ORDER BY c.createdAt DESC")
    java.util.List<ShoppingCart> findByUserWithItems(User user);

    @Query("SELECT c FROM ShoppingCart c WHERE c.sessionId = :sessionId AND c.deletedAt IS NULL AND c.active = true")
    Optional<ShoppingCart> findBySessionId(String sessionId);

    @Query("SELECT c FROM ShoppingCart c WHERE c.expiresAt < :now AND c.deletedAt IS NULL AND c.active = true")
    java.util.List<ShoppingCart> findExpiredCarts(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ShoppingCart c SET c.deletedAt = CURRENT_TIMESTAMP, c.active = false WHERE c.expiresAt < :now AND c.deletedAt IS NULL")
    void deleteExpiredCarts(@Param("now") LocalDateTime now);

    Optional<ShoppingCart> findByUser(User user);
}
