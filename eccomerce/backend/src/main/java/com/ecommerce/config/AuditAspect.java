package com.ecommerce.config;

import com.ecommerce.service.interfaces.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    // Pointcuts para diferentes tipos de operaciones

    @Pointcut("execution(* com.ecommerce.service.interfaces.AuthService.authenticateUser(..))")
    public void authOperations() {}

    @Pointcut("execution(* com.ecommerce.service.interfaces.ShoppingCartService.addItem(..)) || " +
              "execution(* com.ecommerce.service.interfaces.ShoppingCartService.updateItem(..)) || " +
              "execution(* com.ecommerce.service.interfaces.ShoppingCartService.removeItem(..))")
    public void cartOperations() {}

    @Pointcut("execution(* com.ecommerce.service.interfaces.*.create*(..)) || " +
              "execution(* com.ecommerce.service.interfaces.*.save*(..)) || " +
              "execution(* com.ecommerce.service.interfaces.*.update*(..)) || " +
              "execution(* com.ecommerce.service.interfaces.*.delete*(..))")
    public void crudOperations() {}

    @Pointcut("execution(* com.ecommerce.service.interfaces.MercadoPagoService.*(..))")
    public void paymentOperations() {}

    // Aspecto principal que audita todas las operaciones
    @Around("authOperations() || cartOperations() || crudOperations() || paymentOperations()")
    public Object auditMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Obtener información del usuario actual
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        Long userId = null;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            userEmail = authentication.getName();
            // Aquí podrías obtener el userId del contexto si está disponible
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            log.info("Iniciando ejecución de {}.{} por usuario: {}", className, methodName, userEmail != null ? userEmail : "anónimo");

            result = joinPoint.proceed();

            log.info("Ejecutado exitosamente {}.{} por usuario: {}", className, methodName, userEmail != null ? userEmail : "anónimo");

        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();

            log.error("Error ejecutando {}.{} por usuario: {} - Error: {}", className, methodName, userEmail != null ? userEmail : "anónimo", errorMessage, e);

            throw e; // Re-lanzar la excepción

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // Registrar en auditoría
            try {
                String action = determineAction(methodName, className);
                String entityType = determineEntityType(className);
                String entityId = extractEntityId(joinPoint.getArgs());
                String details = buildDetails(methodName, joinPoint.getArgs(), result, executionTime);

                if (userEmail != null) {
                    auditService.logUserAction(userId, userEmail, action, entityType, entityId, details, success, errorMessage);
                } else {
                    auditService.logEvent(action, entityType, entityId, details, success, errorMessage);
                }

            } catch (Exception auditException) {
                log.error("Error al crear registro de auditoría: {}", auditException.getMessage());
            }
        }

        return result;
    }

    private String determineAction(String methodName, String className) {
        if (methodName.startsWith("create") || methodName.startsWith("save") || methodName.equals("registerUser")) {
            return "CREAR";
        } else if (methodName.startsWith("update")) {
            return "ACTUALIZAR";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "ELIMINAR";
        } else if (methodName.equals("authenticateUser")) {
            return "INICIAR_SESION";
        } else if (methodName.contains("addItem")) {
            return "AGREGAR_AL_CARRITO";
        } else if (methodName.contains("removeItem")) {
            return "REMOVER_DEL_CARRITO";
        } else if (methodName.contains("Payment")) {
            return "PAGO";
        } else {
            return "EJECUTAR";
        }
    }

    private String determineEntityType(String className) {
        if (className.contains("Auth")) {
            return "AUTENTICACION";
        } else if (className.contains("User")) {
            return "USUARIO";
        } else if (className.contains("Product")) {
            return "PRODUCTO";
        } else if (className.contains("Order")) {
            return "PEDIDO";
        } else if (className.contains("Cart")) {
            return "CARRITO_COMPRAS";
        } else if (className.contains("Payment")) {
            return "PAGO";
        } else {
            return "SISTEMA";
        }
    }

    private String extractEntityId(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg != null) {
                    // Intentar extraer ID de diferentes tipos de objetos
                    if (arg instanceof Long || arg instanceof Integer || arg instanceof String) {
                        return arg.toString();
                    }
                    // Si es un objeto con método getId(), intentar usarlo
                    try {
                        Method getIdMethod = arg.getClass().getMethod("getId");
                        Object id = getIdMethod.invoke(arg);
                        if (id != null) {
                            return id.toString();
                        }
                    } catch (Exception e) {
                        // Ignorar si no tiene getId()
                    }
                }
            }
        }
        return null;
    }

    private String buildDetails(String methodName, Object[] args, Object result, long executionTime) {
        StringBuilder details = new StringBuilder();
        details.append("Method: ").append(methodName);
        details.append(", Args: ").append(args != null ? args.length : 0);
        details.append(", Execution time: ").append(executionTime).append("ms");

        if (result != null) {
            details.append(", Result type: ").append(result.getClass().getSimpleName());
        }

        return details.toString();
    }
}
