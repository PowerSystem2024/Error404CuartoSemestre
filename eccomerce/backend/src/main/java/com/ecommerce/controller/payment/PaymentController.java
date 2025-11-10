package com.ecommerce.controller.payment;

import com.ecommerce.config.MercadoPagoConfiguration;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.model.entity.MercadoPagoTransaction;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.repository.MercadoPagoTransactionRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.interfaces.MercadoPagoService;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@Slf4j
@Tag(name = "Administración de Pagos", description = "APIs para gestionar pagos con MercadoPago")
public class PaymentController {

    private final MercadoPagoService mercadoPagoService;
    private final OrderRepository orderRepository;
    private final MercadoPagoTransactionRepository transactionRepository;
    private final MercadoPagoConfiguration mercadoPagoConfig;

    public PaymentController(MercadoPagoService mercadoPagoService, OrderRepository orderRepository,
            MercadoPagoTransactionRepository transactionRepository, MercadoPagoConfiguration mercadoPagoConfig) {
        this.mercadoPagoService = mercadoPagoService;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.mercadoPagoConfig = mercadoPagoConfig;
    }

    @GetMapping("/test-mercadopago")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Probar conectividad con MercadoPago", description = "Endpoint de prueba para verificar la configuración de MercadoPago")
    public ResponseEntity<Map<String, Object>> testMercadoPagoConnection() {
        try {
            log.info("🧪 Probando conectividad con MercadoPago...");

            // Verificar configuración
            String token = mercadoPagoConfig.getAccessToken();
            boolean tokenConfigured = token != null && !token.trim().isEmpty();

            log.info("🔑 Token configurado: {}",
                    tokenConfigured ? (token != null && token.length() > 10
                            ? token.substring(0, 5) + "..." + token.substring(token.length() - 5)
                            : "SÍ") : "NO");

            // Verificar URLs
            Map<String, Object> configStatus = new HashMap<>();
            configStatus.put("tokenConfigured", tokenConfigured);
            configStatus.put("activeProfile", mercadoPagoConfig.getActiveProfile());
            configStatus.put("successUrl", mercadoPagoConfig.getSuccessUrl());
            configStatus.put("failureUrl", mercadoPagoConfig.getFailureUrl());
            configStatus.put("pendingUrl", mercadoPagoConfig.getPendingUrl());
            configStatus.put("webhookUrl", mercadoPagoConfig.getWebhookUrl());

            // Intentar una operación simple con MercadoPago
            try {
                com.mercadopago.MercadoPagoConfig.setAccessToken(token);
                log.info("✅ Token configurado en SDK de MercadoPago");

                // Crear una preferencia de prueba mínima
                PreferenceItemRequest testItem = PreferenceItemRequest.builder()
                        .title("Test Product")
                        .quantity(1)
                        .unitPrice(BigDecimal.valueOf(1.0))
                        .currencyId("ARS")
                        .build();

                PreferenceRequest testRequest = PreferenceRequest.builder()
                        .items(List.of(testItem))
                        .build();

                PreferenceClient client = new PreferenceClient();
                Preference testPreference = client.create(testRequest);

                configStatus.put("mercadopagoConnection", "SUCCESS");
                configStatus.put("testPreferenceId", testPreference.getId());
                log.info("✅ Conectividad con MercadoPago verificada. Preference ID de prueba: {}",
                        testPreference.getId());

            } catch (Exception mpError) {
                log.error("❌ Error de conectividad con MercadoPago: {}", mpError.getMessage());
                configStatus.put("mercadopagoConnection", "FAILED");
                configStatus.put("mercadopagoError", mpError.getMessage());
            }

            return ResponseEntity.ok(configStatus);

        } catch (Exception e) {
            log.error("❌ Error en test de MercadoPago: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error testing MercadoPago connection",
                    "details", e.getMessage()));
        }
    }

    @PostMapping("/create-preference")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear preferencia de pago simple", description = "Crea una preferencia de pago con datos básicos o para una orden existente")
    public ResponseEntity<Map<String, Object>> createPreference(@RequestBody Map<String, Object> request) {
        try {
            log.info("📥 Request recibido para create-preference: {}", request);

            // Inicializar variables
            String externalReference = null;

            // Log detallado de cada campo importante
            if (request.containsKey("orderId")) {
                log.info("🛒 OrderId recibido: {}", request.get("orderId"));
            }
            if (request.containsKey("price")) {
                log.info("💰 Price recibido: {}", request.get("price"));
            }
            if (request.containsKey("quantity")) {
                log.info("🔢 Quantity recibido: {}", request.get("quantity"));
            }
            if (request.containsKey("description")) {
                log.info("📝 Description recibido: {}", request.get("description"));
            }
            if (request.containsKey("external_reference")) {
                log.info("🔗 External reference recibido: {}", request.get("external_reference"));
            }
            // Check if orderId is provided
            if (request.containsKey("orderId")) {
                Long orderId = Long.valueOf(request.get("orderId").toString());
                log.info("🛒 Creando preferencia para la orden ID: {}", orderId);

                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                log.info("✅ Orden encontrada: {}", order.getId());

                PaymentResponse response = mercadoPagoService.createPaymentPreference(order);
                log.info("💳 Preferencia creada para la orden {}", order.getId());

                // Get initPoint from response or build it
                String initPoint = response.getInitPoint();

                // If initPoint is null, build standard URL from preferenceId
                if (initPoint == null && response.getPreferenceId() != null) {
                    initPoint = "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id="
                            + response.getPreferenceId();
                    log.info("🔗 URL construida manualmente: {}", initPoint);
                }

                // Return complete response with init_point and sandbox_init_point if applicable
                Map<String, Object> fullResponse = new HashMap<>();
                fullResponse.put("id", response.getPreferenceId());

                if (initPoint != null) {
                    fullResponse.put("init_point", initPoint);
                }

                // Include sandbox_init_point if we're in sandbox mode
                // if (response.getSandboxInitPoint() != null) {
                // fullResponse.put("sandbox_init_point", response.getSandboxInitPoint());
                // log.info("🔗 URL Sandbox: {}", response.getSandboxInitPoint());
                // }

                log.info("📤 Enviando respuesta con preferenceId: {}", response.getPreferenceId());
                return ResponseEntity.ok(fullResponse);
            }

            // Create simple preference with basic data
            Number price = (Number) request.get("price");
            Number quantity = (Number) request.get("quantity");
            String description = (String) request.get("description");
            String autoReturn = (String) request.get("auto_return");

            // Si venimos con orderId, los campos price y quantity no son requeridos
            // Si no tenemos ninguno de ellos, devolvemos un error
            if (price == null && quantity == null) {
                log.error("❌ Error en la solicitud: No se proporcionaron price ni quantity");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Se requiere price y quantity, o un orderId válido"));
            }

            // Si solo falta uno de los campos, lo inferimos del otro
            if (price == null) {
                price = 1.0; // Default price si solo se envía quantity
                log.info("ℹ️ Usando price por defecto: {}", price);
            }
            if (quantity == null) {
                quantity = 1; // Default quantity si solo se envía price
                log.info("ℹ️ Usando quantity por defecto: {}", quantity);
            }

            // Preparar external_reference para la lógica de enhancedDescription
            String tempExternalReference = (String) request.get("external_reference");

            // Si no hay external_reference pero hay orderId, generar uno
            if (tempExternalReference == null || tempExternalReference.isEmpty()) {
                if (request.containsKey("orderId")) {
                    Long orderId = Long.valueOf(request.get("orderId").toString());
                    tempExternalReference = "order_" + orderId;
                    log.info("🔗 External reference generado desde orderId para descripción: {}",
                            tempExternalReference);
                }
            }

            // Construir descripción mejorada con número de orden si está disponible
            String enhancedDescription = description != null ? description : "Compra en E-commerce";

            // Si tenemos external_reference con formato "order_XXX", extraer el número de
            // orden
            if (tempExternalReference != null && tempExternalReference.startsWith("order_")) {
                try {
                    String orderIdStr = tempExternalReference.substring("order_".length());
                    Long orderId = Long.parseLong(orderIdStr);

                    // Si la descripción no incluye ya el número de orden, agregarlo
                    if (!enhancedDescription.contains("Orden #") && !enhancedDescription.contains("Order #")) {
                        if (quantity != null && quantity.intValue() > 1) {
                            enhancedDescription = String.format("Orden #%d - %d productos", orderId,
                                    quantity.intValue());
                        } else {
                            enhancedDescription = String.format("Orden #%d - %s", orderId, enhancedDescription);
                        }
                        log.info("📝 Descripción mejorada con número de orden: {}", enhancedDescription);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo extraer orderId de external_reference: {}", tempExternalReference);
                }
            }

            // Crear items para la preferencia
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();

            item.put("title", enhancedDescription);
            item.put("description", enhancedDescription);
            item.put("quantity", quantity.intValue());
            item.put("currency_id", "ARS");
            item.put("unit_price", price.doubleValue());
            items.add(item);

            log.info("📦 Item creado para MercadoPago:");
            log.info("   📝 Title: {}", item.get("title"));
            log.info("   📝 Description: {}", item.get("description"));
            log.info("   🔢 Quantity: {}", item.get("quantity"));
            log.info("   💰 Unit Price: ${}", item.get("unit_price"));
            log.info("   🔗 Temp External Reference: {}", tempExternalReference);

            // Obtener el origen de la solicitud para URLs de retorno
            String origin = "https://localhost:5173"; // Default para desarrollo local (siempre HTTPS)
            String requestOrigin = request.containsKey("origin") ? (String) request.get("origin") : null;
            if (requestOrigin != null && !requestOrigin.isEmpty()) {
                origin = requestOrigin;
            }

            // URLs de retorno basadas en el origin
            Map<String, String> backUrls = new HashMap<>();

            // Siempre usar HTTPS (en cualquier ambiente)
            backUrls.put("success", origin + "/success");
            backUrls.put("failure", origin + "/failure");
            backUrls.put("pending", origin + "/pending");

            log.info("📍 URLs de retorno configuradas: success={}, failure={}, pending={}",
                    backUrls.get("success"), backUrls.get("failure"), backUrls.get("pending"));

            // Crear preferencia usando el servicio
            Map<String, Object> preferenceData = new HashMap<>();
            preferenceData.put("items", items);
            preferenceData.put("back_urls", backUrls);

            // Agregar descripción general de la preferencia
            Map<String, Object> singleItem = (Map<String, Object>) items.get(0);
            String itemTitle = (String) singleItem.get("title");
            preferenceData.put("statement_descriptor",
                    itemTitle.length() > 20 ? itemTitle.substring(0, 20) : itemTitle);

            log.info("🏷️ Statement descriptor: {}", preferenceData.get("statement_descriptor"));

            // CONFIGURACIÓN ORIGINAL (comentada para el desafío)
            // externalReference = (String) request.get("external_reference");
            // // Si no hay external_reference pero hay orderId, usarlo
            // if (externalReference == null || externalReference.isEmpty()) {
            // if (request.containsKey("orderId")) {
            // Long orderId = Long.valueOf(request.get("orderId").toString());
            // externalReference = "order_" + orderId;
            // log.info("🔗 External reference generado desde orderId: {}",
            // externalReference);
            // } else {
            // // Generar un reference único basado en timestamp
            // externalReference = "payment_" + System.currentTimeMillis();
            // log.info("🔗 External reference generado automáticamente: {}",
            // externalReference);
            // }
            // }

            // CONFIGURACIÓN PARA EL DESAFÍO DE MERCADOPAGO
            externalReference = "lucas_ortega54@hotmail.com";
            log.info("🏆 External reference configurado para desafío: {}", externalReference);

            if (externalReference != null && !externalReference.isEmpty()) {
                preferenceData.put("external_reference", externalReference);
                log.info("🔗 External reference incluido: {}", externalReference);
            }

            // Añadir auto_return si fue proporcionado
            if (autoReturn != null) {
                preferenceData.put("auto_return", autoReturn);
                log.info("📍 Auto-return configurado: {}", autoReturn);

                // Verificar que success esté presente cuando se especifica auto_return
                // (requisito de MercadoPago)
                if (backUrls.get("success") == null || backUrls.get("success").isEmpty()) {
                    backUrls.put("success", mercadoPagoConfig.getSuccessUrl());
                    log.info("📍 Auto-return está presente pero falta success URL, usando URL por defecto: {}",
                            mercadoPagoConfig.getSuccessUrl());
                }
            }

            try {
                // Debug log de los datos que se enviarán
                log.info("🔍 Datos de preferencia a enviar: {}", preferenceData);

                // Verificación extra de que back_urls contiene success cuando auto_return está
                // presente
                if (autoReturn != null && !autoReturn.isEmpty()) {
                    if (backUrls.get("success") == null || backUrls.get("success").isEmpty()) {
                        log.warn("⚠️ Auto-return está presente pero falta success URL, corrigiendo antes de enviar");
                        backUrls.put("success", mercadoPagoConfig.getSuccessUrl());
                        preferenceData.put("back_urls", backUrls);
                    }
                }

                // Crear la preferencia
                String preferenceId = mercadoPagoService.createSimplePreference(preferenceData);

                log.info("✅ Preferencia creada con ID: {}", preferenceId);

                // Verificar si estamos en modo sandbox para incluir la URL de sandbox
                // boolean isSandboxMode =
                // "sandbox".equals(System.getProperty("mercadopago.sdk.environment"));
                // log.info("🔧 Modo de MercadoPago: {}", isSandboxMode ? "SANDBOX" :
                // "PRODUCCIÓN");

                // Crear URL estándar (siempre incluir la URL de producción)
                String initPoint = "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=" + preferenceId;

                // Crear URL de sandbox (solo si estamos en modo sandbox)
                // String sandboxInitPoint = isSandboxMode
                // ? "https://www.mercadopago.com.ar/sandbox/checkout/v1/redirect?pref_id=" +
                // preferenceId
                // : null;

                log.info("🔗 URLs generadas para MercadoPago:");
                log.info("🔗 Producción: {}", initPoint);
                // if (isSandboxMode) {
                // log.info("🔗 Sandbox: {}", sandboxInitPoint);
                // }

                // Devolver respuesta
                Map<String, Object> response = new HashMap<>();
                response.put("id", preferenceId);
                response.put("init_point", initPoint);

                // // Incluir URL de sandbox solo si estamos en modo sandbox
                // if (isSandboxMode) {
                // response.put("sandbox_init_point", sandboxInitPoint);
                // log.info("🔗 URL Sandbox: {}", sandboxInitPoint);
                // }

                log.info("🔗 URL Producción: {}", initPoint);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("❌ Error creating preference: {}", e.getMessage(), e);

                // Intentar extraer más detalles del error
                String errorDetail = "No hay detalles adicionales";
                if (e.getCause() instanceof com.mercadopago.exceptions.MPApiException mpApiEx) {
                    try {
                        errorDetail = "API Error [" + mpApiEx.getApiResponse().getStatusCode() + "]: " +
                                mpApiEx.getApiResponse().getContent();
                        log.error("❌ Detalles del error MercadoPago: {}", errorDetail);
                    } catch (Exception ex) {
                        log.error("❌ No se pudo extraer detalles del error MercadoPago", ex);
                    }
                }

                // Manejar diferentes tipos de errores
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Error desconocido";

                // Revisar si es un problema de configuración
                if (errorMessage.contains("access_token") || errorMessage.contains("MP_ACCESS_TOKEN") ||
                        errorMessage.contains("401") || errorMessage.contains("unauthorized")) {
                    log.error("❌ Error de configuración de MercadoPago. Token inválido o no configurado");

                    // Verificar token actual (enmascarado)
                    String token = com.mercadopago.MercadoPagoConfig.getAccessToken();
                    if (token != null && token.length() > 10) {
                        log.error("❌ Token actual (enmascarado): {}...{}",
                                token.substring(0, 5), token.substring(token.length() - 5));
                    } else {
                        log.error("❌ Token actual: VACÍO o NULL");
                    }

                    log.error("❌ Perfil activo: {}", System.getProperty("spring.profiles.active"));

                    return ResponseEntity.status(500).body(Map.of(
                            "error", "Error de configuración de MercadoPago",
                            "details",
                            "Token de acceso inválido o no configurado. Verifica que MP_ACCESS_TOKEN esté correctamente configurado en tu archivo .env"));
                }

                // Error general
                log.error("❌ Error general al crear preferencia: {}", errorMessage);

                // Verificar errores específicos comunes de MercadoPago
                if (errorMessage.contains("auto_return invalid") ||
                        errorMessage.contains("back_url.success must be defined")) {

                    log.error("⚠️ Error de validación de MercadoPago: auto_return requiere URLs de retorno válidas");

                    // Mensaje de error más claro para el cliente
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "Error de configuración de MercadoPago",
                            "details",
                            "Cuando se especifica auto_return, es obligatorio proporcionar una URL de éxito válida",
                            "tech_details", errorMessage));
                }

                return ResponseEntity.status(500).body(Map.of(
                        "error", "No se pudo crear la preferencia",
                        "details", errorMessage));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify/{preferenceId}")
    @Operation(summary = "Verificar pago", description = "Verifica un pago por su ID de preferencia y actualiza el estado de la orden")
    public ResponseEntity<?> verifyPayment(
            @PathVariable String preferenceId,
            @RequestParam String status,
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String merchant_order_id,
            @RequestParam(required = false) String payment_type,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) String collection_status,
            @RequestParam(required = false) String collection_id,
            @RequestParam(required = false) String processing_mode,
            @RequestParam(required = false) String site_id) {
        try {
            log.info(
                    "🔍 Verificando pago con preferenceId: {}, status: {}, paymentId: {}, merchantOrderId: {}, paymentType: {}, externalReference: {}, collectionStatus: {}, collectionId: {}, processingMode: {}, siteId: {}",
                    preferenceId, status, payment_id, merchant_order_id, payment_type, external_reference,
                    collection_status, collection_id, processing_mode, site_id);

            // Si tenemos un payment_id, podemos verificar su estado
            PaymentResponse paymentDetails = null;
            if (payment_id != null && !payment_id.isEmpty() && !"null".equals(payment_id)) {
                paymentDetails = mercadoPagoService.getPaymentStatus(payment_id);
                log.info("💳 Estado del pago {}: {}", payment_id, paymentDetails.getStatus());
            }

            // Extraer el ID de orden del external_reference si existe
            Long orderId = null;
            if (external_reference != null && !external_reference.isEmpty() && !"null".equals(external_reference)) {
                try {
                    // El formato es "order_123" donde 123 es el ID de la orden
                    if (external_reference.startsWith("order_")) {
                        String orderIdStr = external_reference.substring("order_".length());
                        orderId = Long.parseLong(orderIdStr);
                        log.info("🔍 ID de orden extraído de external_reference: {}", orderId);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo extraer el ID de orden del external_reference: {}", external_reference, e);
                }
            }

            // Si no tenemos orderId del external_reference, intentar obtenerlo de la
            // descripción de la preferencia
            if (orderId == null) {
                try {
                    Map<String, Object> preferenceInfo = mercadoPagoService.getPreferenceInfo(preferenceId);
                    if (preferenceInfo.containsKey("orderId")) {
                        orderId = (Long) preferenceInfo.get("orderId");
                        log.info("🔍 ID de orden extraído de descripción de preferencia: {}", orderId);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo obtener información de preferencia para extraer orderId: {}",
                            e.getMessage());
                }
            }

            // Si aún no tenemos orderId, intentar buscar en la base de datos por
            // preferenceId
            if (orderId == null) {
                try {
                    log.info("🔍 Buscando transacción en BD por preferenceId: {}", preferenceId);
                    List<MercadoPagoTransaction> transactions = transactionRepository
                            .findByPreferenceIdContaining(preferenceId);
                    if (!transactions.isEmpty()) {
                        MercadoPagoTransaction transaction = transactions.get(0);
                        if (transaction.getOrder() != null) {
                            orderId = transaction.getOrder().getId();
                            log.info("🔍 ID de orden extraído de transacción en BD: {}", orderId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Error al buscar transacción en BD por preferenceId: {}", e.getMessage());
                }
            }

            // Buscar la transacción asociada a este preferenceId y actualizar la orden
            OrderStatus orderStatus = null;
            String paymentStatus = status;

            if (paymentDetails != null) {
                paymentStatus = paymentDetails.getStatus();
            }

            // Determinar el estado de la orden según el estado del pago
            // Solo actualizar si el pago es aprobado
            if (paymentStatus != null && "approved".equalsIgnoreCase(paymentStatus.trim())) {
                orderStatus = OrderStatus.CONFIRMED;
                log.info("✅ Pago aprobado: Actualizando orden a CONFIRMED");
            } else {
                log.info("⚠️ Pago no aprobado (estado: {}): No se actualizará el estado de la orden", paymentStatus);
                orderStatus = null; // No actualizar
            }

            // Si tenemos un ID de orden de external_reference y el pago es aprobado,
            // actualizar directamente la orden
            if (orderId != null && orderStatus != null) {
                try {
                    Order order = orderRepository.findById(orderId)
                            .orElse(null);

                    if (order != null) {
                        log.info("✅ Actualizando directamente la orden {} a estado {}", orderId, orderStatus);
                        order.setStatus(orderStatus);
                        orderRepository.save(order);
                        log.info("✅ Orden {} actualizada exitosamente a estado {}", orderId, orderStatus);
                    } else {
                        log.warn("⚠️ No se encontró la orden con ID {} mencionada en external_reference", orderId);
                    }
                } catch (Exception e) {
                    log.error("❌ Error al actualizar directamente la orden {}: {}", orderId, e.getMessage(), e);
                }
            }

            // Actualizar la transacción en la base de datos (o crearla si no existe)
            try {
                // Extraer monto y fechas de paymentDetails si están disponibles
                BigDecimal amount = null;
                LocalDateTime dateCreated = null;
                LocalDateTime dateApproved = null;

                if (paymentDetails != null) {
                    amount = paymentDetails.getAmount();
                    if (paymentDetails.getDateCreated() != null) {
                        dateCreated = paymentDetails.getDateCreated().toLocalDateTime();
                    }
                    if (paymentDetails.getDateApproved() != null) {
                        dateApproved = paymentDetails.getDateApproved().toLocalDateTime();
                    }
                    log.info("💰 Datos del pago obtenidos - Amount: {}, DateCreated: {}, DateApproved: {}",
                            amount, dateCreated, dateApproved);
                } else if (orderId != null) {
                    // Si no tenemos paymentDetails, obtener el monto de la orden
                    try {
                        Order order = orderRepository.findById(orderId).orElse(null);
                        if (order != null && order.getTotalAmount() != null) {
                            amount = order.getTotalAmount();
                            log.info("💰 Monto obtenido de la orden {}: {}", orderId, amount);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ No se pudo obtener el monto de la orden: {}", e.getMessage());
                    }
                }

                log.info(
                        "📣 Llamando a updateTransactionStatus con preferenceId={}, payment_id={}, paymentStatus={}, collectionId={}, collectionStatus={}, processingMode={}, siteId={}, amount={}, orderStatus={}",
                        preferenceId, payment_id, paymentStatus, collection_id, collection_status, processing_mode,
                        site_id, amount, orderStatus);

                mercadoPagoService.updateTransactionStatus(
                        preferenceId,
                        "null".equals(payment_id) ? null : payment_id,
                        paymentStatus,
                        "null".equals(payment_type) ? null : payment_type,
                        orderStatus,
                        "null".equals(collection_id) ? null : collection_id,
                        "null".equals(collection_status) ? null : collection_status,
                        "null".equals(processing_mode) ? null : processing_mode,
                        "null".equals(site_id) ? null : site_id,
                        amount,
                        dateCreated,
                        dateApproved);

                log.info("✅ Transacción y estado de orden actualizados correctamente");
            } catch (Exception e) {
                log.error("❌ Error al actualizar la transacción: {}", e.getMessage(), e);
                // Agregar el stacktrace completo para mejor diagnóstico
                e.printStackTrace();
            }

            // Preparar respuesta con los detalles del pago
            Map<String, Object> response = new HashMap<>();
            if (paymentDetails != null) {
                response.put("status", paymentDetails.getStatus());
                response.put("payment_id", paymentDetails.getPaymentId());
                response.put("amount", paymentDetails.getAmount());
                response.put("date_created", paymentDetails.getDateCreated());
                response.put("date_approved", paymentDetails.getDateApproved());
                response.put("payment_type", payment_type);
                response.put("collection_id", collection_id);
                response.put("collection_status", collection_status);
                response.put("processing_mode", processing_mode);
                response.put("site_id", site_id);
                response.put("order_status", orderStatus != null ? orderStatus.name() : "UNKNOWN");
            } else {
                response.put("status", status);
                response.put("payment_id", payment_id);
                response.put("collection_id", collection_id);
                response.put("collection_status", collection_status);
                response.put("merchant_order_id", merchant_order_id);
                response.put("preference_id", preferenceId);
                response.put("payment_type", payment_type);
                response.put("processing_mode", processing_mode);
                response.put("site_id", site_id);
                response.put("order_status", orderStatus != null ? orderStatus.name() : "UNKNOWN");
                response.put("message", "Información del pago procesada correctamente");

                // Si tenemos orderId, obtener el monto de la orden
                if (orderId != null) {
                    try {
                        Order order = orderRepository.findById(orderId).orElse(null);
                        if (order != null && order.getTotalAmount() != null) {
                            response.put("amount", order.getTotalAmount());
                            log.info("📦 Monto obtenido de la orden {}: {}", orderId, order.getTotalAmount());
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ No se pudo obtener el monto de la orden {}: {}", orderId, e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error al verificar pago: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al verificar el pago",
                    "details", e.getMessage()));
        }
    }

    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reembolsar pago", description = "Reembolsa un pago")
    public ResponseEntity<Void> refundPayment(@PathVariable String paymentId) {
        mercadoPagoService.refundPayment(paymentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/__mp_token_test")
    @Operation(summary = "Verificar token de MercadoPago", description = "Verifica si el token de MercadoPago está configurado correctamente")
    public ResponseEntity<Map<String, Object>> verifyMercadoPagoToken() {
        try {
            String accessToken = mercadoPagoConfig.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "ok", false,
                        "reason", "MP_ACCESS_TOKEN vacío"));
            }

            // Mostrar información de configuración
            Map<String, Object> response = new HashMap<>();

            // Datos del token (enmascarado por seguridad)
            response.put("token_preview",
                    accessToken.length() > 10
                            ? accessToken.substring(0, 5) + "..." + accessToken.substring(accessToken.length() - 5)
                            : "inválido");
            response.put("token_length", accessToken.length());

            // Configuración activa
            boolean isSandboxMode = "sandbox".equals(System.getProperty("mercadopago.sdk.environment"));
            response.put("environment", isSandboxMode ? "sandbox" : "production");
            response.put("active_profile", System.getProperty("spring.profiles.active"));

            // Probar conexión real con MercadoPago usando el SDK
            try {
                // Configurar token para la prueba
                com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);

                // Intentar hacer una consulta simple
                com.mercadopago.client.payment.PaymentClient client = new com.mercadopago.client.payment.PaymentClient();
                // No consultamos un pago real, solo verificamos que la API responda
                // correctamente
                // a una solicitud con el token (respuesta 404 es aceptable porque el ID no
                // existe)
                client.get(1L);

                response.put("ok", true);
                response.put("status", "Token válido y autorizado");
                response.put("connection", "successful");
            } catch (com.mercadopago.exceptions.MPApiException mpEx) {
                // Si es un error 404, es normal (significa que el token es válido pero el pago
                // no existe)
                if (mpEx.getApiResponse().getStatusCode() == 404) {
                    response.put("ok", true);
                    response.put("status", "Token válido (API responde con 404 para el ID de prueba)");
                    response.put("connection", "successful");
                } else if (mpEx.getApiResponse().getStatusCode() == 401) {
                    response.put("ok", false);
                    response.put("status", "Token inválido o no autorizado (error 401)");
                    response.put("connection", "unauthorized");
                    response.put("error_details", mpEx.getApiResponse().getContent());
                } else {
                    response.put("ok", false);
                    response.put("status", "Error de API: " + mpEx.getApiResponse().getStatusCode());
                    response.put("connection", "failed");
                    response.put("error_details", mpEx.getApiResponse().getContent());
                }
            } catch (Exception ex) {
                response.put("ok", false);
                response.put("status", "Error al conectar: " + ex.getMessage());
                response.put("connection", "failed");
            }

            // URLs configuradas
            Map<String, String> urls = new HashMap<>();
            urls.put("webhook", mercadoPagoConfig.getWebhookUrl());
            urls.put("success", mercadoPagoConfig.getSuccessUrl());
            urls.put("failure", mercadoPagoConfig.getFailureUrl());
            urls.put("pending", mercadoPagoConfig.getPendingUrl());
            response.put("configured_urls", urls);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al verificar token de MercadoPago", e);
            return ResponseEntity.status(500).body(Map.of(
                    "ok", false,
                    "error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    @Operation(summary = "Procesar webhook de MercadoPago", description = "Procesa webhooks de MercadoPago")
    public ResponseEntity<Void> processWebhook(
            @RequestBody String webhookData,
            @RequestHeader("x-signature") String signature) {
        mercadoPagoService.processWebhook(webhookData, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verificar pagos pendientes", description = "Verifica y actualiza el estado de todos los pagos pendientes")
    public ResponseEntity<Map<String, Object>> checkPendingPayments() {
        try {
            log.info("🔍 Iniciando verificación de pagos pendientes...");
            mercadoPagoService.checkPendingPayments();

            return ResponseEntity.ok(Map.of(
                    "message", "Verificación de pagos pendientes completada",
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("❌ Error al verificar pagos pendientes: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al verificar pagos pendientes",
                    "details", e.getMessage()));
        }
    }
}
