import { useEffect, useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { useCartStore } from '../stores/cartStore';
import { useUserStore } from '../stores/userStore';
import { orderAPI } from '../services/api';

const Failure = () => {
    const [searchParams] = useSearchParams();
    const { clearCart } = useCartStore();
    const { token, user, initializeFromStorage } = useUserStore();
    const navigate = useNavigate();
    const [orderDetails, setOrderDetails] = useState<any>(null);
    // const [paymentDetails, setPaymentDetails] = useState<any>(null);
    // const [verifying, setVerifying] = useState(true);
    // const [errorMessage, setErrorMessage] = useState<string>('');

    // Intentar restaurar la autenticación al cargar el componente
    useEffect(() => {
        if (!token || !user) {
            // console.log('Failure: Intentando restaurar la autenticación desde localStorage');
            initializeFromStorage();
        } else {
            // console.log('Failure: Usuario autenticado:', user.name);
        }
    }, [token, user, initializeFromStorage]);

    // Extraer parámetros de la URL
    const merchantOrderId = searchParams.get('merchant_order_id');
    const paymentType = searchParams.get('payment_type');
    const errorCode = searchParams.get('error');

    useEffect(() => {
        // Obtener detalles de la orden si disponible
        if (merchantOrderId) {
            try {
                const orderId = parseInt(merchantOrderId, 10);
                orderAPI.getOrder(orderId).then(response => {
                    setOrderDetails(response.data);
                }).catch(() => {
                    // Error handling removed for production mode
                });
            } catch (err) {
                // Error handling removed for production mode
            }
        }
    }, [merchantOrderId, paymentType, errorCode]);

    // Limpiar el carrito cuando llegamos a la página de fallo
    useEffect(() => {
        clearCart().catch(() => {
            // Error handling removed for production mode
        });
    }, [clearCart]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
                <div className="mb-6">
                    <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-orange-100">
                        <svg className="h-8 w-8 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                </div>
                <h1 className="text-2xl font-bold text-gray-900 mb-4">Pago Pendiente de Verificación</h1>
                <div className="text-left bg-gray-50 rounded p-4 mb-6">
                    {orderDetails?.order_number && (
                        <p className="text-sm text-gray-600 mb-2"><strong>Número de Orden:</strong> {orderDetails.order_number}</p>
                    )}
                    <p className="text-sm text-gray-600 mb-2"><strong>Estado:</strong> Rechazado</p>
                    {paymentType && (
                        <p className="text-sm text-gray-600 mb-2"><strong>Método de pago:</strong> {paymentType}</p>
                    )}
                    {errorCode && (
                        <p className="text-sm text-gray-600 mb-2"><strong>Código de error:</strong> {errorCode}</p>
                    )}
                </div>
                <p className="text-gray-600 mb-6">
                    Tu pago está siendo verificado. Si no se completa automáticamente,
                    puedes intentar nuevamente o verificar el estado en tus órdenes.
                </p>

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
                            onClick={() => navigate('/login', { state: { from: '/failure', message: 'Inicia sesión para ver tus órdenes' } })}
                            className="block w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors"
                        >
                            Iniciar sesión para ver órdenes
                        </button>
                    )}
                    <Link
                        to="/checkout"
                        className="block w-full bg-orange-500 text-white py-2 px-4 rounded-md hover:bg-orange-600 transition-colors"
                    >
                        Intentar Pago Nuevamente
                    </Link>
                    <Link
                        to="/"
                        className="block w-full bg-gray-200 text-gray-800 py-2 px-4 rounded-md hover:bg-gray-300 transition-colors"
                    >
                        Volver a la Tienda
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default Failure;
