import { create } from 'zustand';
import { addressAPI, AddressResponse, AddressRequest } from '../services/api';

interface AddressStore {
    addresses: AddressResponse[];
    shippingAddresses: AddressResponse[];
    billingAddresses: AddressResponse[];
    loading: boolean;
    error: string | null;

    // Actions
    loadUserAddresses: () => Promise<void>;
    loadShippingAddresses: () => Promise<void>;
    loadBillingAddresses: () => Promise<void>;
    createShippingAddress: (addressData: AddressRequest) => Promise<AddressResponse>;
    createBillingAddress: (addressData: AddressRequest) => Promise<AddressResponse>;
    updateAddress: (id: number, addressData: AddressRequest) => Promise<AddressResponse>;
    setDefaultAddress: (id: number) => Promise<void>;
    deleteAddress: (id: number) => Promise<void>;
    clearError: () => void;
}

export const useAddressStore = create<AddressStore>((set) => ({
    addresses: [],
    shippingAddresses: [],
    billingAddresses: [],
    loading: false,
    error: null,

    loadUserAddresses: async () => {
        try {
            set({ loading: true, error: null });
            const response = await addressAPI.getUserAddresses();
            set({ addresses: response.data });
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al cargar direcciones' });
        } finally {
            set({ loading: false });
        }
    },

    loadShippingAddresses: async () => {
        try {
            set({ loading: true, error: null });
            const response = await addressAPI.getShippingAddresses();
            set({ shippingAddresses: response.data });
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al cargar direcciones de envío' });
        } finally {
            set({ loading: false });
        }
    },

    loadBillingAddresses: async () => {
        try {
            set({ loading: true, error: null });
            const response = await addressAPI.getBillingAddresses();
            set({ billingAddresses: response.data });
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al cargar direcciones de facturación' });
        } finally {
            set({ loading: false });
        }
    },

    createShippingAddress: async (addressData: AddressRequest) => {
        try {
            set({ loading: true, error: null });
            const response = await addressAPI.createShippingAddress(addressData);
            const newAddress = response.data;

            // Update local state
            set(state => ({
                shippingAddresses: [...state.shippingAddresses, newAddress],
                addresses: [...state.addresses, newAddress]
            }));

            return newAddress;
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al crear dirección de envío' });
            throw error;
        } finally {
            set({ loading: false });
        }
    },

    createBillingAddress: async (addressData: AddressRequest) => {
        try {
            set({ loading: true, error: null });
            const response = await addressAPI.createBillingAddress(addressData);
            const newAddress = response.data;

            // Update local state
            set(state => ({
                billingAddresses: [...state.billingAddresses, newAddress],
                addresses: [...state.addresses, newAddress]
            }));

            return newAddress;
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al crear dirección de facturación' });
            throw error;
        } finally {
            set({ loading: false });
        }
    },

    updateAddress: async (id: number, addressData: AddressRequest) => {
        try {
            set({ loading: true, error: null });
            const response = await addressAPI.updateAddress(id, addressData);
            const updatedAddress = response.data;

            // Update local state
            set(state => ({
                addresses: state.addresses.map(addr => addr.id === id ? updatedAddress : addr),
                shippingAddresses: state.shippingAddresses.map(addr => addr.id === id ? updatedAddress : addr),
                billingAddresses: state.billingAddresses.map(addr => addr.id === id ? updatedAddress : addr)
            }));

            return updatedAddress;
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al actualizar dirección' });
            throw error;
        } finally {
            set({ loading: false });
        }
    },

    setDefaultAddress: async (id: number) => {
        try {
            set({ loading: true, error: null });
            await addressAPI.setDefaultAddress(id);

            // Update local state - mark this address as default and others as not default
            set(state => ({
                addresses: state.addresses.map(addr => ({
                    ...addr,
                    isDefault: addr.id === id
                })),
                shippingAddresses: state.shippingAddresses.map(addr => ({
                    ...addr,
                    isDefault: addr.id === id
                })),
                billingAddresses: state.billingAddresses.map(addr => ({
                    ...addr,
                    isDefault: addr.id === id
                }))
            }));
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al establecer dirección predeterminada' });
            throw error;
        } finally {
            set({ loading: false });
        }
    },

    deleteAddress: async (id: number) => {
        try {
            set({ loading: true, error: null });
            await addressAPI.deleteAddress(id);

            // Update local state
            set(state => ({
                addresses: state.addresses.filter(addr => addr.id !== id),
                shippingAddresses: state.shippingAddresses.filter(addr => addr.id !== id),
                billingAddresses: state.billingAddresses.filter(addr => addr.id !== id)
            }));
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al eliminar dirección' });
            throw error;
        } finally {
            set({ loading: false });
        }
    },

    clearError: () => set({ error: null }),
}));
