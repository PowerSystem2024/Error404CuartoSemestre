import { useEffect, useState } from 'react';
import { useAddressStore } from '../stores/addressStore';
import { AddressResponse } from '../services/api';
import AddressForm from './AddressForm';
import { Plus, Star, MapPin } from 'lucide-react';

interface AddressManagerProps {
    type: 'shipping' | 'billing';
    title: string;
}

const AddressManager = ({ type, title }: AddressManagerProps) => {
    const {
        shippingAddresses,
        billingAddresses,
        loadShippingAddresses,
        loadBillingAddresses,
        deleteAddress,
        loading,
        error
    } = useAddressStore();

    const [showForm, setShowForm] = useState(false);
    const [editingAddress, setEditingAddress] = useState<AddressResponse | null>(null);

    const currentAddresses = type === 'shipping' ? shippingAddresses : billingAddresses;

    useEffect(() => {
        if (type === 'shipping') {
            loadShippingAddresses();
        } else {
            loadBillingAddresses();
        }
    }, [type]);

    const handleDelete = async (addressId: number) => {
        if (window.confirm('¿Estás seguro de que quieres eliminar esta dirección?')) {
            try {
                await deleteAddress(addressId);
            } catch (error) {
                // Error handling removed for production mode
            }
        }
    };

    const handleEdit = (address: AddressResponse) => {
        setEditingAddress(address);
        setShowForm(true);
    };

    const handleFormSuccess = () => {
        setShowForm(false);
        setEditingAddress(null);
        // Reload addresses
        if (type === 'shipping') {
            loadShippingAddresses();
        } else {
            loadBillingAddresses();
        }
    };

    const handleCancel = () => {
        setShowForm(false);
        setEditingAddress(null);
    };

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h3 className="text-lg font-semibold text-foreground">{title}</h3>
                <button
                    onClick={() => setShowForm(true)}
                    className="bg-primary text-primary-foreground px-4 py-2 rounded-md hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-colors flex items-center gap-2"
                >
                    <Plus className="w-4 h-4" />
                    Agregar Dirección
                </button>
            </div>

            {error && (
                <div className="bg-destructive/10 text-destructive border border-destructive/20 p-3 rounded-md">
                    {error}
                </div>
            )}

            {showForm && (
                <div className="bg-card border border-border rounded-lg p-6">
                    <h4 className="text-md font-medium text-foreground mb-4">
                        {editingAddress ? 'Editar' : 'Nueva'} Dirección de {type === 'shipping' ? 'Envío' : 'Facturación'}
                    </h4>
                    <AddressForm
                        type={type}
                        onSuccess={handleFormSuccess}
                        onCancel={handleCancel}
                        initialData={editingAddress ? {
                            firstName: editingAddress.firstName,
                            lastName: editingAddress.lastName,
                            address: editingAddress.address,
                            address2: editingAddress.address2,
                            city: editingAddress.city,
                            state: editingAddress.state,
                            zipCode: editingAddress.zipCode,
                            country: editingAddress.country,
                            phone: editingAddress.phone,
                            instructions: editingAddress.instructions,
                            isDefault: editingAddress.isDefault
                        } : undefined}
                        editMode={!!editingAddress}
                        addressId={editingAddress?.id}
                    />
                </div>
            )}

            <div className="space-y-3">
                {currentAddresses.length === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                        No tienes direcciones de {type === 'shipping' ? 'envío' : 'facturación'} registradas
                    </div>
                ) : (
                    <>
                        {currentAddresses.map((address) => (
                            <div key={address.id} className={`bg-card border rounded-lg p-4 transition-colors ${!address.isDefault ? 'hover:bg-muted/50 border-border' : 'border-primary/50 bg-primary/5'}`}>
                                <div className="flex items-start gap-4">
                                    {/* Información de la dirección */}
                                    <div className="flex-1">
                                        {address.isDefault && (
                                            <div className="flex items-center gap-2 mb-2">
                                                <span className="inline-flex items-center gap-1 bg-primary text-primary-foreground text-xs px-3 py-1 rounded-full font-medium">
                                                    <Star className="w-3 h-3" />
                                                    Dirección Predeterminada
                                                </span>
                                            </div>
                                        )}

                                        <div className="font-medium text-foreground">
                                            {address.fullName}
                                        </div>

                                        <div className="text-sm text-muted-foreground mt-1">
                                            {address.fullAddress}
                                        </div>

                                        {address.phone && (
                                            <div className="text-sm text-muted-foreground mt-1 flex items-center gap-2">
                                                <MapPin className="w-4 h-4" />
                                                {address.phone}
                                            </div>
                                        )}

                                        {address.instructions && (
                                            <div className="text-sm text-muted-foreground mt-2 italic">
                                                "{address.instructions}"
                                            </div>
                                        )}
                                    </div>

                                    {/* Botones de acción */}
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => handleEdit(address)}
                                            disabled={loading}
                                            className={`text-sm underline focus:outline-none focus:ring-2 focus:ring-blue-600 focus:ring-offset-2 rounded px-1 ${loading
                                                ? 'text-muted-foreground cursor-not-allowed'
                                                : 'text-blue-600 hover:text-blue-800'
                                                }`}
                                        >
                                            Editar
                                        </button>

                                        <button
                                            onClick={() => handleDelete(address.id)}
                                            disabled={loading}
                                            className={`text-sm underline focus:outline-none focus:ring-2 focus:ring-destructive focus:ring-offset-2 rounded px-1 ${loading
                                                ? 'text-muted-foreground cursor-not-allowed'
                                                : 'text-destructive hover:text-destructive/80'
                                                }`}
                                        >
                                            Eliminar
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </>
                )}
            </div>
        </div>
    );
};

export default AddressManager;
