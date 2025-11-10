package com.ecommerce.service.impl;

import com.ecommerce.config.MercadoPagoConfiguration;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.model.entity.MercadoPagoTransaction;
import com.ecommerce.model.entity.MercadoPagoWebhookLog;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.OrderItem;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.repository.MercadoPagoTransactionRepository;
import com.ecommerce.repository.MercadoPagoWebhookLogRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.MercadoPagoService;
import com.ecommerce.service.interfaces.ShoppingCartService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferenceRequest.PreferenceRequestBuilder;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private final MercadoPagoConfiguration mercadoPagoConfig;
    private final MercadoPagoTransactionRepository transactionRepository;
    private final MercadoPagoWebhookLogRepository webhookLogRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final ShoppingCartService shoppingCartService;

    @Override
    @Transactional
    public PaymentResponse createPaymentPreference(Order order) {
        try {
            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());

            // 🔥 IMPORTANTE: Obtener items del CARRITO del backend, NO de la orden
            Long userId = order.getUser().getId();
            com.ecommerce.model.entity.ShoppingCart cart = shoppingCartService.getOrCreateCart(userId);

            log.info("🛒 Leyendo carrito del backend para preferencia de pago");
            log.info("   UserId: {}, Items en carrito: {}", userId,
                    cart.getItems() != null ? cart.getItems().size() : 0);

            // Crear items de la preferencia desde el CARRITO
            List<PreferenceItemRequest> items = new ArrayList<>();

            int itemsSize = cart.getItems() != null ? cart.getItems().size() : 0;

            // log.info("Items in cart for Mercado Pago:");
            if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                // for (int i = 0; i < cart.getItems().size(); i++) {
                // com.ecommerce.model.entity.CartItem cartItem = cart.getItems().get(i);
                // log.info("Item [{}]: ProductId={}, Name={}, Qty={}, Price={}",
                // i, cartItem.getProduct().getId(), cartItem.getProduct().getName(),
                // cartItem.getQuantity(), cartItem.getUnitPrice());
                // }
            }

            if (itemsSize == 1) {
                // Un solo producto: mostrar nombre del producto
                com.ecommerce.model.entity.CartItem singleItem = cart.getItems().get(0);
                String itemTitle = String.format("%s - %s", order.getOrderNumber(), singleItem.getProduct().getName());
                // log.info("Creating preference with 1 item: {}", itemTitle);
                items.add(PreferenceItemRequest.builder()
                        .title(itemTitle)
                        .quantity(singleItem.getQuantity())
                        .unitPrice(singleItem.getUnitPrice())
                        .currencyId("ARS")
                        .build());
            } else if (itemsSize > 1) {
                // Múltiples productos: crear un solo item con la cantidad total
                String itemTitle = String.format("%s (%d productos)", order.getOrderNumber(), itemsSize);
                // log.info("Creating preference with {} items", itemsSize);
                items.add(PreferenceItemRequest.builder()
                        .title(itemTitle)
                        .quantity(1)
                        .unitPrice(order.getTotalAmount())
                        .currencyId("ARS")
                        .build());
            } else {
                // log.error("Empty cart - no items for preference");
                throw new RuntimeException("El carrito está vacío");
            }

            // CONFIGURACIÓN PARA EL DESAFÍO DE MERCADOPAGO
            String externalReference = "lucas_ortega54@hotmail.com";

            // Configurar URLs de retorno y notificacion
            String successUrl = mercadoPagoConfig.getSuccessUrl();
            String failureUrl = mercadoPagoConfig.getFailureUrl();
            String pendingUrl = mercadoPagoConfig.getPendingUrl();
            String notificationUrl = mercadoPagoConfig.getWebhookUrl();

            // log.info("URLs configuradas - Success: {}, Failure: {}, Pending: {},
            // Notification: {}",
            // successUrl, failureUrl, pendingUrl, notificationUrl);

            // Crear la preferencia
            PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(items)
                    .externalReference(externalReference)
                    .autoReturn("approved")
                    .notificationUrl(notificationUrl);

            // Configurar back_urls
            com.mercadopago.client.preference.PreferenceBackUrlsRequest backUrls = com.mercadopago.client.preference.PreferenceBackUrlsRequest
                    .builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();
            builder.backUrls(backUrls);

            PreferenceRequest preferenceRequest = builder.build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // Guardar o actualizar transaccion en base de datos
            // Verificar si ya existe una transacción para esta orden
            Optional<MercadoPagoTransaction> existingTransaction = transactionRepository.findByOrderId(order.getId());

            MercadoPagoTransaction transaction;
            if (existingTransaction.isPresent()) {
                // Actualizar la transacción existente
                transaction = existingTransaction.get();
                transaction.setPreferenceId(preference.getId());
                transaction.setStatus("pending");
                // Mantener el createdAt original, actualizar updatedAt
            } else {
                // Crear nueva transacción
                transaction = MercadoPagoTransaction.builder()
                        .order(order)
                        .status("pending")
                        .amount(order.getTotalAmount())
                        .preferenceId(preference.getId())
                        .externalReference(externalReference)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            transactionRepository.save(transaction);

            // Construir respuesta
            PaymentResponse response = PaymentResponse.builder()
                    .preferenceId(preference.getId())
                    .initPoint(preference.getInitPoint())
                    // .sandboxInitPoint(preference.getSandboxInitPoint())
                    .build();

            return response;

        } catch (MPException | MPApiException e) {
            log.error("Error creando preferencia de pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating payment preference", e);
        }
    }

    @Override
    public String createSimplePreference(Map<String, Object> preferenceData) {
        try {
            log.info("🔄 Creando preferencia simple con datos: {}", preferenceData);

            // Log detallado de cada campo recibido
            log.info("📋 Detalles de preferenceData:");
            preferenceData.forEach((key, value) -> log.info(" {}: {}", key, value));

            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());

            // Verificar si viene un array de items o datos simples
            List<Map<String, Object>> items = null;
            Number price = null;
            Number quantity = null;
            String description = null;

            if (preferenceData.containsKey("items")) {
                // Formato antiguo: array de items
                ObjectMapper mapper = new ObjectMapper();
                items = mapper.convertValue(
                        preferenceData.get("items"),
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                if (items != null && !items.isEmpty()) {
                    Map<String, Object> firstItem = items.get(0);
                    price = (Number) firstItem.get("unit_price");
                    quantity = (Number) firstItem.get("quantity");
                    description = (String) firstItem.get("title");
                }
            } else {
                // Formato nuevo: datos simples
                price = (Number) preferenceData.get("price");
                quantity = (Number) preferenceData.get("quantity");
                description = (String) preferenceData.get("description");
            }

            // Validar que tengamos los datos necesarios
            if (price == null || price.doubleValue() <= 0) {
                log.error("❌ Error: Precio inválido o cero: {}", price);
                throw new RuntimeException("Precio inválido para la preferencia de pago");
            }

            if (description == null || description.isEmpty()) {
                description = "Producto";
            }

            if (quantity == null || quantity.intValue() <= 0) {
                quantity = 1;
            }

            log.info("📊 Datos procesados - Price: {}, Quantity: {}, Description: '{}'",
                    price, quantity, description);

            // Extraer otros datos comunes
            String autoReturn = (String) preferenceData.get("auto_return");

            Map<String, String> backUrls = null;
            Object backUrlsObj = preferenceData.get("back_urls");
            if (backUrlsObj != null) {
                ObjectMapper mapper = new ObjectMapper();
                backUrls = mapper.convertValue(
                        backUrlsObj,
                        mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            }
            // CONFIGURACIÓN ORIGINAL (comentada para el desafío)
            // String externalReference = (String) preferenceData.get("external_reference");

            // CONFIGURACIÓN PARA EL DESAFÍO DE MERCADOPAGO
            String externalReference = "lucas_ortega54@hotmail.com";

            log.info("🔍 External reference configurado para desafío: '{}' (tipo: {})",
                    externalReference,
                    externalReference != null ? externalReference.getClass().getSimpleName() : "null");

            // Validar datos básicos
            if (price == null || price.doubleValue() <= 0) {
                log.error("❌ Error: Precio inválido o cero: {}", price);
                throw new RuntimeException("Precio inválido para la preferencia de pago");
            }

            // Crear item
            String finalTitle = description != null && !description.trim().isEmpty() ? description.trim() : "Producto";

            // Limitar la longitud del título para evitar problemas con MercadoPago
            if (finalTitle.length() > 128) {
                finalTitle = finalTitle.substring(0, 125) + "...";
            }

            log.info("📦 Título que se enviará a MercadoPago: '{}' (longitud: {})", finalTitle, finalTitle.length());

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(finalTitle)
                    .quantity(quantity != null ? quantity.intValue() : 1)
                    .unitPrice(BigDecimal.valueOf(price.doubleValue()))
                    .currencyId("ARS")
                    .build();

            log.info("📦 Item creado: título='{}', cantidad={}, precio_unitario={}",
                    item.getTitle(), item.getQuantity(), item.getUnitPrice());

            // Crear builder de preferencia
            PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(List.of(item));

            // Configurar external_reference si está presente
            if (externalReference != null && !externalReference.isEmpty()) {
                builder.externalReference(externalReference);
                log.info("🔗 External reference configurado: {}", externalReference);
            }

            // Configurar auto_return si está presente
            if (autoReturn != null && !autoReturn.isEmpty()) {
                builder.autoReturn(autoReturn);
            }

            // Configurar back_urls si están presentes
            if (backUrls != null && !backUrls.isEmpty()) {
                // Usar URLs directamente (siempre HTTPS)
                String successUrl = backUrls.get("success");
                String failureUrl = backUrls.get("failure");
                String pendingUrl = backUrls.get("pending");

                com.mercadopago.client.preference.PreferenceBackUrlsRequest backUrlsRequest = com.mercadopago.client.preference.PreferenceBackUrlsRequest
                        .builder()
                        .success(successUrl)
                        .failure(failureUrl)
                        .pending(pendingUrl)
                        .build();
                builder.backUrls(backUrlsRequest);
            }

            // Configurar statement_descriptor para mejor visualización
            String statementDesc = finalTitle.replace("Orden", "Compra").length() > 20
                    ? finalTitle.replace("Orden", "Compra").substring(0, 20)
                    : finalTitle.replace("Orden", "Compra");
            builder.statementDescriptor(statementDesc);
            log.info("🏷️ Statement descriptor configurado: '{}'", statementDesc);

            PreferenceRequest preferenceRequest = builder.build();

            // Log detallado del request antes de enviarlo
            log.info("🔧 Request que se enviará a MercadoPago:");
            log.info("  - Items: {}", preferenceRequest.getItems().stream()
                    .map(prefItem -> String.format("'%s' (x%d)", prefItem.getTitle(), prefItem.getQuantity()))
                    .toList());
            log.info("  - External Reference: {}", preferenceRequest.getExternalReference());
            log.info("  - Back URLs: {}",
                    preferenceRequest.getBackUrls() != null ? String.format("success=%s, failure=%s, pending=%s",
                            preferenceRequest.getBackUrls().getSuccess(),
                            preferenceRequest.getBackUrls().getFailure(),
                            preferenceRequest.getBackUrls().getPending()) : "null");

            PreferenceClient client = new PreferenceClient();
            // log.info("📡 Enviando request a MercadoPago...");

            Preference preference = client.create(preferenceRequest);

            // Guardar transacción en base de datos si tenemos external_reference
            if (externalReference != null && !externalReference.isEmpty()) {
                // log.info("🔄 Intentando guardar transacción para externalReference: {}",
                // externalReference);

                // Verificar si es una orden y revisar su status
                if (externalReference.startsWith("order_")) {
                    try {
                        String orderIdStr = externalReference.substring("order_".length());
                        Long orderId = Long.parseLong(orderIdStr);

                        // Buscar la orden para verificar su status
                        Order order = orderRepository.findById(orderId).orElse(null);
                        if (order != null) {
                            log.debug("📋 Orden {} encontrada con status: {}", orderId, order.getStatus());

                            // Si la orden ya está confirmada/aprobada, NO reutilizar transacción existente
                            if (OrderStatus.CONFIRMED.equals(order.getStatus())) {
                                log.info("✅ Orden {} ya está confirmada. Creando nueva transacción.", orderId);
                                // Continuar con la creación de nueva transacción (no devolver existente)
                            } else {
                                log.info(
                                        "⏳ Orden {} no está aprobada (status: {}). Verificando transacciones existentes.",
                                        orderId, order.getStatus());

                                // Verificar si ya existe una transacción para esta orden
                                Optional<MercadoPagoTransaction> existingOrderTransaction = transactionRepository
                                        .findByOrderId(orderId);
                                if (existingOrderTransaction.isPresent()) {
                                    log.warn(
                                            "⚠️ Ya existe una transacción para la orden {} (no aprobada): {}. Devolviendo preferenceId existente: {}",
                                            orderId, existingOrderTransaction.get().getId(),
                                            existingOrderTransaction.get().getPreferenceId());
                                    return existingOrderTransaction.get().getPreferenceId();
                                }
                            }
                        } else {
                            log.warn("⚠️ No se encontró la orden con ID {} para verificar status", orderId);
                        }
                    } catch (NumberFormatException e) {
                        log.debug("No se pudo extraer orderId de externalReference: {}", externalReference);
                    }
                }

                // Verificar por externalReference solo si no es una orden (o si la orden no
                // existe)
                List<MercadoPagoTransaction> existingTransactions = transactionRepository
                        .findByExternalReference(externalReference);
                if (!existingTransactions.isEmpty()) {
                    log.warn(
                            "⚠️ Ya existe una transacción para externalReference: {}. Devolviendo preferenceId existente: {}",
                            externalReference, existingTransactions.get(0).getPreferenceId());
                    // Devolver el preferenceId existente
                    return existingTransactions.get(0).getPreferenceId();
                }

                try {
                    // Extraer orderId del external_reference (formato: "order_123")
                    Long orderId = null;
                    if (externalReference.startsWith("order_")) {
                        String orderIdStr = externalReference.substring("order_".length());
                        orderId = Long.parseLong(orderIdStr);
                        // log.info("🔍 OrderId extraído del external_reference: {}", orderId);
                    }

                    if (orderId != null) {
                        // log.info("🔍 Buscando orden con ID: {}", orderId);
                        // Buscar la orden
                        Order order = orderRepository.findById(orderId).orElse(null);
                        if (order != null) {
                            // log.info("✅ Orden encontrada: {} - Estado actual: {}", order.getId(),
                            // order.getStatus());
                            // Calcular el total
                            BigDecimal totalAmount = BigDecimal.valueOf(price.doubleValue());
                            if (quantity != null) {
                                totalAmount = totalAmount.multiply(BigDecimal.valueOf(quantity.intValue()));
                            }

                            // log.info("💰 Total calculado: {}", totalAmount);

                            // Guardar transacción
                            MercadoPagoTransaction mercadoPagoTransaction = MercadoPagoTransaction.builder()
                                    .order(order)
                                    .status("pending")
                                    .amount(totalAmount)
                                    .preferenceId(preference.getId())
                                    .externalReference(externalReference)
                                    .createdAt(LocalDateTime.now())
                                    .build();

                            // MercadoPagoTransaction savedTransaction = transactionRepository
                            // .save(mercadoPagoTransaction);
                            transactionRepository.save(mercadoPagoTransaction);
                            // log.info("✅ Transacción guardada en BD para orden {}: ID={}", orderId,
                            // savedTransaction.getId());
                        } else {
                            log.warn("⚠️ No se encontró la orden con ID {} para guardar la transacción", orderId);
                        }
                    } else {
                        log.warn("⚠️ No se pudo extraer orderId del externalReference: {}", externalReference);
                    }
                } catch (Exception e) {
                    log.error("❌ Error al guardar la transacción en BD: {}", e.getMessage(), e);
                }
            } else {
                log.warn("⚠️ ExternalReference es null o vacío, no se guardará transacción en BD");
            }

            return preference.getId();

        } catch (MPException | MPApiException e) {
            log.error("❌ Error de MercadoPago API: {}", e.getMessage(), e);
            if (e instanceof MPApiException) {
                MPApiException mpEx = (MPApiException) e;
                log.error("❌ Detalles de la API: Status={}, Content={}",
                        mpEx.getApiResponse().getStatusCode(),
                        mpEx.getApiResponse().getContent());
            }
            throw new RuntimeException("Error creating simple preference", e);
        } catch (Exception e) {
            log.error("❌ Error general al crear preferencia simple: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating simple preference", e);
        }
    }

    @Override
    @Transactional
    public void processWebhook(String webhookData, String signature) {
        try {
            // log.info("Procesando webhook de MercadoPago: {}", webhookData);

            // Parsear el JSON del webhook
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode webhookJson = objectMapper.readTree(webhookData);

            // Extraer informacion del webhook
            String action = webhookJson.path("action").asText();
            String type = webhookJson.path("type").asText();
            JsonNode data = webhookJson.path("data");

            // log.info("Webhook - Action: {}, Type: {}", action, type);

            // Solo procesar eventos de pago
            if ("payment".equals(type) && "updated".equals(action)) {
                String paymentId = data.path("id").asText();
                // log.info("Procesando pago ID: {}", paymentId);

                // Obtener detalles del pago desde MercadoPago
                com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());
                PaymentClient paymentClient = new PaymentClient();
                Payment payment = paymentClient.get(Long.valueOf(paymentId));

                if (payment != null) {
                    String status = payment.getStatus();
                    String description = payment.getDescription();
                    String externalReference = payment.getExternalReference();

                    // log.info("Estado del pago: {}, Descripcion: {}, External Reference: {}",
                    // status, description, externalReference);

                    // Intentar extraer orderId de la descripcion o external reference
                    Long orderId = extractOrderId(description, externalReference);

                    if (orderId != null) {
                        // log.info("Orden encontrada en pago: {}", orderId);

                        // Buscar la orden en la base de datos
                        Order order = orderRepository.findById(orderId).orElse(null);

                        if (order != null) {
                            // Actualizar estado de la orden basado en el estado del pago
                            OrderStatus newStatus = mapPaymentStatusToOrderStatus(status);
                            if (newStatus != null && !newStatus.equals(order.getStatus())) {
                                order.setStatus(newStatus);
                                order.setUpdatedAt(LocalDateTime.now());

                                // Actualizar descripcion con informacion del pago
                                String updatedDescription = buildOrderDescription(description, paymentId, status);
                                order.setDescription(updatedDescription);

                                orderRepository.save(order);

                                // Enviar email de confirmación si el pago fue aprobado
                                if ("approved".equalsIgnoreCase(status)) {
                                    try {
                                        emailService.sendPaymentConfirmationEmail(order);
                                        log.info("Email de confirmación enviado para orden: {}",
                                                order.getOrderNumber());
                                    } catch (Exception e) {
                                        log.error("Error enviando email de confirmación para orden {}: {}",
                                                order.getOrderNumber(), e.getMessage());
                                        // No fallar el webhook por error en email
                                    }
                                }

                                // log.info("Orden {} actualizada - Nuevo estado: {}, Descripcion actualizada",
                                // orderId, newStatus);
                            } else {
                                // log.info("Orden {} ya tiene el estado correcto: {}", orderId,
                                // order.getStatus());
                            }
                        } else {
                            log.warn("Orden {} no encontrada en base de datos", orderId);
                        }
                    } else {
                        log.warn("No se pudo extraer orderId del pago {}", paymentId);
                    }
                }
            }

            // Guardar log del webhook
            MercadoPagoWebhookLog logEntry = MercadoPagoWebhookLog.builder()
                    .data(webhookData)
                    .signature(signature)
                    .processedAt(LocalDateTime.now())
                    .build();

            webhookLogRepository.save(logEntry);
            // log.info("Webhook procesado exitosamente");

        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing webhook", e);
        }
    }

    /**
     * Extrae el orderId de la descripcion o external reference
     */
    private Long extractOrderId(String description, String externalReference) {
        // Primero intentar desde external reference
        if (externalReference != null && !externalReference.trim().isEmpty()) {
            try {
                return Long.valueOf(externalReference.trim());
            } catch (NumberFormatException e) {
                log.debug("External reference no es numerico: {}", externalReference);
            }
        }

        // Si no, intentar extraer de la descripcion usando regex
        if (description != null && !description.trim().isEmpty()) {
            // Buscar patrones como "Orden #123", "Order #123", "#123", etc.
            Pattern pattern = Pattern.compile("(?:Orden|Order)\\s*#?(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                try {
                    return Long.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.debug("No se pudo parsear orderId de descripcion: {}", description);
                }
            }

            // Tambien buscar solo numeros al final
            Pattern numberPattern = Pattern.compile("(\\d+)$");
            Matcher numberMatcher = numberPattern.matcher(description.trim());
            if (numberMatcher.find()) {
                try {
                    return Long.valueOf(numberMatcher.group(1));
                } catch (NumberFormatException e) {
                    log.debug("No se pudo parsear numero de descripcion: {}", description);
                }
            }
        }

        return null;
    }

    /**
     * Mapea el estado del pago de MercadoPago al estado de la orden
     */
    private OrderStatus mapPaymentStatusToOrderStatus(String paymentStatus) {
        switch (paymentStatus.toLowerCase()) {
            case "approved":
                return OrderStatus.CONFIRMED;
            case "pending":
                return OrderStatus.PENDING;
            case "rejected":
            case "cancelled":
                return OrderStatus.CANCELLED;
            case "refunded":
                return OrderStatus.REFUNDED;
            default:
                log.warn("Estado de pago desconocido: {}", paymentStatus);
                return null;
        }
    }

    /**
     * Construye la descripcion actualizada de la orden con informacion del pago
     */
    private String buildOrderDescription(String originalDescription, String paymentId, String paymentStatus) {
        String baseDescription = originalDescription != null ? originalDescription : "";
        return String.format("%s [Pago ID: %s, Estado: %s]", baseDescription, paymentId, paymentStatus);
    }

    @Override
    public PaymentResponse getPaymentStatus(String paymentId) {
        try {
            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.valueOf(paymentId));

            return PaymentResponse.builder()
                    .paymentId(paymentId)
                    .status(payment.getStatus())
                    .amount(payment.getTransactionAmount())
                    .dateCreated(payment.getDateCreated())
                    .dateApproved(payment.getDateApproved())
                    .build();

        } catch (MPException | MPApiException e) {
            throw new RuntimeException("Error getting payment status", e);
        }
    }

    @Override
    @Transactional
    public void cancelPayment(String paymentId) {
        try {
            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());
            PaymentClient client = new PaymentClient();
            client.cancel(Long.valueOf(paymentId));
        } catch (MPException | MPApiException e) {
            throw new RuntimeException("Error cancelling payment", e);
        }
    }

    @Override
    @Transactional
    public void refundPayment(String paymentId) {
        try {
            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());
            PaymentClient client = new PaymentClient();
            client.refund(Long.valueOf(paymentId));
        } catch (MPException | MPApiException e) {
            throw new RuntimeException("Error refunding payment", e);
        }
    }

    @Override
    @Transactional
    public void updateTransactionStatus(String preferenceId, String paymentId, String paymentStatus,
            String paymentType, OrderStatus orderStatus, String collectionId, String collectionStatus,
            String processingMode, String siteId, BigDecimal amount, LocalDateTime dateCreated,
            LocalDateTime dateApproved) {
        // log.info("Updating transaction - Preference: {}, Payment: {}, Status: {}",
        // preferenceId, paymentId, paymentStatus);

        // Extraer orderId del preferenceId (asumiendo que el externalReference es el
        // orderId)
        Long orderId = null;
        try {
            // Intentar extraer el orderId directamente si el preferenceId es numérico
            orderId = Long.valueOf(preferenceId);
        } catch (NumberFormatException e) {
            // log.debug("PreferenceId not numeric: {}", preferenceId);
        }

        // Si no se pudo extraer directamente, intentar buscar por preferenceData como
        // fallback
        MercadoPagoTransaction transaction = null;

        if (orderId != null) {
            // Buscar transacción por orderId (método preferido)
            transaction = transactionRepository.findByOrderId(orderId).orElse(null);
            // log.debug("Search by orderId: {}, Found: {}", orderId, transaction != null);
        }

        // Si no encontramos por orderId, intentar el método anterior como fallback
        if (transaction == null) {
            List<MercadoPagoTransaction> transactions = transactionRepository
                    .findByPreferenceIdContaining(preferenceId);
            transaction = transactions.isEmpty() ? null : transactions.get(0);
            // log.debug("Search by preferenceId: {}, Count: {}", preferenceId,
            // transactions.size());
        }

        if (transaction != null) {
            // log.info("Updating existing transaction ID: {}", transaction.getId());
            transaction.setPaymentId(paymentId);
            transaction.setStatus(paymentStatus);
            transaction.setPaymentMethod(paymentType);
            transaction.setCollectionId(collectionId);
            transaction.setCollectionStatus(collectionStatus);
            transaction.setProcessingMode(processingMode);
            transaction.setSiteId(siteId);

            // IMPORTANTE: Guardar el monto y fechas
            if (amount != null) {
                transaction.setAmount(amount);
                transaction.setTransactionAmount(amount);
                // log.info("Amount saved: {}", amount);
            }

            if (dateCreated != null) {
                transaction.setDateCreated(dateCreated);
                // log.info("Created date saved: {}", dateCreated);
            }

            if (dateApproved != null) {
                transaction.setDateApproved(dateApproved);
                // log.info("Approval date saved: {}", dateApproved);
            }

            transaction.setUpdatedAt(LocalDateTime.now());

            // Actualizar fecha de aprobación si el estado es "approved"
            if ("approved".equalsIgnoreCase(paymentStatus)) {
                transaction.setApprovedAt(LocalDateTime.now());
                // log.info("Payment approved - Approval date set");
            }

            transactionRepository.save(transaction);
            // log.info("Transaction updated: {}", transaction.getId());

            // Actualizar estado de la orden si se proporciona
            if (orderStatus != null && transaction.getOrder() != null) {
                Order order = transaction.getOrder();
                order.setStatus(orderStatus);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                // log.info("Order {} updated to status: {}", order.getId(), orderStatus);

                // Si la orden fue confirmada/aprobada, descontar el stock
                if (OrderStatus.CONFIRMED.equals(orderStatus)) {
                    try {
                        decreaseProductStock(order);
                        // log.info("Stock decreased for confirmed order {}", order.getId());
                    } catch (Exception e) {
                        log.error("Error decreasing stock for order {}: {}", order.getId(), e.getMessage(), e);
                        // No fallar si hay error al descontar - la orden ya fue confirmada
                    }

                    // LIMPIAR CARRITO SOLO CUANDO EL PAGO ES APROBADO
                    try {
                        Long userId = order.getUser().getId();
                        shoppingCartService.clearCart(userId);
                        // log.info("Cart cleared for user {} - Payment approved for order {}",
                        // userId, order.getId());
                    } catch (Exception e) {
                        log.error("Error clearing cart for user {}: {}",
                                order.getUser().getId(), e.getMessage());
                        // No fallar si hay error al limpiar - la orden ya fue confirmada
                    }
                }
            }
        } else {
            // log.warn("Transaction not found for preferenceId: {} or orderId: {}",
            // preferenceId, orderId);
        }
    }

    /**
     * Descuenta el stock de los productos cuando se confirma una orden
     */
    private void decreaseProductStock(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            // log.warn("Order {} has no items to decrease stock", order.getId());
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                Integer currentStock = item.getProduct().getStockQuantity();
                if (currentStock == null) {
                    currentStock = 0;
                }

                Integer newStock = currentStock - item.getQuantity();

                // Asegurar que no baje de 0
                newStock = Math.max(newStock, 0);

                item.getProduct().setStockQuantity(newStock);

                // log.info(
                // "Stock decreased - Product ID: {}, Name: {}, Before: {}, Order Qty: {},
                // After: {}",
                // item.getProduct().getId(),
                // item.getProduct().getName(),
                // currentStock,
                // item.getQuantity(),
                // newStock);
            }
        }

        // log.info("Stock decrease processed for {} products in order {}",
        // order.getItems().size(), order.getId());
    }

    public List<MercadoPagoTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    public Map<String, Object> getPreferenceInfo(String preferenceId) {
        try {
            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());
            // MercadoPago SDK doesn't have a direct method to get preference info
            // We need to get it from our database
            List<MercadoPagoTransaction> transactions = transactionRepository
                    .findByPreferenceIdContaining(preferenceId);
            Map<String, Object> result = new HashMap<>();
            if (!transactions.isEmpty()) {
                MercadoPagoTransaction transaction = transactions.get(0);
                result.put("preferenceId", preferenceId);
                result.put("status", transaction.getStatus());
                result.put("amount", transaction.getAmount());
                result.put("orderId", transaction.getOrder().getId());
            }
            return result;
        } catch (Exception e) {
            log.error("Error getting preference info", e);
            return new HashMap<>();
        }
    }

    @Override
    @Transactional
    public void checkPendingPayments() {
        // log.info("🔍 Verificando pagos pendientes...");

        try {
            com.mercadopago.MercadoPagoConfig.setAccessToken(mercadoPagoConfig.getAccessToken());

            // Buscar todas las transacciones que están en estado pendiente o en proceso
            List<MercadoPagoTransaction> pendingTransactions = transactionRepository
                    .findByStatusIn(List.of("pending", "in_process"));

            // log.info("📊 Encontradas {} transacciones pendientes para verificar",
            // pendingTransactions.size());

            PaymentClient paymentClient = new PaymentClient();

            for (MercadoPagoTransaction transaction : pendingTransactions) {
                try {
                    if (transaction.getPaymentId() != null && !transaction.getPaymentId().isEmpty()) {
                        log.debug("🔍 Verificando pago ID: {}", transaction.getPaymentId());

                        // Obtener el estado actual del pago desde MercadoPago
                        Payment payment = paymentClient.get(Long.valueOf(transaction.getPaymentId()));
                        String currentStatus = payment.getStatus();

                        log.debug("📊 Estado actual del pago {}: {}", transaction.getPaymentId(), currentStatus);

                        // Si el estado cambió, actualizar la transacción y la orden
                        if (!currentStatus.equalsIgnoreCase(transaction.getStatus())) {
                            // log.info("🔄 Estado del pago {} cambió de '{}' a '{}'",
                            // transaction.getPaymentId(), transaction.getStatus(), currentStatus);

                            // Actualizar la transacción
                            transaction.setStatus(currentStatus);
                            transaction.setUpdatedAt(LocalDateTime.now());

                            // Actualizar campos específicos según el estado
                            if ("approved".equalsIgnoreCase(currentStatus)) {
                                transaction.setApprovedAt(LocalDateTime.now());
                            } else if ("cancelled".equalsIgnoreCase(currentStatus)
                                    || "rejected".equalsIgnoreCase(currentStatus)) {
                                transaction.setCancelledAt(LocalDateTime.now());
                            }

                            transactionRepository.save(transaction);

                            // Actualizar el estado de la orden
                            Order order = transaction.getOrder();
                            if (order != null) {
                                OrderStatus newOrderStatus = mapPaymentStatusToOrderStatus(currentStatus);
                                if (newOrderStatus != null && !newOrderStatus.equals(order.getStatus())) {
                                    order.setStatus(newOrderStatus);
                                    order.setUpdatedAt(LocalDateTime.now());
                                    orderRepository.save(order);

                                    // log.info("✅ Orden {} actualizada a estado: {}", order.getId(),
                                    // newOrderStatus);
                                }
                            }
                        }
                    } else {
                        log.warn("⚠️ Transacción {} no tiene paymentId, no se puede verificar", transaction.getId());
                    }
                } catch (Exception e) {
                    log.error("❌ Error al verificar transacción {}: {}", transaction.getId(), e.getMessage());
                    // Continuar con la siguiente transacción
                }
            }

            // log.info("✅ Verificación de pagos pendientes completada");

        } catch (Exception e) {
            log.error("❌ Error general al verificar pagos pendientes: {}", e.getMessage(), e);
        }
    }
}
