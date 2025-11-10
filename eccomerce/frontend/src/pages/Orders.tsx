import { useEffect, useState } from 'react';
import { orderAPI, paymentAPI } from '../services/api';
import { useUserStore } from '../stores/userStore';
import SignedImage from '../components/SignedImage';

interface OrderItem {
  id: number;
  product: {
    id: number;
    name: string;
    price: number;
    imageUrl?: string;
  };
  quantity: number;
  price: number;
  subtotal: number;
}

interface Order {
  id: number;
  orderNumber: string;
  totalAmount: number;
  status: string;
  paymentMethod?: string;
  createdAt: string;
  items: OrderItem[];
}

const Orders = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [retryingPayment, setRetryingPayment] = useState<number | null>(null);
  const { token, initializeFromStorage } = useUserStore();

  const fetchOrders = async () => {
    try {
      setError(null);
      // Asegurarnos de tener un token válido
      if (!token) {
        // console.log('⚠️ No hay token disponible para obtener pedidos');
        setError('Por favor inicia sesión para ver tus pedidos');
        setLoading(false);
        return;
      }

      // console.log('Obteniendo pedidos del usuario con token');
      const response = await orderAPI.getUserOrders();
      // console.log('Pedidos obtenidos:', response.data);
      setOrders(response.data);
    } catch (error: any) {
      // Error handling removed for production mode

      // Extraer mensaje de error detallado
      let errorMessage = 'Error al cargar los pedidos';

      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.response?.status === 401) {
        errorMessage = 'Debes iniciar sesión para ver tus pedidos.';
      } else if (error.response?.status === 403) {
        errorMessage = 'No tienes permisos para ver estos pedidos.';
      } else if (error.response?.status === 500) {
        errorMessage = 'Error del servidor. Inténtalo de nuevo más tarde.';
      } else if (error.message) {
        errorMessage = error.message;
      }

      setError(errorMessage);
      // Error is shown in the UI
    } finally {
      setLoading(false);
    }
  };

  const handleRetryPayment = async (order: Order) => {
    try {
      setRetryingPayment(order.id);
      // Crear nueva preferencia de pago para la orden existente
      const response = await paymentAPI.createPaymentPreference(order.id);

      // Redirigir al checkout con la preferencia de pago
      if (response.data?.init_point) {
        window.location.href = response.data.init_point;
      } else {
        setError('No se pudo obtener el link de pago. Inténtalo de nuevo.');
      }
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Error al reintentar el pago';
      setError(errorMessage);
    } finally {
      setRetryingPayment(null);
    }
  };

  useEffect(() => {
    // Intentar restaurar la autenticación al cargar el componente
    if (!token) {
      // console.log('Orders: Intentando restaurar la autenticación desde localStorage');
      initializeFromStorage();
    }
  }, [token, initializeFromStorage]);

  useEffect(() => {
    fetchOrders();
  }, [token]); // Dependencia de token para recargar cuando el usuario inicie sesión

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'pending':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
      case 'processing':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case 'shipped':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200';
      case 'delivered':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'cancelled':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  const getStatusText = (status: string) => {
    switch (status.toLowerCase()) {
      case 'pending':
        return 'Pendiente';
      case 'processing':
        return 'Procesando';
      case 'shipped':
        return 'Enviado';
      case 'delivered':
        return 'Entregado';
      case 'cancelled':
        return 'Cancelado';
      default:
        return status;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Cargando pedidos...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-3xl font-bold text-foreground">Mis pedidos</h1>
            <button
              onClick={() => {
                setLoading(true);
                fetchOrders();
              }}
              className="flex items-center space-x-2 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
              disabled={loading}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>{loading ? 'Actualizando...' : 'Actualizar'}</span>
            </button>
          </div>

          {error && (
            <div className="bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded mb-6">
              <strong>Error:</strong> {error}
            </div>
          )}

          {orders.length === 0 ? (
            <div className="bg-card rounded-lg shadow-md p-8 text-center border">
              <svg className="w-24 h-24 text-muted-foreground mx-auto mb-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <h2 className="text-2xl font-bold text-card-foreground mb-4">No tienes pedidos aún</h2>
              <p className="text-muted-foreground mb-6">Cuando realices tu primera compra, aparecerá aquí.</p>
              <a
                href="/"
                className="inline-block bg-primary hover:bg-primary/90 text-primary-foreground font-bold py-3 px-6 rounded-lg transition-colors"
              >
                Empezar a comprar
              </a>
            </div>
          ) : (
            <div className="space-y-6">
              {orders.map((order) => (
                <div key={order.id} className="bg-card rounded-lg shadow-md overflow-hidden border">
                  {/* Order Header */}
                  <div className="bg-muted px-6 py-4 border-b border-border">
                    <div className="flex flex-col md:flex-row md:items-center md:justify-between">
                      <div>
                        <h2 className="text-lg font-semibold text-card-foreground">
                          Pedido #{order.id}
                        </h2>
                        <p className="text-sm text-muted-foreground mt-1">
                          {new Date(order.createdAt).toLocaleDateString('es-AR', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                          })}
                        </p>
                      </div>
                      <div className="mt-4 md:mt-0 flex items-center space-x-4">
                        <span className={`inline-flex px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(order.status)}`}>
                          {getStatusText(order.status)}
                        </span>
                        <span className="text-lg font-bold text-card-foreground">
                          ${order.totalAmount.toFixed(2)}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Order Items */}
                  <div className="px-6 py-4">
                    <h3 className="text-sm font-medium text-card-foreground mb-4">Productos</h3>
                    <div className="space-y-4">
                      {order.items.map((item) => (
                        <div key={item.id} className="flex items-center space-x-4">
                          <div className="w-12 h-12 bg-muted rounded-lg overflow-hidden flex-shrink-0">
                            <SignedImage
                              src={item.product.imageUrl || '/placeholder-product.jpg'}
                              alt={item.product.name}
                              className="w-full h-full object-cover"
                            />
                          </div>
                          <div className="flex-1 min-w-0">
                            <h4 className="text-sm font-medium text-card-foreground truncate">
                              {item.product.name}
                            </h4>
                            <p className="text-sm text-muted-foreground">
                              Cantidad: {item.quantity} × ${item.price.toFixed(2)}
                            </p>
                          </div>
                          <div className="text-sm font-medium text-card-foreground">
                            ${(item.price * item.quantity).toFixed(2)}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Order Footer */}
                  <div className="bg-muted px-6 py-4 border-t border-border">
                    <div className="flex justify-between items-center">
                      <div className="text-sm text-muted-foreground">
                        Total del pedido: <span className="font-medium text-card-foreground">${order.totalAmount.toFixed(2)}</span>
                      </div>
                      <button
                        className="text-primary hover:text-primary/80 text-sm font-medium"
                        onClick={() => {
                          setSelectedOrder(order);
                          setShowDetailsModal(true);
                        }}
                      >
                        Ver detalles
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Modal de Detalles de Orden */}
      {showDetailsModal && selectedOrder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-card border border-border rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-2xl font-bold text-card-foreground">
                  Detalles de la Orden #{selectedOrder.id}
                </h3>
                <button
                  onClick={() => setShowDetailsModal(false)}
                  className="text-muted-foreground hover:text-card-foreground text-2xl"
                >
                  ×
                </button>
              </div>

              {/* Información básica de la orden */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                <div className="space-y-3">
                  <div>
                    <span className="font-medium text-muted-foreground">Estado:</span>
                    <span className={`ml-2 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(selectedOrder.status)}`}>
                      {selectedOrder.status}
                    </span>
                  </div>
                  <div>
                    <span className="font-medium text-muted-foreground">Fecha de creación:</span>
                    <span className="ml-2 text-card-foreground">
                      {new Date(selectedOrder.createdAt).toLocaleDateString('es-AR', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </span>
                  </div>
                </div>
                <div className="space-y-3">
                  <div>
                    <span className="font-medium text-muted-foreground">Total de productos:</span>
                    <span className="ml-2 text-card-foreground">
                      {selectedOrder.items?.length || 0} productos
                    </span>
                  </div>
                  <div>
                    <span className="font-medium text-muted-foreground">Total del pedido:</span>
                    <span className="ml-2 text-xl font-bold text-primary">
                      ${selectedOrder.totalAmount?.toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Lista de productos */}
              <div className="border-t border-border pt-6">
                <h4 className="text-lg font-semibold text-card-foreground mb-4">Productos en la orden</h4>
                <div className="space-y-4">
                  {selectedOrder.items?.map((item, index) => (
                    <div key={index} className="flex items-center space-x-4 p-4 border border-border rounded-lg bg-muted/50">
                      {/* Imagen del producto */}
                      <div className="w-16 h-16 bg-muted rounded-md flex items-center justify-center">
                        {item.product?.imageUrl ? (
                          <img
                            src={item.product.imageUrl}
                            alt={item.product.name}
                            className="w-full h-full object-cover rounded-md"
                          />
                        ) : (
                          <div className="text-muted-foreground text-xs">Sin imagen</div>
                        )}
                      </div>

                      {/* Información del producto */}
                      <div className="flex-1">
                        <h5 className="font-medium text-card-foreground">{item.product?.name}</h5>
                        <p className="text-sm text-muted-foreground">
                          Precio unitario: ${item.price?.toFixed(2)}
                        </p>
                      </div>

                      {/* Cantidad y subtotal */}
                      <div className="text-right">
                        <p className="text-sm text-muted-foreground">Cantidad: {item.quantity}</p>
                        <p className="font-medium text-card-foreground">
                          Subtotal: ${(item.quantity * item.price).toFixed(2)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Botones de acción */}
              <div className="flex justify-end space-x-3 mt-6 pt-6 border-t border-border">
                {selectedOrder.status.toLowerCase() === 'pending' && (
                  <button
                    onClick={() => {
                      setShowDetailsModal(false);
                      handleRetryPayment(selectedOrder);
                    }}
                    disabled={retryingPayment === selectedOrder.id}
                    className="px-4 py-2 bg-orange-600 text-white rounded-md hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    {retryingPayment === selectedOrder.id ? 'Procesando...' : 'Reintentar Pago'}
                  </button>
                )}
                <button
                  onClick={() => setShowDetailsModal(false)}
                  className="px-4 py-2 text-muted-foreground hover:text-card-foreground border border-border rounded-md hover:bg-muted transition-colors"
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Orders;
