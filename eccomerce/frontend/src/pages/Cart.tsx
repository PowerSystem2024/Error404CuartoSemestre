import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useCartStore } from '../stores/cartStore';
import { useUserStore } from '../stores/userStore';
import { useNotificationStore } from '../stores/notificationStore';
import ScrollMemory from '../components/ScrollMemory';
import { AlertCircle, CheckCircle, LogIn, Eye, EyeOff } from 'lucide-react';

const Cart = () => {
  const { items, removeItem, updateQuantity, getTotal, clearCart, loadCart, transferCartToBackend } = useCartStore();
  const { user, token, login, register } = useUserStore();
  const { addNotification } = useNotificationStore();
  const total = getTotal();
  const navigate = useNavigate();
  const [notification, setNotification] = useState<{ type: 'error' | 'success'; message: string } | null>(null);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [authForm, setAuthForm] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);

  useEffect(() => {
    loadCart();
  }, [loadCart]);

  // console.log(' Cart items:', items);

  const handleUpdateQuantity = async (id: number, quantity: number) => {
    try {
      await updateQuantity(id, quantity);
    } catch (error) {
      // Error handling removed for production mode
    }
  };

  const handleRemoveItem = async (id: number) => {
    try {
      await removeItem(id);
    } catch (error) {
      // Error handling removed for production mode
    }
  };

  const handleClearCart = async () => {
    try {
      await clearCart();
      setNotification({ type: 'success', message: 'Carrito vaciado exitosamente' });
      setTimeout(() => setNotification(null), 3000);
    } catch (error) {
      // Error handling removed for production mode
      setNotification({ type: 'error', message: 'Error al vaciar el carrito' });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthLoading(true);

    try {
      if (authMode === 'login') {
        const result = await login(authForm.email, authForm.password);
        if (result.success) {
          // Transfer cart from localStorage to backend
          await transferCartToBackend();
          addNotification({
            type: 'success',
            message: '¡Bienvenido! Tu carrito ha sido guardado.',
            duration: 4000
          });
          setShowAuthModal(false);
          // Proceed to checkout
          navigate('/checkout');
        } else {
          addNotification({
            type: 'error',
            message: result.error || 'Error al iniciar sesión',
            duration: 4000
          });
        }
      } else {
        const result = await register(authForm.email, authForm.email, authForm.password);
        if (result.success) {
          // After successful registration, the user needs to login
          const loginResult = await login(authForm.email, authForm.password);
          if (loginResult.success) {
            // Transfer cart from localStorage to backend
            await transferCartToBackend();
            addNotification({
              type: 'success',
              message: '¡Cuenta creada! Tu carrito ha sido guardado.',
              duration: 4000
            });
            setShowAuthModal(false);
            // Proceed to checkout
            navigate('/checkout');
          } else {
            addNotification({
              type: 'error',
              message: 'Cuenta creada pero error al iniciar sesión automáticamente',
              duration: 4000
            });
          }
        } else {
          addNotification({
            type: 'error',
            message: result.error || 'Error al crear la cuenta',
            duration: 4000
          });
        }
      }
    } catch (error: any) {
      addNotification({
        type: 'error',
        message: error.response?.data?.message || 'Error en la autenticación',
        duration: 4000
      });
    } finally {
      setAuthLoading(false);
    }
  };

  const handleProceedToCheckout = () => {
    // Verificar si el usuario está logueado
    if (!token || !user) {
      setShowAuthModal(true);
      return;
    }

    // Validar que el carrito no esté vacío
    if (items.length === 0) {
      setNotification({ type: 'error', message: 'Tu carrito está vacío. Agrega productos antes de proceder al pago.' });
      setTimeout(() => setNotification(null), 4000);
      return;
    }

    // Validar que el total no sea cero
    if (total <= 0) {
      setNotification({ type: 'error', message: 'El total del carrito debe ser mayor a cero. Revisa los precios de los productos.' });
      setTimeout(() => setNotification(null), 4000);
      return;
    }

    // Validar que todos los items tengan precio válido
    const invalidItems = items.filter(item => !item.price || item.price <= 0);
    if (invalidItems.length > 0) {
      setNotification({ type: 'error', message: 'Algunos productos en tu carrito tienen precios inválidos. Por favor, contacta al soporte.' });
      setTimeout(() => setNotification(null), 4000);
      return;
    }

    // Si todas las validaciones pasan, navegar al checkout
    navigate('/checkout');
  };

  if (items.length === 0) {
    return (
      <ScrollMemory>
        <div className='min-h-screen bg-background py-8'>
          <div className='container mx-auto px-4'>
            <div className='max-w-2xl mx-auto text-center'>
              <div className='bg-card rounded-lg shadow-md p-8 border'>
                <svg className='w-24 h-24 text-muted-foreground mx-auto mb-6' fill='none' stroke='currentColor' viewBox='0 0 24 24'>
                  <path strokeLinecap='round' strokeLinejoin='round' strokeWidth={1} d='M3 3h2l.4 2M7 13h10l4-8H5.4m0 0L7 13m0 0l-1.1 5H19M7 13l-1.1 5M7 13h10m0 0v8a2 2 0 01-2 2H9a2 2 0 01-2-2v-8z' />
                </svg>
                <h1 className='text-2xl font-bold text-foreground mb-4'>Tu carrito está vacío</h1>
                <p className='text-muted-foreground mb-6'>¡Agrega algunos productos para comenzar!</p>
                <Link
                  to='/'
                  className='inline-block bg-primary hover:bg-primary/90 text-primary-foreground font-bold py-3 px-6 rounded-lg transition-colors'
                >
                  Continuar comprando
                </Link>
              </div>
            </div>
          </div>
        </div>
      </ScrollMemory>
    );
  }

  return (
    <ScrollMemory>
      <div className='min-h-screen bg-background py-8'>
        <div className='container mx-auto px-4'>
          <div className='max-w-6xl mx-auto'>
            <h1 className='text-3xl font-bold text-foreground mb-8'>Carrito de compras</h1>

            {/* Notification */}
            {notification && (
              <div className={`mb-6 p-4 rounded-lg border flex items-center space-x-3 ${notification.type === 'error'
                ? 'bg-red-50 border-red-200 text-red-800'
                : 'bg-green-50 border-green-200 text-green-800'
                }`}>
                {notification.type === 'error' ? (
                  <AlertCircle className='w-5 h-5 flex-shrink-0' />
                ) : (
                  <CheckCircle className='w-5 h-5 flex-shrink-0' />
                )}
                <p className='text-sm font-medium'>{notification.message}</p>
              </div>
            )}

            <div className='grid grid-cols-1 lg:grid-cols-3 gap-8'>
              {/* Cart Items */}
              <div className='lg:col-span-2'>
                <div className='bg-card rounded-lg shadow-md border'>
                  <div className='p-6 border-b border-border'>
                    <h2 className='text-xl font-semibold text-card-foreground'>Productos ({items.length})</h2>
                  </div>

                  <div className='divide-y divide-border'>
                    {items.map((item) => {
                      // console.log(' Item image:', item.productImage);
                      return (
                        <div key={item.id} className='p-6'>
                          <div className='flex items-center space-x-4'>
                            {/* Product Image */}
                            <div className='w-20 h-20 bg-muted rounded-lg overflow-hidden flex-shrink-0'>
                              <img
                                src={item.productImage || '/placeholder-product.jpg'}
                                alt={item.name}
                                className='w-full h-full object-cover'
                                onError={(e) => {
                                  const target = e.target as HTMLImageElement;
                                  target.src = '/placeholder-product.jpg';
                                }}
                              />
                              {/* Debug: mostrar el valor de productImage */}
                              <div className='text-xs text-red-500 mt-1'>
                                Debug: {item.productImage ? 'Tiene imagen' : 'Sin imagen'} - {item.productImage}
                              </div>
                            </div>

                            {/* Product Details */}
                            <div className='flex-1 min-w-0'>
                              <h3 className='text-lg font-medium text-foreground truncate'>{item.name}</h3>
                              <p className='text-sm text-muted-foreground mt-1'>${item.price} cada uno</p>
                            </div>

                            {/* Quantity Controls */}
                            <div className='flex items-center space-x-3'>
                              <button
                                onClick={() => handleUpdateQuantity(item.id, Math.max(1, item.quantity - 1))}
                                className='w-8 h-8 rounded-full border border-border flex items-center justify-center hover:bg-accent'
                              >
                                -
                              </button>
                              <span className='w-8 text-center font-medium'>{item.quantity}</span>
                              <button
                                onClick={() => handleUpdateQuantity(item.id, item.quantity + 1)}
                                className='w-8 h-8 rounded-full border border-border flex items-center justify-center hover:bg-accent'
                              >
                                +
                              </button>
                            </div>

                            {/* Price and Remove */}
                            <div className='text-right'>
                              <p className='text-lg font-bold text-foreground'>${(item.price * item.quantity).toFixed(2)}</p>
                              <button
                                onClick={() => handleRemoveItem(item.id)}
                                className='text-destructive hover:text-destructive/80 text-sm mt-1'
                              >
                                Eliminar
                              </button>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  <div className='p-6 border-t border-border'>
                    <button
                      onClick={handleClearCart}
                      className='text-destructive hover:text-destructive/80 font-medium'
                    >
                      Vaciar carrito
                    </button>
                  </div>
                </div>
              </div>

              {/* Order Summary */}
              <div className='lg:col-span-1'>
                <div className='bg-card rounded-lg shadow-md p-6 sticky top-4 border'>
                  <h2 className='text-xl font-semibold text-card-foreground mb-6'>Resumen del pedido</h2>

                  <div className='space-y-4'>
                    <div className='flex justify-between'>
                      <span className='text-muted-foreground'>Subtotal ({items.length} productos)</span>
                      <span className='font-medium'>${total.toFixed(2)}</span>
                    </div>

                    <div className='flex justify-between'>
                      <span className='text-muted-foreground'>Envío</span>
                      <span className='font-medium text-green-600'>Gratis</span>
                    </div>

                    <hr className='my-4 border-border' />

                    <div className='flex justify-between text-lg font-bold'>
                      <span>Total</span>
                      <span>${total.toFixed(2)}</span>
                    </div>
                  </div>

                  <button
                    onClick={handleProceedToCheckout}
                    className='w-full bg-primary hover:bg-primary/90 text-primary-foreground font-bold py-3 px-4 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed'
                    disabled={items.length === 0 || total <= 0}
                  >
                    Proceder al pago
                  </button>

                  <Link
                    to='/'
                    className='w-full bg-secondary hover:bg-secondary/80 text-secondary-foreground font-medium py-3 px-4 rounded-lg transition-colors block text-center mt-3'
                  >
                    Continuar comprando
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Authentication Modal */}
      {showAuthModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-card rounded-lg shadow-xl max-w-md w-full mx-4 border">
            <div className="p-6">
              {/* Header */}
              <div className="text-center mb-6">
                <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                  <LogIn className="w-8 h-8 text-primary" />
                </div>
                <h2 className="text-xl font-bold text-card-foreground mb-2">
                  {authMode === 'login' ? 'Inicia sesión' : 'Crear cuenta'}
                </h2>
                <p className="text-muted-foreground text-sm">
                  {authMode === 'login'
                    ? 'Ingresa tus datos para continuar con tu compra'
                    : 'Crea tu cuenta para guardar tu carrito y proceder al pago'
                  }
                </p>
              </div>

              {/* Auth Mode Tabs */}
              <div className="flex mb-6 bg-muted rounded-lg p-1">
                <button
                  onClick={() => setAuthMode('login')}
                  className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-colors ${authMode === 'login'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                    }`}
                >
                  Iniciar sesión
                </button>
                <button
                  onClick={() => setAuthMode('register')}
                  className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-colors ${authMode === 'register'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                    }`}
                >
                  Registrarse
                </button>
              </div>

              {/* Auth Form */}
              <form onSubmit={handleAuthSubmit} className="space-y-4">
                {authMode === 'register' && (
                  <>
                    <div>
                      <label htmlFor="firstName" className="block text-sm font-medium text-card-foreground mb-1">
                        Nombre
                      </label>
                      <input
                        id="firstName"
                        type="text"
                        required
                        value={authForm.firstName}
                        onChange={(e) => setAuthForm(prev => ({ ...prev, firstName: e.target.value }))}
                        className="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                        placeholder="Tu nombre"
                      />
                    </div>
                    <div>
                      <label htmlFor="lastName" className="block text-sm font-medium text-card-foreground mb-1">
                        Apellido
                      </label>
                      <input
                        id="lastName"
                        type="text"
                        required
                        value={authForm.lastName}
                        onChange={(e) => setAuthForm(prev => ({ ...prev, lastName: e.target.value }))}
                        className="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                        placeholder="Tu apellido"
                      />
                    </div>
                  </>
                )}

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-card-foreground mb-1">
                    Email
                  </label>
                  <input
                    id="email"
                    type="email"
                    required
                    value={authForm.email}
                    onChange={(e) => setAuthForm(prev => ({ ...prev, email: e.target.value }))}
                    className="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                    placeholder="tu@email.com"
                  />
                </div>

                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-card-foreground mb-1">
                    Contraseña
                  </label>
                  <div className="relative">
                    <input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      required
                      value={authForm.password}
                      onChange={(e) => setAuthForm(prev => ({ ...prev, password: e.target.value }))}
                      className="w-full px-3 py-2 pr-10 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                      placeholder="Tu contraseña"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute inset-y-0 right-0 pr-3 flex items-center"
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Eye className="h-4 w-4 text-muted-foreground" />
                      )}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={authLoading}
                  className="w-full bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed text-primary-foreground font-bold py-3 px-4 rounded-lg transition-colors flex items-center justify-center"
                >
                  {authLoading ? (
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  ) : null}
                  {authMode === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}
                </button>
              </form>

              {/* Close Button */}
              <button
                onClick={() => setShowAuthModal(false)}
                className="w-full mt-4 text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                Continuar explorando sin cuenta
              </button>
            </div>
          </div>
        </div>
      )}
    </ScrollMemory>
  );
};

export default Cart;

