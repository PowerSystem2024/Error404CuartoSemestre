import { useEffect, useState, useRef } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { useCartStore } from '../stores/cartStore';
import { useUserStore } from '../stores/userStore';
import { paymentAPI, orderAPI } from '../services/api';

const Success = () => {
    const [searchParams] = useSearchParams();
    const { clearCart } = useCartStore();
    const { token, user, initializeFromStorage } = useUserStore();
    const navigate = useNavigate();
    const [verifying, setVerifying] = useState(true);
    const [paymentDetails, setPaymentDetails] = useState<any>(null);
    const [orderDetails, setOrderDetails] = useState<any>(null);
    const [error, setError] = useState<string | null>(null);
    const hasVerified = useRef(false);

    // Intentar restaurar la autenticación al cargar el componente
    useEffect(() => {
        // Intenta restaurar la sesión desde localStorage si es necesario
        if (!token || !user) {
            // console.log('� Success: Intentando restaurar la autenticación desde localStorage');
            initializeFromStorage();
        } else {
            // console.log('Success: Usuario autenticado:', user.name, 'con rol:', user.role);
        }
    }, [token, user, initializeFromStorage]);

    // Check for all possible parameters returned by Mercado Pago
    const paymentId = searchParams.get("payment_id");
    const collectionId = searchParams.get("collection_id");
    const status = searchParams.get("status");
    const collectionStatus = searchParams.get("collection_status");
    const merchantOrderId = searchParams.get("merchant_order_id");
    const externalReference = searchParams.get("external_reference");
    const preferenceId = searchParams.get("preference_id");
    const paymentType = searchParams.get("payment_type");
    const processingMode = searchParams.get("processing_mode");
    const siteId = searchParams.get("site_id");
    const merchant = searchParams.get("merchant");

    // Capturar todos los parámetros para debugging
    const allParams = Object.fromEntries(searchParams.entries());

    useEffect(() => {
        console.log("Parámetros de pago recibidos:", {
            paymentId,
            collectionId,
            collectionStatus,
            status,
            merchantOrderId,
            externalReference,
            preferenceId,
            paymentType,
            processingMode,
            siteId,
            merchant,
            allParams
        });

        // Ejecutar verificación solo una vez
        if (hasVerified.current) return;
        hasVerified.current = true;

        const verifyPayment = async () => {
            try {
                setVerifying(true);
                setError(null);

                // Usar el ID que esté disponible
                const actualPaymentId = paymentId || undefined;

                // Si tenemos un ID de preferencia, podemos verificarlo con el backend
                if (preferenceId) {
                    // console.log("Verificando pago con preferenceId:", preferenceId);
                    try {
                        // Asegurarnos de que status sea "approved" para pagos exitosos
                        const paymentStatus = status === "approved" ? "approved" : (status || "unknown");
                        // console.log("Estado de pago a enviar:", paymentStatus);

                        // Asegurarnos de que el token esté disponible para la solicitud
                        if (!token && user) {
                            // console.log("Restaurando token para verificación de pago");
                            initializeFromStorage();
                        }

                        // Extraer el ID de orden del externalReference si existe
                        let orderId: number | undefined;
                        if (externalReference) {
                            try {
                                // El formato debería ser 'order_123' donde 123 es el ID de la orden
                                const orderIdMatch = externalReference.match(/order_(\d+)/);
                                if (orderIdMatch && orderIdMatch[1]) {
                                    orderId = parseInt(orderIdMatch[1], 10);
                                    // console.log("ID de orden extraído del external_reference:", orderId);
                                }
                            } catch (err) {
                                // Warning removed for production mode
                            }
                        }

                        const result = await paymentAPI.verifyPayment(preferenceId, {
                            paymentId: actualPaymentId,
                            status: paymentStatus,
                            merchant_order_id: merchantOrderId || undefined,
                            payment_type: paymentType || undefined,
                            external_reference: externalReference || undefined,
                            collection_status: collectionStatus || undefined,
                            processing_mode: processingMode || undefined,
                            site_id: siteId || undefined,
                            // Añadir información extra para debugging
                            additionalInfo: {
                                page: "success",
                                timestamp: new Date().toISOString(),
                                allParams: allParams,
                                userId: user?.id, // Incluir ID del usuario si está disponible
                                orderId: orderId // Incluir el ID de la orden extraído
                            }
                        });
                        console.log("Resultado de verificación:", result.data);
                        setPaymentDetails(result.data);
                    } catch (err) {
                        // Error handling removed for production mode
                        // Si falla, intentar con el payment_id directamente
                        if (actualPaymentId) {
                            // console.log("Intentando verificar con paymentId:", actualPaymentId);
                            const result = await paymentAPI.getPaymentStatus(actualPaymentId);
                            // console.log("Estado de pago:", result.data);
                            setPaymentDetails(result.data);
                        } else {
                            throw err; // Re-lanzar si no hay forma de verificar
                        }
                    }
                }
                // Si solo tenemos payment_id, intentar directamente con �l
                else if (actualPaymentId) {
                    // console.log("Verificando pago solo con paymentId:", actualPaymentId);
                    const result = await paymentAPI.getPaymentStatus(actualPaymentId);
                    // console.log("Estado de pago:", result.data);
                    setPaymentDetails(result.data);
                }

                // Limpiar el carrito solo si el pago fue exitoso o está pendiente (in_process)
                if (status === "approved" || status === "in_process") {
                    // console.log("Limpiando carrito después de pago exitoso o pendiente");
                    try {
                        await clearCart();
                        // console.log("Carrito limpiado con éxito");

                        // Esperar un momento para asegurar que el backend haya procesado todo
                        await new Promise(resolve => setTimeout(resolve, 500));

                        // Si hay un ID de pago y está aprobado, actualizar localmente y en el backend
                        if (status === "approved" && preferenceId) {
                            // console.log("Pago aprobado, actualizando estado");

                            // Guardar información localmente
                            localStorage.setItem('lastSuccessfulPayment', JSON.stringify({
                                preferenceId,
                                paymentId: actualPaymentId,
                                status,
                                timestamp: new Date().toISOString()
                            }));

                            // Extraer el ID de orden del externalReference si existe
                            if (externalReference) {
                                // console.log("External reference recibido:", externalReference);
                                try {
                                    const orderIdMatch = externalReference.match(/order_(\d+)/);
                                    if (orderIdMatch && orderIdMatch[1]) {
                                        const orderId = parseInt(orderIdMatch[1], 10);
                                        // console.log("Actualizando estado de orden:", orderId);

                                        // Obtener detalles de la orden
                                        try {
                                            const orderDetailsResponse = await orderAPI.getOrder(orderId);
                                            setOrderDetails(orderDetailsResponse.data);
                                        } catch (orderErr) {
                                            // Error handling removed for production mode
                                        }

                                        // Actualizar explícitamente el estado de la orden
                                        try {
                                            // const updateResult = await orderAPI.updateOrderPaymentStatus(orderId, {
                                            await orderAPI.updateOrderPaymentStatus(orderId, {
                                                status: "PAID",
                                                paymentId: actualPaymentId || '',
                                                paymentMethod: paymentType || 'mercadopago'
                                            });
                                            // console.log("Orden actualizada con éxito:", updateResult.data);
                                        } catch (updateErr) {
                                            // Error handling removed for production mode
                                        }
                                    }
                                } catch (err) {
                                    // Warning removed for production mode
                                }
                            }
                        }
                    } catch (clearError) {
                        // Error handling removed for production mode
                        setError("Error al finalizar la compra. Por favor contacta a soporte.");
                    }
                } else if (status === "rejected" || status === "cancelled") {
                    // console.log("El pago fue rechazado o cancelado, NO se limpiará el carrito");
                    // No limpiamos el carrito para que el usuario pueda intentar nuevamente
                } else if (!status && !actualPaymentId && !preferenceId) {
                    // Si no hay información de pago, redirigir al inicio
                    // Warning removed for production mode
                    setError("No se encontró información del pago");
                    setTimeout(() => navigate("/"), 3000);
                }
            } catch (e: any) {
                // Error handling removed for production mode
                setError(e.message || "Error al verificar el pago");
            } finally {
                setVerifying(false);
            }
        };

        verifyPayment();
    }, []);

    return (
        <div className="min-h-screen flex items-center justify-center bg-green-50">
            <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
                {verifying ? (
                    <div className="flex flex-col items-center justify-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mb-4"></div>
                        <p className="text-gray-600">Verificando pago...</p>
                    </div>
                ) : (
                    <>
                        <div className="mb-6">
                            <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100">
                                <svg className="h-8 w-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900 mb-4">
                            {status === "approved" ? "¡Pago Exitoso!" :
                                status === "in_process" ? "Pago en Proceso" :
                                    status === "rejected" ? "Pago Rechazado" :
                                        "Estado del Pago"}
                        </h1>
                        {error && (
                            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                                <p>{error}</p>
                            </div>
                        )}
                        <div className="text-left bg-gray-50 rounded p-4 mb-6">
                            {orderDetails?.order_number && (
                                <p className="text-sm text-gray-600 mb-2"><strong>Número de Orden:</strong> {orderDetails.order_number}</p>
                            )}
                            {paymentDetails?.amount && (
                                <p className="text-sm text-gray-600 mb-2"><strong>Monto:</strong> ${paymentDetails.amount}</p>
                            )}
                            {paymentType && (
                                <p className="text-sm text-gray-600 mb-2"><strong>Método de pago:</strong> {paymentType}</p>
                            )}
                            {paymentDetails?.date_created && (
                                <p className="text-sm text-gray-600 mb-2"><strong>Fecha:</strong> {new Date(paymentDetails.date_created).toLocaleString()}</p>
                            )}
                        </div>
                        <div className="space-y-3">
                            {token && user ? (
                                <Link
                                    to="/orders"
                                    className="block w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors"
                                >
                                    Ver Mis Órdenes
                                </Link>
                            ) : (
                                <button
                                    onClick={() => navigate('/login', { state: { from: '/success', message: 'Inicia sesión para ver tus órdenes' } })}
                                    className="block w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors"
                                >
                                    Iniciar sesión para ver órdenes
                                </button>
                            )}
                            <Link
                                to="/"
                                className="block w-full bg-gray-200 text-gray-800 py-2 px-4 rounded-md hover:bg-gray-300 transition-colors"
                            >
                                Volver a la Tienda
                            </Link>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default Success;

