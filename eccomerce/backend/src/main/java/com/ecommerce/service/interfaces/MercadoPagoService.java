package com.ecommerce.service.interfaces;

import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface MercadoPagoService {

    /**
     * Crea una preferencia de pago completa para una orden
     * 
     * @param order La orden para la que crear la preferencia
     * @return PaymentResponse con los datos de la preferencia
     */
    PaymentResponse createPaymentPreference(Order order);

    /**
     * Crea una preferencia de pago simple con datos básicos
     * Similar a la implementación de Node.js
     * 
     * @param preferenceData Datos para la preferencia
     * @return ID de la preferencia creada
     */
    String createSimplePreference(Map<String, Object> preferenceData);

    /**
     * Procesa un webhook de MercadoPago
     * 
     * @param webhookData Datos del webhook
     * @param signature   Firma del webhook para validación
     */
    void processWebhook(String webhookData, String signature);

    PaymentResponse getPaymentStatus(String paymentId);

    void cancelPayment(String paymentId);

    void refundPayment(String paymentId);

    /**
     * Actualiza el estado de una transacción de MercadoPago y la orden asociada
     *
     * @param preferenceId     ID de preferencia de MercadoPago
     * @param paymentId        ID de pago de MercadoPago
     * @param paymentStatus    Estado del pago
     * @param paymentType      Tipo de pago (tarjeta, efectivo, etc)
     * @param orderStatus      Nuevo estado para la orden
     * @param collectionId     ID de colección (collection_id)
     * @param collectionStatus Estado de la colección
     * @param processingMode   Modo de procesamiento
     * @param siteId           ID del sitio
     * @param amount           Monto de la transacción
     * @param dateCreated      Fecha de creación
     * @param dateApproved     Fecha de aprobación
     */
    void updateTransactionStatus(String preferenceId, String paymentId, String paymentStatus, String paymentType,
            OrderStatus orderStatus, String collectionId, String collectionStatus, String processingMode, String siteId,
            BigDecimal amount, LocalDateTime dateCreated, LocalDateTime dateApproved);

    /**
     * Obtiene la información de una preferencia de MercadoPago
     *
     * @param preferenceId ID de la preferencia
     * @return Map con la información de la preferencia
     */
    Map<String, Object> getPreferenceInfo(String preferenceId);

    /**
     * Verifica y actualiza el estado de pagos pendientes
     * Útil para sincronizar estados cuando los webhooks fallan
     */
    void checkPendingPayments();
}
