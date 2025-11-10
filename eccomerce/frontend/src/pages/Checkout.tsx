import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCartStore } from '../stores/cartStore';
import { useProfileStore } from '../stores/profileStore';
import { useAddressStore } from '../stores/addressStore';
// Importaciones no utilizadas comentadas
// import { orderAPI, paymentAPI } from '../services/api';
import MercadoPagoBrick from '../components/MercadoPagoBrick';
import { AlertCircle, MapPin, Plus } from 'lucide-react';



const Checkout = () => {
  const { items, getTotal } = useCartStore();
  const { loadProfile } = useProfileStore();
  const { shippingAddresses, billingAddresses, loadShippingAddresses, loadBillingAddresses } = useAddressStore();
  const navigate = useNavigate();
  const [notification, setNotification] = useState<{ type: 'error' | 'warning'; message: string } | null>(null);
  const [selectedShippingAddress, setSelectedShippingAddress] = useState<number | null>(null);
  const [selectedBillingAddress, setSelectedBillingAddress] = useState<number | null>(null);
  const [showNewAddressForm, setShowNewAddressForm] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    city: '',
    zipCode: '',
    country: 'Argentina',
  });
  // Estados utilizados anteriormente con la función handleCheckout
  // Ahora se mantienen solo para uso en la interfaz de usuario
  const [error] = useState<string | null>(null);
  // const [isCreatingPref, setIsCreatingPref] = useState(false);

  const total = getTotal();

  // Validar carrito antes de mostrar checkout
  useEffect(() => {
    if (items.length === 0) {
      setNotification({ type: 'warning', message: 'Tu carrito está vacío. Agrega productos antes de proceder al pago.' });
      setTimeout(() => navigate('/cart'), 2000);
      return;
    }

    if (total <= 0) {
      setNotification({ type: 'warning', message: 'El total del carrito debe ser mayor a cero. Revisa los precios de los productos.' });
      setTimeout(() => navigate('/cart'), 2000);
      return;
    }

    // Validar que todos los items tengan precio válido
    const invalidItems = items.filter(item => !item.price || item.price <= 0);
    if (invalidItems.length > 0) {
      setNotification({ type: 'warning', message: 'Algunos productos en tu carrito tienen precios inválidos. Por favor, contacta al soporte.' });
      setTimeout(() => navigate('/cart'), 2000);
      return;
    }
  }, [items, total, navigate]);

  // Load user profile on component mount
  useEffect(() => {
    loadProfile();
    loadShippingAddresses();
    loadBillingAddresses();
  }, [loadProfile, loadShippingAddresses, loadBillingAddresses]);

  // Auto-select default addresses when addresses are loaded
  useEffect(() => {
    if (shippingAddresses.length > 0 && !selectedShippingAddress) {
      const defaultShipping = shippingAddresses.find(addr => addr.isDefault);
      if (defaultShipping) {
        setSelectedShippingAddress(defaultShipping.id);
        updateFormWithAddress(defaultShipping);
      }
    }
  }, [shippingAddresses, selectedShippingAddress]);

  useEffect(() => {
    if (billingAddresses.length > 0 && !selectedBillingAddress) {
      const defaultBilling = billingAddresses.find(addr => addr.isDefault);
      if (defaultBilling) {
        setSelectedBillingAddress(defaultBilling.id);
      }
    }
  }, [billingAddresses, selectedBillingAddress]);

  const updateFormWithAddress = (address: any) => {
    setFormData(prev => ({
      ...prev,
      firstName: address.firstName || prev.firstName,
      lastName: address.lastName || prev.lastName,
      address: address.address || prev.address,
      city: address.city || prev.city,
      zipCode: address.zipCode || prev.zipCode,
      country: address.country || prev.country,
    }));
  };

  const handleAddressSelect = (addressId: number, type: 'shipping' | 'billing') => {
    const addresses = type === 'shipping' ? shippingAddresses : billingAddresses;
    const address = addresses.find(addr => addr.id === addressId);

    if (address) {
      if (type === 'shipping') {
        setSelectedShippingAddress(addressId);
        updateFormWithAddress(address);
      } else {
        setSelectedBillingAddress(addressId);
      }
    }
  };


  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => {
      const newFormData = { ...prev, [name]: value };
      // Guardamos los datos del formulario en sessionStorage para que MercadoPagoBrick pueda usarlos
      sessionStorage.setItem('shippingFormData', JSON.stringify(newFormData));
      return newFormData;
    });
  };

  if (items.length === 0) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-foreground mb-4">Tu carrito está vacío</h1>
          <p className="text-muted-foreground mb-6">Agrega productos antes de proceder al pago.</p>
          <Link
            to="/"
            className="inline-block bg-primary hover:bg-primary/90 text-primary-foreground font-bold py-3 px-6 rounded-lg transition-colors"
          >
            Ir a la tienda
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4">
        <div className="max-w-6xl mx-auto">
          <h1 className="text-3xl font-bold text-foreground mb-8">Finalizar compra</h1>

          {/* Notification */}
          {notification && (
            <div className={`mb-6 p-4 rounded-lg border flex items-center space-x-3 ${notification.type === 'error'
              ? 'bg-red-50 border-red-200 text-red-800'
              : 'bg-yellow-50 border-yellow-200 text-yellow-800'
              }`}>
              <AlertCircle className='w-5 h-5 flex-shrink-0' />
              <p className='text-sm font-medium'>{notification.message}</p>
            </div>
          )}

          {error && (
            <div className="bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded mb-6">
              <strong>Error:</strong> {error}
            </div>
          )}

          <form className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Shipping Information */}
            <div className="space-y-6">
              <div className="bg-card rounded-lg shadow-md p-6 border">
                <h2 className="text-xl font-semibold text-card-foreground mb-6">Información de envío</h2>

                {/* Shipping Address Selection */}
                {shippingAddresses.length > 0 && (
                  <div className="mb-6">
                    <h3 className="text-lg font-medium text-card-foreground mb-4">Seleccionar dirección de envío</h3>
                    <div className="space-y-3">
                      {shippingAddresses.map((address) => (
                        <div
                          key={address.id}
                          className={`border rounded-lg p-4 cursor-pointer transition-colors ${selectedShippingAddress === address.id
                            ? 'border-primary bg-primary/5'
                            : 'border-border hover:border-primary/50'
                            }`}
                          onClick={() => handleAddressSelect(address.id, 'shipping')}
                        >
                          <div className="flex items-start justify-between">
                            <div className="flex items-start space-x-3">
                              <MapPin className="w-5 h-5 text-muted-foreground mt-0.5 flex-shrink-0" />
                              <div>
                                <p className="font-medium text-card-foreground">
                                  {address.firstName} {address.lastName}
                                </p>
                                <p className="text-sm text-muted-foreground">
                                  {address.address}
                                  {address.address2 && `, ${address.address2}`}
                                </p>
                                <p className="text-sm text-muted-foreground">
                                  {address.city}, {address.zipCode}
                                </p>
                                <p className="text-sm text-muted-foreground">
                                  {address.country}
                                </p>
                                {address.phone && (
                                  <p className="text-sm text-muted-foreground">
                                    Tel: {address.phone}
                                  </p>
                                )}
                              </div>
                            </div>
                            {address.isDefault && (
                              <span className="text-xs bg-primary/10 text-primary px-2 py-1 rounded-full">
                                Predeterminada
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowNewAddressForm(!showNewAddressForm)}
                      className="mt-4 flex items-center space-x-2 text-primary hover:text-primary/80 transition-colors"
                    >
                      <Plus className="w-4 h-4" />
                      <span className="text-sm font-medium">
                        {showNewAddressForm ? 'Cancelar' : 'Agregar nueva dirección'}
                      </span>
                    </button>
                  </div>
                )}

                {/* Manual Address Form */}
                {(!shippingAddresses.length || showNewAddressForm) && (
                  <>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Nombre *
                        </label>
                        <input
                          type="text"
                          name="firstName"
                          value={formData.firstName}
                          onChange={handleInputChange}
                          className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          required />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Apellido *
                        </label>
                        <input
                          type="text"
                          name="lastName"
                          value={formData.lastName}
                          onChange={handleInputChange}
                          className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          required />
                      </div>
                    </div>

                    <div className="mt-4">
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Email *
                      </label>
                      <input
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        required />
                    </div>

                    <div className="mt-4">
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Teléfono *
                      </label>
                      <input
                        type="tel"
                        name="phone"
                        value={formData.phone}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        required />
                    </div>

                    <div className="mt-4">
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Dirección *
                      </label>
                      <input
                        type="text"
                        name="address"
                        value={formData.address}
                        onChange={handleInputChange}
                        placeholder="Calle y número"
                        className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        required />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Ciudad *
                        </label>
                        <input
                          type="text"
                          name="city"
                          value={formData.city}
                          onChange={handleInputChange}
                          className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          required />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Código Postal *
                        </label>
                        <input
                          type="text"
                          name="zipCode"
                          value={formData.zipCode}
                          onChange={handleInputChange}
                          className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          required />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          País
                        </label>
                        <select
                          name="country"
                          value={formData.country}
                          onChange={handleInputChange}
                          className="w-full px-3 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        >
                          <option value="Argentina">Argentina</option>
                          <option value="Chile">Chile</option>
                          <option value="Uruguay">Uruguay</option>
                        </select>
                      </div>
                    </div>
                  </>
                )}
              </div>

              {/* Billing Address Selection */}
              {billingAddresses.length > 0 && (
                <div className="bg-card rounded-lg shadow-md p-6 border">
                  <h2 className="text-xl font-semibold text-card-foreground mb-6">Dirección de facturación</h2>
                  <div className="space-y-3">
                    {billingAddresses.map((address) => (
                      <div
                        key={address.id}
                        className={`border rounded-lg p-4 cursor-pointer transition-colors ${selectedBillingAddress === address.id
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:border-primary/50'
                          }`}
                        onClick={() => handleAddressSelect(address.id, 'billing')}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex items-start space-x-3">
                            <MapPin className="w-5 h-5 text-muted-foreground mt-0.5 flex-shrink-0" />
                            <div>
                              <p className="font-medium text-card-foreground">
                                {address.firstName} {address.lastName}
                              </p>
                              <p className="text-sm text-muted-foreground">
                                {address.address}
                                {address.address2 && `, ${address.address2}`}
                              </p>
                              <p className="text-sm text-muted-foreground">
                                {address.city}, {address.zipCode}
                              </p>
                              <p className="text-sm text-muted-foreground">
                                {address.country}
                              </p>
                              {address.phone && (
                                <p className="text-sm text-muted-foreground">
                                  Tel: {address.phone}
                                </p>
                              )}
                            </div>
                          </div>
                          {address.isDefault && (
                            <span className="text-xs bg-primary/10 text-primary px-2 py-1 rounded-full">
                              Predeterminada
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                  <p className="text-sm text-muted-foreground mt-4">
                    {selectedBillingAddress
                      ? 'Dirección de facturación seleccionada'
                      : 'Selecciona una dirección de facturación'
                    }
                  </p>
                </div>
              )}
            </div>

            {/* Order Summary */}
            <div className="space-y-6">
              <div className="bg-card rounded-lg shadow-md p-6 border">
                <h2 className="text-xl font-semibold text-card-foreground mb-6">Resumen del pedido</h2>

                <div className="space-y-4 mb-6">
                  {items.map((item) => (
                    <div key={item.id} className="flex items-center space-x-4">
                      <div className="w-12 h-12 bg-muted rounded-lg overflow-hidden flex-shrink-0">
                        <img
                          src={item.productImage || '/placeholder-product.jpg'}
                          alt={item.name}
                          className="w-full h-full object-cover" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="text-sm font-medium text-foreground truncate">{item.name}</h3>
                        <p className="text-sm text-muted-foreground">Cantidad: {item.quantity}</p>
                      </div>
                      <div className="text-sm font-medium text-foreground">
                        ${(item.price * item.quantity).toFixed(2)}
                      </div>
                    </div>
                  ))}
                </div>

                <hr className="my-4 border-border" />

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Subtotal</span>
                    <span>${total.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Envío</span>
                    <span className="text-green-600">Gratis</span>
                  </div>
                  <hr className="my-2 border-border" />
                  <div className="flex justify-between text-lg font-bold">
                    <span>Total</span>
                    <span>${total.toFixed(2)}</span>
                  </div>
                </div>



                <div className="mt-6">
                  <div className="text-center">
                    <div className="mb-6">
                      {/* Guardamos la información del formulario en sessionStorage para que MercadoPagoBrick pueda accederla */}
                      {(() => {
                        // Solo guardar si todos los campos requeridos están completos
                        const { firstName, lastName, email, phone, address, city, zipCode } = formData;
                        if (firstName && lastName && email && phone && address && city && zipCode) {
                          sessionStorage.setItem('shippingFormData', JSON.stringify(formData));
                        }
                        return <MercadoPagoBrick />;
                      })()}
                    </div>

                    <hr className="my-6 border-gray-200" />

                    <div className="mt-4 text-center">
                      <Link to="/cart" className="text-primary hover:text-primary/80 text-sm">
                        ← Volver al carrito
                      </Link>
                    </div>
                  </div>
                </div>
              </div>

              {/* Payment Info */}
              <div className="bg-accent rounded-lg p-4 border">
                <div className="flex items-center space-x-3">
                  <svg className="w-6 h-6 text-primary" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                  </svg>
                  <div>
                    <h3 className="text-sm font-medium text-accent-foreground">Pago seguro</h3>
                    <p className="text-sm text-muted-foreground">Procesamos tu pago de forma segura con MercadoPago</p>
                  </div>
                </div>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Checkout;
