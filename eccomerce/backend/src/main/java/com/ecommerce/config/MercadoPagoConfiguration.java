package com.ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class MercadoPagoConfiguration {

    @Value("${MP_ACCESS_TOKEN}")
    private String accessToken;

    @Value("${MP_INTEGRATOR_ID}")
    private String integratorId;

    @Value("${MP_WEBHOOK_URL}")
    private String webhookUrl;

    @Value("${MP_SUCCESS_URL}")
    private String successUrl;

    @Value("${MP_FAILURE_URL}")
    private String failureUrl;

    @Value("${MP_PENDING_URL}")
    private String pendingUrl;

    @Value("${MP_USE_SANDBOX:true}")
    private boolean useSandbox;

    @Value("${SPRING_PROFILES_ACTIVE:dev}")
    private String activeProfile;

    @PostConstruct
    public void configureMercadoPago() {

        boolean isSandboxMode = useSandbox && "dev".equals(activeProfile);
        String environment = isSandboxMode ? "sandbox" : "production";
        System.setProperty("mercadopago.sdk.environment", environment);

        // Configurar Integrator ID para identificación como desarrollador certificado
        if (integratorId != null && !integratorId.isEmpty()) {
            com.mercadopago.MercadoPagoConfig.setIntegratorId(integratorId);
        }

    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public String getFailureUrl() {
        return failureUrl;
    }

    public String getPendingUrl() {
        return pendingUrl;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public String getIntegratorId() {
        return integratorId;
    }
}
