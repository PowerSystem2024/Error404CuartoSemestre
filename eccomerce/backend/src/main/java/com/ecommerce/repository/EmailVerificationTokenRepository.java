package com.ecommerce.repository;

import com.ecommerce.model.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserIdAndUsedFalse(Long userId);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void invalidateUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.expiryDate < :now AND t.used = false")
    java.util.List<EmailVerificationToken> findExpiredTokens(@Param("now") LocalDateTime now);
}