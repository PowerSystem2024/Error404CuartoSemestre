import { create } from 'zustand';
import { api } from '../services/api';

interface CartItem {
    id: number; // This is productId when created locally, cartItemId after sync
    productId?: number; // Optional - will be set during sync
    name: string;
    price: number;
    quantity: number;
    productImage?: string;
}

interface CartStore {
    items: CartItem[];
    loading: boolean;
    sessionId: string | null;
    addItem: (item: CartItem) => Promise<void>;
    removeItem: (id: number) => Promise<void>;
    updateQuantity: (id: number, quantity: number) => Promise<void>;
    clearCart: () => Promise<void>;
    getTotal: () => number;
    loadCart: () => Promise<void>;
    syncWithBackend: () => Promise<void>;
    transferCartToBackend: () => Promise<void>;
    saveToLocalStorage: () => void;
    loadFromLocalStorage: () => void;
    generateSessionId: () => string;
    getSessionId: () => string;
}

const CART_STORAGE_KEY = 'smart_cart';
const SESSION_ID_KEY = 'cart_session_id';

// Utility functions
const generateSessionId = (): string => {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
};

const getSessionId = (): string => {
    let sessionId = localStorage.getItem(SESSION_ID_KEY);
    if (!sessionId) {
        sessionId = generateSessionId();
        localStorage.setItem(SESSION_ID_KEY, sessionId);
    }
    return sessionId;
};

export const useCartStore = create<CartStore>((set, get) => ({
    items: [],
    loading: false,
    sessionId: null,

    generateSessionId,

    getSessionId,

    saveToLocalStorage: () => {
        const items = get().items;
        const sessionId = get().getSessionId();
        const cartData = {
            items,
            sessionId,
            timestamp: Date.now()
        };
        localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cartData));
    },

    loadFromLocalStorage: () => {
        const stored = localStorage.getItem(CART_STORAGE_KEY);
        if (stored) {
            try {
                const cartData = JSON.parse(stored);
                // Check if data is not too old (24 hours)
                const isExpired = Date.now() - cartData.timestamp > 24 * 60 * 60 * 1000;
                if (!isExpired && cartData.items) {
                    set({ items: cartData.items });
                    return true;
                }
            } catch (error) {
                // Error handling removed for production mode
            }
        }
        return false;
    },

    syncWithBackend: async () => {
        const token = localStorage.getItem('token');
        const sessionId = get().getSessionId();

        try {
            if (token) {
                // User is authenticated - sync with user cart
                const response = await api.get('/cart');
                const items: CartItem[] = response.data.items?.map((item: any) => ({
                    id: parseInt(item.id), // cartItemId becomes the id
                    productId: parseInt(item.productId), // Store productId separately
                    name: item.productName || 'Producto',
                    price: parseFloat(item.price),
                    quantity: item.quantity,
                    productImage: item.productImage
                })).filter((item: CartItem) => !isNaN(item.price) && item.price > 0) || [];

                set({ items });
                get().saveToLocalStorage();
            } else {
                // Guest user - sync with session cart
                const response = await api.get(`/guest-cart?sessionId=${sessionId}`);
                const items: CartItem[] = response.data.items?.map((item: any) => ({
                    id: parseInt(item.id), // cartItemId becomes the id
                    productId: parseInt(item.productId), // Store productId separately
                    name: item.productName || 'Producto',
                    price: parseFloat(item.price),
                    quantity: item.quantity,
                    productImage: item.productImage
                })).filter((item: CartItem) => !isNaN(item.price) && item.price > 0) || [];

                set({ items });
                get().saveToLocalStorage();
            }
        } catch (error) {
            // Error handling removed for production mode
            // Fallback to localStorage
            get().loadFromLocalStorage();
        }
    },

    loadCart: async () => {
        // First, try to load from localStorage for instant UX
        get().loadFromLocalStorage();
        const hasLocalData = get().items.length > 0;

        // Then sync with backend (don't block UI)
        try {
            await get().syncWithBackend();
        } catch (error) {
            // Error handling removed for production mode
            // If backend fails and we don't have local data, keep empty cart
            if (!hasLocalData) {
                set({ items: [] });
            }
        }
    },

    transferCartToBackend: async () => {
        // This function is now deprecated - sync happens automatically
        await get().syncWithBackend();
    },

    addItem: async (item) => {
        const token = localStorage.getItem('token');
        const sessionId = get().getSessionId();

        // Optimistic update - update localStorage immediately for instant UX
        set((state) => {
            const existing = state.items.find((i) => i.id === item.id);
            if (existing) {
                const updatedItems = state.items.map((i) =>
                    i.id === item.id ? { ...i, quantity: i.quantity + item.quantity } : i
                );
                set({ items: updatedItems });
                get().saveToLocalStorage();
                return { items: updatedItems };
            } else {
                const newItems = [...state.items, item];
                set({ items: newItems });
                get().saveToLocalStorage();
                return { items: newItems };
            }
        });

        // Sync with backend (don't wait for it to complete)
        try {
            if (token) {
                await api.post('/cart/items', { productId: item.id, quantity: item.quantity });
            } else {
                await api.post('/guest-cart/items', { productId: item.id, quantity: item.quantity, sessionId });
            }
            // Re-sync to ensure consistency
            setTimeout(() => get().syncWithBackend(), 100);
        } catch (error) {
            // Error handling removed for production mode
            // Backend sync failed, but localStorage is already updated
            // User will see the item in cart, and it will sync when possible
        }
    },

    removeItem: async (id) => {
        const token = localStorage.getItem('token');
        const sessionId = get().getSessionId();

        // Optimistic update
        set((state) => {
            const newItems = state.items.filter((i) => i.id !== id);
            set({ items: newItems });
            get().saveToLocalStorage();
            return { items: newItems };
        });

        // Sync with backend
        try {
            if (token) {
                await api.delete(`/cart/items/${id}`);
            } else {
                await api.delete(`/guest-cart/items/${id}?sessionId=${sessionId}`);
            }
            setTimeout(() => get().syncWithBackend(), 100);
        } catch (error) {
            // Error handling removed for production mode
            // If deletion failed, sync with backend to restore correct state
            await get().syncWithBackend();
        }
    },

    updateQuantity: async (id, quantity) => {
        const token = localStorage.getItem('token');
        const sessionId = get().getSessionId();

        // Optimistic update
        set((state) => {
            const updatedItems = state.items.map((item) =>
                item.id === id ? { ...item, quantity } : item
            );
            set({ items: updatedItems });
            get().saveToLocalStorage();
            return { items: updatedItems };
        });

        // Sync with backend
        try {
            if (token) {
                await api.put(`/cart/items/${id}`, {}, { params: { quantity } });
            } else {
                await api.put(`/guest-cart/items/${id}`, {}, { params: { quantity, sessionId } });
            }
            setTimeout(() => get().syncWithBackend(), 100);
        } catch (error) {
            // Error handling removed for production mode
        }
    },

    clearCart: async () => {
        const token = localStorage.getItem('token');
        const sessionId = get().getSessionId();

        // Optimistic update
        set({ items: [] });
        get().saveToLocalStorage();

        // Sync with backend
        try {
            if (token) {
                await api.delete('/cart');
            } else {
                await api.delete(`/guest-cart?sessionId=${sessionId}`);
            }
        } catch (error) {
            // Error handling removed for production mode
        }
    },

    getTotal: () => get().items.reduce((total, item) => total + item.price * item.quantity, 0),
}));
