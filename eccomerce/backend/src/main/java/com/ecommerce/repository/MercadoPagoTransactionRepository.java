package com.ecommerce.repository;

import com.ecommerce.model.entity.MercadoPagoTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MercadoPagoTransactionRepository extends JpaRepository<MercadoPagoTransaction, Long> {

    Optional<MercadoPagoTransaction> findByPaymentId(String paymentId);

    Optional<MercadoPagoTransaction> findByOrderId(Long orderId);

    List<MercadoPagoTransaction> findByPreferenceIdContaining(String preferenceId);

    List<MercadoPagoTransaction> findByExternalReference(String externalReference);

    List<MercadoPagoTransaction> findByStatusIn(List<String> statuses);
}
