import { useEffect, useState } from 'react';
import { useCartStore } from '../stores/cartStore';
import { paymentAPI, orderAPI } from '../services/api';

const MercadoPagoBrick = () => {
    const { items, getTotal } = useCartStore();
    const [preferenceId, setPreferenceId] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    // Usamos un estado para controlar el flujo del proceso de la orden
    const [processingStep, setProcessingStep] = useState<'idle' | 'processing_order' | 'order_processed' | 'creating_preference'>('idle');
    // Estado para controlar si ya procesamos un pago
    const [paymentProcessed, setPaymentProcessed] = useState(false);

    // Comprobar si el carrito está vacío
    const cartIsEmpty = !items || items.length === 0 || getTotal() <= 0;

    const total = getTotal();
    const publicKey = import.meta.env.VITE_MP_PUBLIC_KEY as string;

    // Verificar si hay parámetros de pago en la URL al cargar el componente
    useEffect(() => {
        // console.log("useEffect de verificación de pago ejecutándose, paymentProcessed:", paymentProcessed);

        // Detectar si venimos de un pago de MercadoPago
        const handlePaymentResponse = async () => {
            // console.log("Ejecutando handlePaymentResponse");

            // Obtener parámetros de la URL
            const urlParams = new URLSearchParams(window.location.search);
            const preferenceId = urlParams.get('preference-id');
            const status = urlParams.get('status');

            // console.log("Parámetros de URL:", { preferenceId, status, paymentProcessed });

            if (preferenceId && status && !paymentProcessed) {
                // console.log("Pago detectado:", { preferenceId, status });

                try {
                    // Extraer todos los parámetros de la URL
                    const paymentId = urlParams.get('payment_id') || urlParams.get('collection_id') || undefined;
                    const merchantOrderId = urlParams.get('merchant_order_id') || undefined;
                    const paymentType = urlParams.get('payment_type') || undefined;
                    const externalReference = urlParams.get('external_reference') || undefined;

                    // console.log("Parámetros extraídos:", {
                    //     paymentId,
                    //     merchantOrderId,
                    //     paymentType,
                    //     externalReference
                    // });

                    // Notificar al backend sobre el resultado del pago
                    await paymentAPI.verifyPayment(preferenceId, {
                        status,
                        paymentId,
                        merchant_order_id: merchantOrderId,
                        payment_type: paymentType,
                        external_reference: externalReference,
                        fullUrl: window.location.href
                    });

                    // Marcar como procesado para evitar re-procesamiento
                    setPaymentProcessed(true);

                    // Si el pago fue APROBADO, limpiar el carrito del frontend
                    if (status === 'approved') {
                        // console.log('Pago aprobado - Limpiando carrito');
                        await useCartStore.getState().clearCart();
                        // console.log('Carrito limpiado');
                    }

                    // Redirigir según el estado del pago
                    if (status === 'approved') {
                        window.location.href = '/success';
                    } else if (status === 'rejected') {
                        window.location.href = '/failure';
                    } else if (status === 'pending') {
                        window.location.href = '/pending';
                    }
                } catch (error: any) {
                    // Error handling removed for production mode
                    setError("Error al verificar el pago");
                }
            }
        };

        // Ejecutar la función al cargar el componente
        handlePaymentResponse();
    }, [paymentProcessed]); // Agregar paymentProcessed como dependencia

    // Log cuando el componente se re-renderiza
    useEffect(() => {
        // console.log("Componente MercadoPagoBrick re-renderizado, paymentProcessed:", paymentProcessed);
    });
    useEffect(() => {
        const checkPendingPayments = async () => {
            try {
                // console.log("Verificando pagos pendientes...");
                await paymentAPI.checkPendingPayments();
                // console.log("✅ Verificación de pagos pendientes completada");
            } catch (error) {
                // Error handling removed for production mode
            }
        };

        // Verificar inmediatamente y luego cada hora
        checkPendingPayments();
        const interval = setInterval(checkPendingPayments, 60 * 60 * 1000); // 1 hora en milisegundos

        return () => clearInterval(interval); // Limpiar intervalo al desmontar
    }, []);

    // Log initialization
    useEffect(() => {
        // console.log("Inicializando MercadoPagoBrick - simplificado");
    }, []);

    const createPreference = async () => {
        try {
            setIsLoading(true);
            setError(null);

            // Verificar que el token esté disponible
            const token = localStorage.getItem('token');
            if (!token) {
                setError("Por favor inicia sesión para continuar con el pago");
                setIsLoading(false);
                return;
            }

            // Verificar que tenemos la clave pública de Mercado Pago
            if (!publicKey) {
                // Error handling removed for production mode
                setError("Error de configuración: falta la clave pública de Mercado Pago");
                setIsLoading(false);
                return;
            }

            // Primero procesamos la orden
            // console.log("⏳ Procesando orden...");

            // Verificar que haya elementos en el carrito
            if (!items || items.length === 0) {
                setError("El carrito está vacío. Agrega productos antes de continuar.");
                setIsLoading(false);
                return;
            }

            // Reunimos los datos para la orden
            const itemsPayload = items.map(i => ({ productId: i.id, quantity: i.quantity, price: i.price }));

            // DEBUG: Log de items que se envían
            // console.log('Items a enviar al backend:', itemsPayload);
            // console.log('Carrito local tiene', items.length, 'items');

            // Verificamos si hay un formulario de envío en sessionStorage
            const shippingFormData = sessionStorage.getItem('shippingFormData');
            let shippingAddress = "Dirección de envío no especificada";

            if (shippingFormData) {
                try {
                    const formData = JSON.parse(shippingFormData);
                    shippingAddress = `${formData.firstName} ${formData.lastName}, ${formData.email}, ${formData.phone}, ${formData.address}, ${formData.city}, ${formData.zipCode}, ${formData.country}`;
                } catch (e) {
                    // Error handling removed for production mode
                }
            }

            // Creamos la orden en el backend
            const orderData = {
                items: itemsPayload,
                shippingAddress: shippingAddress,
                paymentMethod: 'MERCADO_PAGO'
            };

            setProcessingStep('processing_order');
            let orderId: number;
            try {
                const orderResp = await orderAPI.createOrder(orderData);
                // Success logging removed for production mode
                setProcessingStep('order_processed');

                // Guardamos el ID de la orden para usarlo en la descripción
                orderId = orderResp.data.id;

            } catch (orderErr: any) {
                // Error handling removed for production mode
                setError(orderErr.message || "Error al procesar la orden");
                setProcessingStep('idle');
                throw orderErr; // Detenemos el proceso si falla la creación de la orden
            }

            // Crear la preferencia usando el endpoint que recibe orderId
            // El backend usará createPaymentPreference(Order order) que maneja correctamente
            // la construcción de items y descripciones
            setProcessingStep('creating_preference');

            const preferenceResponse = await paymentAPI.createPaymentPreference(orderId);

            if (!preferenceResponse.data?.id) {
                throw new Error("No se recibió un ID de preferencia válido");
            }
            // Guardamos información importante para recuperarla después de la redirección
            sessionStorage.setItem('mp_lastPreferenceId', preferenceResponse.data.id);

            // Guardamos el ID de la preferencia
            setPreferenceId(preferenceResponse.data.id);

            // Usamos la URL directamente de la respuesta de la API en lugar de construirla
            // Preferimos init_point sobre sandbox_init_point
            const mpUrl = preferenceResponse.data.init_point || `https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenceResponse.data.id}`;

            // Guardamos la URL en sessionStorage para recuperarla después si es necesario
            sessionStorage.setItem('mp_checkout_url', mpUrl);

            // Abrir la ventana de pago directamente
            window.open(mpUrl, '_blank');
        } catch (err: any) {
            // Error handling removed for production mode
            setError(err.message || "Error al iniciar el pago");
        } finally {
            setIsLoading(false);
        }
    };

    // Función simplificada para crear URL de pago y abrirla
    const openPaymentUrl = (prefId: string) => {
        // Creamos directamente la URL de MercadoPago con el preferenceId
        const baseUrl = "https://www.mercadopago.com.ar/checkout/v1/redirect";
        const mpUrl = `${baseUrl}?preference-id=${prefId}`;

        // console.log("Abriendo URL de MercadoPago:", mpUrl);

        // Guardamos esta URL para referencias futuras
        sessionStorage.setItem('mp_checkout_url', mpUrl);

        // Abrimos la ventana de pago
        window.open(mpUrl, '_blank');
    };

    return (
        <div className="mercadopago-container mt-6">
            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                    <p>{error}</p>
                </div>
            )}

            {cartIsEmpty && !isLoading && !preferenceId && (
                <div className="bg-yellow-50 border border-yellow-200 text-yellow-700 px-4 py-3 rounded mb-4">
                    <p className="text-center">Tu carrito está vacío. Agrega productos para continuar.</p>
                </div>
            )}

            {!preferenceId ? (
                <div className="space-y-3">
                    <button
                        onClick={createPreference}
                        disabled={isLoading || items.length === 0 || total <= 0}
                        className={`w-full py-3 px-4 rounded-md font-medium transition-colors ${isLoading || items.length === 0 || total <= 0
                            ? 'bg-gray-400 text-gray-100 cursor-not-allowed'
                            : 'bg-blue-600 text-white hover:bg-blue-700'
                            }`}
                    >
                        {isLoading ? (
                            <span className="flex items-center justify-center">
                                <svg className="animate-spin -ml-1 mr-3 h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                {processingStep === 'creating_preference' ? "Preparando pago..." :
                                    processingStep === 'order_processed' ? "Preparando pago..." : "Procesando orden..."}
                            </span>
                        ) : 'Procesar Orden'}
                    </button>
                </div>
            ) : (
                <div className="flex flex-col items-center">
                    <button
                        className="w-full py-3 px-4 bg-blue-500 text-white rounded-md hover:bg-blue-600 flex items-center justify-center"
                        onClick={() => preferenceId && openPaymentUrl(preferenceId)}
                    >
                        <img src="https://http2.mlstatic.com/frontend-assets/mp-web-payment/v1/images/mercado-pago-logo.svg" alt="Mercado Pago" className="h-6 mr-2" />
                        Pagar con Mercado Pago
                    </button>
                    <p className="text-xs text-gray-500 mt-2">Haz clic para reabrir la ventana de pago si la cerraste</p>
                </div>
            )}
        </div>
    );
};

export default MercadoPagoBrick;
