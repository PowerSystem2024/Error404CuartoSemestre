package com.ecommerce.repository;

import com.ecommerce.model.entity.MercadoPagoWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MercadoPagoWebhookLogRepository extends JpaRepository<MercadoPagoWebhookLog, Long> {

    List<MercadoPagoWebhookLog> findByProcessedFalse();

    boolean existsByWebhookId(String webhookId);
}
