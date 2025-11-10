import { useState } from 'react';
import { useAddressStore } from '../stores/addressStore';
import { AddressRequest } from '../services/api';

interface AddressFormProps {
    type: 'shipping' | 'billing';
    onSuccess?: () => void;
    onCancel?: () => void;
    initialData?: Partial<AddressRequest>;
    editMode?: boolean;
    addressId?: number;
}

const AddressForm = ({ type, onSuccess, onCancel, initialData, editMode = false, addressId }: AddressFormProps) => {
    const { createShippingAddress, createBillingAddress, updateAddress, loading } = useAddressStore();

    const [formData, setFormData] = useState<AddressRequest>({
        firstName: initialData?.firstName || '',
        lastName: initialData?.lastName || '',
        address: initialData?.address || '',
        address2: initialData?.address2 || '',
        city: initialData?.city || '',
        state: initialData?.state || '',
        zipCode: initialData?.zipCode || '',
        country: initialData?.country || '',
        phone: initialData?.phone || '',
        instructions: initialData?.instructions || '',
        isDefault: initialData?.isDefault || false
    });

    const [errors, setErrors] = useState<Partial<AddressRequest>>({});

    const validateForm = (): boolean => {
        const newErrors: Partial<AddressRequest> = {};

        if (!formData.firstName.trim()) newErrors.firstName = 'El nombre es obligatorio';
        if (!formData.lastName.trim()) newErrors.lastName = 'El apellido es obligatorio';
        if (!formData.address.trim()) newErrors.address = 'La dirección es obligatoria';
        if (!formData.city.trim()) newErrors.city = 'La ciudad es obligatoria';
        if (!formData.zipCode.trim()) newErrors.zipCode = 'El código postal es obligatorio';
        if (!formData.country.trim()) newErrors.country = 'El país es obligatorio';

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!validateForm()) return;

        try {
            if (editMode && addressId) {
                await updateAddress(addressId, formData);
            } else {
                if (type === 'shipping') {
                    await createShippingAddress(formData);
                } else {
                    await createBillingAddress(formData);
                }
            }

            onSuccess?.();
        } catch (error) {
            // Error handling removed for production mode
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof AddressRequest]) {
            setErrors(prev => ({ ...prev, [name]: undefined }));
        }
    };

    const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, checked } = e.target;
        setFormData(prev => ({ ...prev, [name]: checked }));
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                    <label className="block text-sm font-medium text-foreground mb-1">
                        Nombre *
                    </label>
                    <input
                        type="text"
                        name="firstName"
                        value={formData.firstName}
                        onChange={handleChange}
                        className={`w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors ${errors.firstName ? 'border-destructive' : 'border-border'
                            }`}
                        placeholder="Juan"
                    />
                    {errors.firstName && (
                        <p className="text-sm text-destructive mt-1">{errors.firstName}</p>
                    )}
                </div>

                <div>
                    <label className="block text-sm font-medium text-foreground mb-1">
                        Apellido *
                    </label>
                    <input
                        type="text"
                        name="lastName"
                        value={formData.lastName}
                        onChange={handleChange}
                        className={`w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors ${errors.lastName ? 'border-destructive' : 'border-border'
                            }`}
                        placeholder="Pérez"
                    />
                    {errors.lastName && (
                        <p className="text-sm text-destructive mt-1">{errors.lastName}</p>
                    )}
                </div>
            </div>

            <div>
                <label className="block text-sm font-medium text-foreground mb-1">
                    Dirección *
                </label>
                <input
                    type="text"
                    name="address"
                    value={formData.address}
                    onChange={handleChange}
                    className={`w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors ${errors.address ? 'border-destructive' : 'border-border'
                        }`}
                    placeholder="Calle Principal 123"
                />
                {errors.address && (
                    <p className="text-sm text-destructive mt-1">{errors.address}</p>
                )}
            </div>

            <div>
                <label className="block text-sm font-medium text-foreground mb-1">
                    Dirección adicional
                </label>
                <input
                    type="text"
                    name="address2"
                    value={formData.address2}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
                    placeholder="Apartamento, piso, etc."
                />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                    <label className="block text-sm font-medium text-foreground mb-1">
                        Ciudad *
                    </label>
                    <input
                        type="text"
                        name="city"
                        value={formData.city}
                        onChange={handleChange}
                        className={`w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors ${errors.city ? 'border-destructive' : 'border-border'
                            }`}
                        placeholder="Buenos Aires"
                    />
                    {errors.city && (
                        <p className="text-sm text-destructive mt-1">{errors.city}</p>
                    )}
                </div>

                <div>
                    <label className="block text-sm font-medium text-foreground mb-1">
                        Estado/Provincia
                    </label>
                    <input
                        type="text"
                        name="state"
                        value={formData.state}
                        onChange={handleChange}
                        className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
                        placeholder="Buenos Aires"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-foreground mb-1">
                        Código Postal *
                    </label>
                    <input
                        type="text"
                        name="zipCode"
                        value={formData.zipCode}
                        onChange={handleChange}
                        className={`w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors ${errors.zipCode ? 'border-destructive' : 'border-border'
                            }`}
                        placeholder="1000"
                    />
                    {errors.zipCode && (
                        <p className="text-sm text-destructive mt-1">{errors.zipCode}</p>
                    )}
                </div>

                <div>
                    <label className="block text-sm font-medium text-foreground mb-1">
                        País *
                    </label>
                    <input
                        type="text"
                        name="country"
                        value={formData.country}
                        onChange={handleChange}
                        className={`w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors ${errors.country ? 'border-destructive' : 'border-border'
                            }`}
                        placeholder="Argentina"
                    />
                    {errors.country && (
                        <p className="text-sm text-destructive mt-1">{errors.country}</p>
                    )}
                </div>
            </div>

            <div>
                <label className="block text-sm font-medium text-foreground mb-1">
                    Teléfono
                </label>
                <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
                    placeholder="+54 11 1234-5678"
                />
            </div>

            <div>
                <label className="block text-sm font-medium text-foreground mb-1">
                    Instrucciones de entrega
                </label>
                <textarea
                    name="instructions"
                    value={formData.instructions}
                    onChange={handleChange}
                    rows={3}
                    className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors"
                    placeholder="Instrucciones especiales para la entrega..."
                />
            </div>

            <div className="flex items-center space-x-2">
                <input
                    type="checkbox"
                    id="isDefault"
                    name="isDefault"
                    checked={formData.isDefault}
                    onChange={handleCheckboxChange}
                    className="w-4 h-4 text-primary bg-background border-border rounded focus:ring-primary focus:ring-2"
                />
                <label htmlFor="isDefault" className="text-sm font-medium text-foreground">
                    Marcar como dirección predeterminada
                </label>
            </div>

            <div className="flex gap-3 pt-4">
                <button
                    type="submit"
                    disabled={loading}
                    className="flex-1 bg-primary text-primary-foreground px-4 py-2 rounded-md hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                    {loading ? 'Guardando...' : `${editMode ? 'Actualizar' : 'Guardar'} Dirección de ${type === 'shipping' ? 'Envío' : 'Facturación'}`}
                </button>

                {onCancel && (
                    <button
                        type="button"
                        onClick={onCancel}
                        className="px-4 py-2 border border-border rounded-md bg-background text-foreground hover:bg-accent focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-colors"
                    >
                        Cancelar
                    </button>
                )}
            </div>
        </form>
    );
};

export default AddressForm;
