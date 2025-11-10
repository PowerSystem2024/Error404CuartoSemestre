package com.ecommerce.repository;

import com.ecommerce.model.entity.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    List<EmailNotification> findBySentFalseAndScheduledAtBefore(LocalDateTime dateTime);

    List<EmailNotification> findByUserIdAndSentFalse(Long userId);

    @Query("SELECT n FROM EmailNotification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<EmailNotification> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
}