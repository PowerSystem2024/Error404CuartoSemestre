import { create } from 'zustand';
import { publicAPI } from '../services/api';

interface Product {
    id: number;
    name: string;
    description: string;
    shortDescription?: string;
    price: number;
    compareAtPrice?: number;
    imageUrl: string;
    stockQuantity?: number;
    lowStockThreshold?: number;
    sku?: string;
    barcode?: string;
    categoryId?: number;
    category?: { id: string | number; name: string };
    active?: boolean;
    featured?: boolean;
    seoTitle?: string;
    seoDescription?: string;
    seoKeywords?: string;
}

interface ProductStore {
    products: Product[];
    featuredProducts: Product[];
    loading: boolean;
    error: string | null;
    // Pagination info
    currentPage: number;
    totalPages: number;
    totalElements: number;
    size: number;
    setProducts: (products: Product[]) => void;
    fetchProducts: (page?: number, size?: number) => Promise<void>;
    fetchFeaturedProducts: () => Promise<void>;
    searchProducts: (query: string, page?: number, size?: number) => Promise<void>;
    addProduct: (product: Product) => void;
    updateProduct: (id: number, product: Partial<Product>) => void;
    removeProduct: (id: number) => void;
    clearError: () => void;
}

export const useProductStore = create<ProductStore>((set) => ({
    products: [],
    featuredProducts: [],
    loading: false,
    error: null,
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    size: 20,
    setProducts: (products) => set({ products }),
    fetchProducts: async (page = 0, size = 20) => {
        // console.log('TiendaProducto: fetchProducts llamado');
        set({ loading: true, error: null });
        try {
            // console.log('TiendaProducto: Llamando a publicAPI.getProducts()');
            const response = await publicAPI.getProducts(page, size);
            // console.log('✅ TiendaProducto: Respuesta de API recibida:', response.data);
            // console.log('TiendaProducto: Tipo de response.data:', typeof response.data);

            // Axios debería parsear JSON automáticamente
            const data = response.data;
            // console.log('TiendaProducto: data:', data);
            // console.log('TiendaProducto: data.content:', data?.content);

            // Manejar respuesta paginada de Spring Boot
            const products = data.content || data || [];
            // console.log('TiendaProducto: Productos a establecer:', products);

            set({
                products,
                loading: false,
                currentPage: data.pageable?.pageNumber || page,
                totalPages: data.totalPages || 0,
                totalElements: data.totalElements || 0,
                size: data.pageable?.pageSize || size
            });
            // console.log('TiendaProducto: Productos establecidos en el store');
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al obtener productos', loading: false });
        }
    },
    fetchFeaturedProducts: async () => {
        // console.log('TiendaProducto: fetchFeaturedProducts llamado');
        set({ loading: true, error: null });
        try {
            // console.log('TiendaProducto: Llamando a publicAPI.getFeaturedProducts()');
            const response = await publicAPI.getFeaturedProducts();
            // console.log('✅ TiendaProducto: Respuesta de API de productos destacados:', response.data);
            set({ featuredProducts: response.data || [], loading: false });
            // console.log('TiendaProducto: Productos destacados establecidos en:', response.data || []);
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al obtener productos destacados', loading: false });
        }
    },
    searchProducts: async (query: string, page = 0, size = 20) => {
        // console.log('TiendaProducto: searchProducts llamado con consulta:', query, 'página:', page, 'tamaño:', size);
        set({ loading: true, error: null });
        try {
            // console.log('TiendaProducto: Llamando a publicAPI.searchProducts()');
            const response = await publicAPI.searchProducts(query, page, size);
            // console.log('✅ TiendaProducto: Respuesta de API de búsqueda:', response.data);
            const data = response.data;
            const products = data.content || data || [];

            set({
                products,
                loading: false,
                currentPage: data.pageable?.pageNumber || page,
                totalPages: data.totalPages || 0,
                totalElements: data.totalElements || 0,
                size: data.pageable?.pageSize || size
            });
            // console.log('TiendaProducto: Resultados de búsqueda establecidos en:', products);
        } catch (error: any) {
            // Error handling removed for production mode
            set({ error: error.response?.data?.message || 'Error al buscar productos', loading: false });
        }
    },
    addProduct: (product) => set((state) => ({ products: [...state.products, product] })),
    updateProduct: (id, updatedProduct) =>
        set((state) => ({
            products: state.products.map((p) => (p.id === id ? { ...p, ...updatedProduct } : p)),
        })),
    removeProduct: (id) =>
        set((state) => ({ products: state.products.filter((p) => p.id !== id) })),
    clearError: () => set({ error: null }),
}));
