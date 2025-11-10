/// <reference types="vite/client" />
import axios from 'axios';

// Get API base URL from environment variables
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// Type definitions
export interface ProductUpdateRequest {
    name: string;
    description?: string;
    shortDescription?: string;
    price: number;
    compareAtPrice?: number;
    stockQuantity: number;
    lowStockThreshold?: number;
    sku?: string;
    barcode?: string;
    categoryId?: number;
    imageUrl?: string;
    active: boolean;
    featured: boolean;
    seoTitle?: string;
    seoDescription?: string;
    seoKeywords?: string;
}

const api = axios.create({
    baseURL: API_BASE_URL,
});

// Export the main API instance
export { api };

// Test connectivity function
export const testConnectivity = async () => {
    try {
        // Use a public endpoint that doesn't require authentication
        const response = await fetch(`${API_BASE_URL}/public/health`);
        if (response.ok) {
            return true;
        } else {
            return false;
        }
    } catch (error) {
        return false;
    }
};

// Test token validity
export const testTokenValidity = async () => {
    try {
        // console.log('Testing token validity...');
        const token = localStorage.getItem('token');
        if (!token) {
            // console.log('❌ No token found');
            return false;
        }

        // console.log('Token found, testing with backend...');
        // console.log('✅ Token is valid');
        return true;
    } catch (error: any) {
        return false;
    }
};

// Add token to requests if available (but not for public endpoints)
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    // Don't add token for public endpoints
    if (token && config.url && !config.url.includes('/public/')) {
        config.headers.Authorization = `Bearer ${token}`;
        // console.log('API: Token found and added to request:', token.substring(0, 20) + '...');
    } else if (token) {
        // console.log('API: Token found but not added (public endpoint):', config.url);
    } else {
        // console.log('❌ API: No token found in localStorage');
    }
    // console.log('API: Making request to:', config.url, 'with method:', config.method);
    return config;
});

// Add response interceptor for logging and token expiration handling
api.interceptors.response.use(
    (response) => {
        // console.log('✅ API: Response received from:', response.config.url, 'status:', response.status);
        return response;
    },
    (error) => {
        try {
            const status = error.response?.status;
            const url = error.config?.url;
            // Error handling in production mode - logs removed

            // Log search errors specifically
            if (url && url.includes('/search')) {
                console.error('Search Error:', {
                    url,
                    status,
                    data: error.response?.data,
                    message: error.message
                });
            }

            // Handle token expiration (401 Unauthorized)
            // But don't logout for public endpoints or image/media requests
            if (status === 401 && url &&
                !url.includes('/public/') &&
                !url.includes('/images/') &&
                !url.includes('/media/')) {

                // Import stores dynamically to avoid circular dependencies
                import('../stores/userStore').then(({ useUserStore }) => {
                    import('../stores/notificationStore').then(({ useNotificationStore }) => {
                        // Clear user session
                        useUserStore.getState().logout();

                        // Show notification
                        useNotificationStore.getState().addNotification({
                            type: 'error',
                            message: 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.',
                            duration: 6000, // 6 seconds
                        });

                        // Redirect to login after a short delay
                        setTimeout(() => {
                            window.location.href = '/login';
                        }, 1000);
                    });
                });
            }

            // If it's the orders endpoint, log the request body as well for debugging
            if (url && url.includes('/orders')) {
                // Debug logging removed for production
            }
        } catch (logErr) {
            // Error logging removed for production
        }
        return Promise.reject(error);
    }
);

// =============================================================================
// ENDPOINTS PÚBLICOS (sin autenticación requerida)
// =============================================================================

export const publicAPI = {
    // Health check y testing
    health: () => api.get('/public/health'),
    test: () => api.get('/public/test'),

    // PRODUCTOS PÚBLICOS
    getProducts: (page: number = 0, size: number = 20) => {
        // console.log('PublicAPI: getProducts called with page:', page, 'size:', size);
        return api.get('/public/products', { params: { page, size } });
    },
    getProduct: (id: number) => {
        // console.log('PublicAPI: getProduct called with id:', id);
        return api.get(`/public/products/${id}`);
    },
    searchProducts: (query: string, page: number = 0, size: number = 20) => {
        // console.log('PublicAPI: searchProducts called with query:', query, 'page:', page, 'size:', size);
        return api.get('/public/products/search', { params: { query: query, page, size } });
    },
    getFeaturedProducts: () => {
        // console.log('PublicAPI: getFeaturedProducts called');
        return api.get('/public/products/featured');
    },

    // REVIEWS DE PRODUCTOS PÚBLICOS
    getProductReviews: (productId: number, page: number = 0, size: number = 10) => {
        // console.log('PublicAPI: getProductReviews called with productId:', productId, 'page:', page, 'size:', size);
        return api.get(`/public/products/${productId}/reviews`, { params: { page, size } });
    },

    // CATEGORÍAS PÚBLICAS
    getCategories: () => {
        // console.log('PublicAPI: getCategories called');
        return api.get('/public/categories');
    },
};

// =============================================================================
// ENDPOINTS DE USUARIOS AUTENTICADOS (requiere login de usuario normal)
// =============================================================================

export const authAPI = {
    login: (identifier: string, password: string) =>
        api.post('/auth/login', { identifier, password }),
    register: (username: string, email: string, password: string) =>
        api.post('/auth/register', { username, email, password }),
    refreshToken: () => api.post('/auth/refresh'),
    forgotPassword: (email: string) =>
        api.post('/auth/forgot-password', { email }),
    resetPassword: (token: string, newPassword: string) =>
        api.post('/auth/reset-password', { token, newPassword }),
};

export const profileAPI = {
    getProfile: () => api.get('/profile'),
    updateProfile: (profileData: { firstName: string; lastName: string; phone?: string }) =>
        api.put('/profile', profileData),
    deactivateAccount: () => api.delete('/profile'),
};

export const addressAPI = {
    // Get all user addresses
    getUserAddresses: () => api.get('/auth/addresses'),

    // Get addresses by type
    getShippingAddresses: () => api.get('/auth/addresses/shipping'),
    getBillingAddresses: () => api.get('/auth/addresses/billing'),

    // Create addresses
    createShippingAddress: (addressData: AddressRequest) => api.post('/auth/addresses/shipping', addressData),
    createBillingAddress: (addressData: AddressRequest) => api.post('/auth/addresses/billing', addressData),

    // Update address
    updateAddress: (id: number, addressData: AddressRequest) => api.put(`/auth/addresses/${id}`, addressData),

    // Set default address
    setDefaultAddress: (id: number) => api.put(`/auth/addresses/${id}/default`),

    // Delete address
    deleteAddress: (id: number) => api.delete(`/auth/addresses/${id}`),
};

// Types for address operations
export interface AddressRequest {
    firstName: string;
    lastName: string;
    address: string;
    address2?: string;
    city: string;
    state: string;
    zipCode: string;
    country: string;
    phone?: string;
    instructions?: string;
    isDefault?: boolean;
}

export interface AddressResponse {
    id: number;
    type: string;
    firstName: string;
    lastName: string;
    fullName: string;
    address: string;
    address2?: string;
    city: string;
    state?: string;
    zipCode: string;
    country: string;
    fullAddress: string;
    phone?: string;
    instructions?: string;
    isDefault: boolean;
    createdAt: string;
    updatedAt: string;
}

// Create a separate instance for cart operations that don't require authentication
const cartApi = axios.create({
    baseURL: API_BASE_URL,
});

// Add minimal interceptor for cart operations (no token required)
cartApi.interceptors.request.use((config) => {
    // console.log('CartAPI: Making request to:', config.url, 'with method:', config.method);
    return config;
});

cartApi.interceptors.response.use(
    (response) => {
        // console.log('✅ CartAPI: Response received from:', response.config.url, 'status:', response.status);
        return response;
    },
    (error) => {
        // Error handling removed for production mode
        return Promise.reject(error);
    }
);

export const cartAPI = {
    getCart: () => cartApi.get('/cart'),
    addToCart: (productId: number, quantity: number) =>
        cartApi.post('/cart/items', { productId, quantity }),
    updateCartItem: (itemId: string, quantity: number) =>
        cartApi.put(`/cart/items/${itemId}`, {}, { params: { quantity } }),
    removeFromCart: (itemId: string) => cartApi.delete(`/cart/items/${itemId}`),
    clearCart: () => cartApi.delete('/cart'),
};

export const orderAPI = {
    createOrder: (orderData: {
        shippingAddress: string;
        paymentMethod: string;
    }) => api.post('/orders', orderData),
    testAuth: () => api.get('/orders/test'),
    getUserOrders: () => api.get('/orders'),
    getOrder: (id: number) => api.get(`/orders/${id}`),
    updateOrderPaymentStatus: (orderId: number, paymentData: {
        status: string;
        paymentId: string;
        paymentMethod: string;
    }) => api.put(`/orders/${orderId}/payment-status`, paymentData),
};

export const productAPI = {
    // Reviews de productos (requiere autenticación)
    createProductReview: (productId: number, reviewData: { rating: number; comment?: string; title?: string }) => {
        // console.log('ProductAPI: createProductReview called with productId:', productId, 'reviewData:', reviewData);
        return api.post(`/products/${productId}/reviews`, reviewData);
    },
    updateProductReview: (productId: number, reviewId: number, reviewData: { productId?: number; rating: number; comment?: string; title?: string }) => {
        // console.log('ProductAPI: updateProductReview called with productId:', productId, 'reviewId:', reviewId, 'reviewData:', reviewData);
        return api.put(`/products/${productId}/reviews/${reviewId}`, reviewData);
    },
    deleteProductReview: (productId: number, reviewId: number) => {
        // console.log('ProductAPI: deleteProductReview called with productId:', productId, 'reviewId:', reviewId);
        return api.delete(`/products/${productId}/reviews/${reviewId}`);
    },
};

export const paymentAPI = {
    createPaymentPreference: (orderId: number) =>
        api.post('/payments/create-preference', { orderId }),
    createSimplePreference: (data: {
        orderId?: number;
        quantity?: number;
        description?: string;
        price?: number;
        auto_return?: "approved" | "all";
        origin?: string; // URL base para las URLs de retorno
        external_reference?: string; // Identificador externo (útil para tracking de órdenes)
        back_urls?: {
            success: string;
            failure: string;
            pending: string;
        }
    }) => api.post('/payments/create-preference', data),
    testMercadoPago: () => api.get('/payments/test-mercadopago'),
    getPaymentStatus: (paymentId: string) =>
        api.get(`/payments/status/${paymentId}`),
    cancelPayment: (paymentId: string) =>
        api.post(`/payments/cancel/${paymentId}`),
    handleError: (errorData: any) =>
        api.post('/payments/handle-error', errorData),
    verifyPayment: (preferenceId: string, paymentData?: {
        paymentId?: string;
        status?: string;
        fullUrl?: string;
        payment_type?: string;
        merchant_order_id?: string;
        external_reference?: string;
        collection_status?: string;
        processing_mode?: string;
        site_id?: string;
        additionalInfo?: any; // Para cualquier información adicional
    }) => {
        // Construir parámetros de consulta para el endpoint GET
        let params = new URLSearchParams();

        if (paymentData?.paymentId) {
            params.append('payment_id', paymentData.paymentId);
        }

        if (paymentData?.status) {
            params.append('status', paymentData.status);
        }

        if (paymentData?.merchant_order_id) {
            params.append('merchant_order_id', paymentData.merchant_order_id);
        }

        if (paymentData?.external_reference) {
            params.append('external_reference', paymentData.external_reference);
            // console.log('Incluyendo external_reference en la verificación:', paymentData.external_reference);
        }

        if (paymentData?.payment_type) {
            params.append('payment_type', paymentData.payment_type);
        }

        if (paymentData?.collection_status) {
            params.append('collection_status', paymentData.collection_status);
        }

        if (paymentData?.processing_mode) {
            params.append('processing_mode', paymentData.processing_mode);
        }

        if (paymentData?.site_id) {
            params.append('site_id', paymentData.site_id);
        }

        const queryString = params.toString();
        const url = queryString ?
            `/payments/verify/${preferenceId}?${queryString}` :
            `/payments/verify/${preferenceId}`;

        // console.log('Verificando pago con URL:', url);

        // Usar método GET con parámetros de consulta
        return api.get(url);
    },
    checkPendingPayments: () =>
        api.post('/payments/check-pending'),
};

// =============================================================================
// ENDPOINTS DE ADMINISTRADORES (requiere rol ADMIN)
// =============================================================================

// PRODUCTOS - ADMIN
export const adminProductAPI = {
    getProducts: (page: number = 0, size: number = 20) => api.get('/admin/products', { params: { page, size } }),
    getProduct: (id: number) => api.get(`/admin/products/${id}`),
    searchProducts: (query: string, page: number = 0, size: number = 20) => api.get('/admin/products/search', { params: { query, page, size } }),
    createProduct: (product: any) => api.post('/admin/products', product),
    updateProduct: (id: number, product: any) => api.put(`/admin/products/${id}`, product),
    softDeleteProduct: (id: number) => api.delete(`/admin/products/${id}`),
    hardDeleteProduct: (id: number, password: string) => api.delete(`/admin/products/${id}/eliminar`, {
        data: { id: id, confirmationPassword: password }
    }),
};

// ÓRDENES - ADMIN
export const adminOrderAPI = {
    getOrders: () => api.get('/admin/orders'),
    getOrder: (id: number) => api.get(`/admin/orders/${id}`),
    updateOrderStatus: (id: number, status: string) => api.put(`/admin/orders/${id}/status`, {}, { params: { status } }),
    getOrdersByStatus: (status: string) => api.get(`/admin/orders/status/${status}`),
    getOrdersByDateRange: (startDate: string, endDate: string) =>
        api.get('/admin/orders/date-range', { params: { startDate, endDate } }),
    getOrderStatistics: () => api.get('/admin/orders/statistics'),
    getRevenue: (startDate: string, endDate: string) =>
        api.get('/admin/orders/revenue', { params: { startDate, endDate } }),
};

// USUARIOS - ADMIN
export const adminUserAPI = {
    getUsers: (page: number = 0, size: number = 20) => api.get('/admin/users', { params: { page, size } }),
    searchUsers: (searchTerm: string, page: number = 0, size: number = 20) =>
        api.get('/admin/users/search', { params: { searchTerm, page, size } }),
    getUser: (id: number) => api.get(`/admin/users/${id}`),
    changeUserRole: (id: number, role: string) => api.put(`/admin/users/${id}/role`, { role }),
    toggleUserStatus: (id: number, active: boolean) => api.put(`/admin/users/${id}/status`, { active }),
    softDeleteUser: (id: number) => api.delete(`/admin/users/${id}`),
    hardDeleteUser: (id: number, password: string) => api.delete(`/admin/users/${id}/eliminar`, {
        data: { id: id, confirmationPassword: password }
    }),
    getUserStats: () => api.get('/admin/users/stats'),
};

// AUDITORÍA - ADMIN
export const auditAPI = {
    getAuditLogs: (page: number = 0, size: number = 20, filters?: {
        sortBy?: string;
        sortDir?: 'asc' | 'desc';
        userEmail?: string;
        action?: string;
        entityType?: string;
        entityId?: string;
        success?: boolean;
        startDate?: string;
        endDate?: string;
    }) => {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
            ...(filters?.sortBy && { sortBy: filters.sortBy }),
            ...(filters?.sortDir && { sortDir: filters.sortDir }),
            ...(filters?.userEmail && { userEmail: filters.userEmail }),
            ...(filters?.action && { action: filters.action }),
            ...(filters?.entityType && { entityType: filters.entityType }),
            ...(filters?.entityId && { entityId: filters.entityId }),
            ...(filters?.success !== undefined && { success: filters.success.toString() }),
            ...(filters?.startDate && { startDate: filters.startDate }),
            ...(filters?.endDate && { endDate: filters.endDate }),
        });
        return api.get(`/admin/audit?${params}`);
    },
    createTestData: () => api.post('/admin/audit/test-data'),
    getAuditCount: () => api.get('/admin/audit/count'),
    getAuditLogsByUser: (userId: number, page: number = 0, size: number = 50) =>
        api.get(`/admin/audit/user/${userId}`, { params: { page, size } }),
};

// IMÁGENES - ADMIN
export const adminImageAPI = {
    uploadImage: async (file: File, folder: string = 'PRODUCTS'): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('folder', folder);

        try {
            const response = await api.post('/admin/images/upload', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            // console.log('☁️ Backend: Image uploaded successfully:', response.data.data.secure_url);
            return response.data.data.secure_url;
        } catch (error) {
            // Error handling removed for production mode
            throw new Error('Failed to upload image to Cloudinary');
        }
    },
};

// REVIEWS - ADMIN
export const adminReviewAPI = {
    getAllReviews: (page: number = 0, size: number = 20) =>
        api.get('/admin/reviews', { params: { page, size } }),
    getPendingReviews: (page: number = 0, size: number = 20) =>
        api.get('/admin/reviews/pending', { params: { page, size } }),
    approveReview: (reviewId: number) =>
        api.put(`/admin/reviews/${reviewId}/approve`),
    rejectReview: (reviewId: number) =>
        api.delete(`/admin/reviews/${reviewId}/reject`),
    deleteReview: (reviewId: number, reason: string) =>
        api.delete(`/admin/reviews/${reviewId}`, { data: { reason } }),
};

// ATRIBUTOS DE VARIANTES - ADMIN
export const adminVariantAttributeAPI = {
    // Gestión de atributos globales
    getAttributes: () => api.get('/admin/variant-attributes'),
    getAttribute: (id: number) => api.get(`/admin/variant-attributes/${id}`),
    createAttribute: (attribute: any) => api.post('/admin/variant-attributes', attribute),
    updateAttribute: (id: number, attribute: any) => api.put(`/admin/variant-attributes/${id}`, attribute),
    deleteAttribute: (id: number) => api.delete(`/admin/variant-attributes/${id}`),

    // Configuración de atributos por categoría
    getCategoryAttributes: (categoryId: number) => api.get(`/admin/categories/${categoryId}/variant-attributes`),
    configureCategoryAttribute: (categoryId: number, config: any) =>
        api.post(`/admin/categories/${categoryId}/variant-attributes`, config),
    updateCategoryAttribute: (categoryId: number, attributeId: number, config: any) =>
        api.put(`/admin/categories/${categoryId}/variant-attributes/${attributeId}`, config),
    removeCategoryAttribute: (categoryId: number, attributeId: number) =>
        api.delete(`/admin/categories/${categoryId}/variant-attributes/${attributeId}`),
};

// CATEGORÍAS - ADMIN
export const adminCategoryAPI = {
    getCategories: () => api.get('/admin/categories'),
    getCategoryTree: () => api.get('/admin/categories/tree'),
    createCategory: (data: any) => api.post('/admin/categories', data),
    updateCategory: (id: number, data: any) => api.put(`/admin/categories/${id}`, data),
    deleteCategory: (id: number) => api.delete(`/admin/categories/${id}`),
    hardDeleteCategory: (id: number) => api.delete(`/admin/categories/${id}/eliminar`),
    updateCategoryOrder: (id: number, order: number) => api.put(`/admin/categories/${id}/order`, { sortOrder: order }),
    restoreCategory: (id: number) => api.post(`/admin/categories/${id}/restore`),
};

// ATRIBUTOS DE VARIANTES - PÚBLICO
export const variantAttributeAPI = {
    getAttributes: () => api.get('/variant-attributes'),
    getAttribute: (id: number) => api.get(`/variant-attributes/${id}`),
    getAttributesByCategory: (categoryId: number) => api.get(`/variant-attributes/category/${categoryId}`),
    getCategoryAttributeConfig: (categoryId: number) => api.get(`/variant-attributes/category/${categoryId}/config`),
};

// PAGOS - ADMIN
export const adminPaymentAPI = {
    refundPayment: (paymentId: string) =>
        api.post(`/payments/refund/${paymentId}`),
    processWebhook: (webhookData: string, signature: string) =>
        api.post('/payments/webhook', webhookData, {
            headers: { 'x-signature': signature }
        }),
};

// =============================================================================
// UTILIDADES
// =============================================================================

export const decodeJWT = (token: string) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload);
    } catch (error) {
        // Error handling removed for production mode
        return null;
    }
};

// Check if JWT token is expired
export const isTokenExpired = (token: string): boolean => {
    try {
        const decoded = decodeJWT(token);
        if (!decoded || !decoded.exp) {
            return true; // Consider invalid tokens as expired
        }

        // exp is in seconds, Date.now() is in milliseconds
        const currentTime = Math.floor(Date.now() / 1000);
        return decoded.exp < currentTime;
    } catch (error) {
        // Error handling removed for production mode
        return true; // Consider tokens that can't be decoded as expired
    }
};

// imageAPI eliminado - Las imágenes ahora son públicas

export default api;
