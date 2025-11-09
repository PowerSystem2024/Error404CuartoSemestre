import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useProductStore } from '../stores/productStore';
import { useCartStore } from '../stores/cartStore';
import { useNotificationStore } from '../stores/notificationStore';
import SignedImage from '../components/SignedImage';
import Pagination from '../components/Pagination';
import { Package } from 'lucide-react';

const Products = () => {
    // console.log('Productos: Componente renderizado');
    const {
        products,
        fetchProducts,
        loading,
        error,
        currentPage,
        totalPages,
        totalElements,
        size
    } = useProductStore();
    const { addItem } = useCartStore();
    const { addNotification } = useNotificationStore();

    // console.log('Productos: Estado - productos:', products, 'cargando:', loading, 'error:', error);

    useEffect(() => {
        // console.log('Productos: useEffect activado, llamando a fetchProducts');
        fetchProducts(0, 12); // Cargar primera página con 12 productos
    }, [fetchProducts]);

    const handleAddToCart = async (product: any) => {
        try {
            // Logging removed for production mode
            await addItem({
                id: product.id,
                name: product.name,
                price: product.price,
                quantity: 1,
            });
            // Success logging removed for production mode
            addNotification({
                type: 'success',
                message: 'Producto agregado al carrito exitosamente',
                duration: 3000
            });
        } catch (error) {
            // Error handling removed for production mode
            addNotification({
                type: 'error',
                message: 'Error al agregar producto al carrito',
                duration: 4000
            });
        }
    };

    const handlePageChange = (page: number) => {
        fetchProducts(page, 12);
    };

    const handleReload = () => {
        fetchProducts(0, 12);
    };

    if (loading) {
        // console.log('Productos: Renderizando estado de carga');
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary mx-auto mb-4"></div>
                    <p className="text-xl text-muted-foreground">Cargando productos...</p>
                </div>
            </div>
        );
    }

    if (error) {
        // console.log('Productos: Renderizando estado de error:', error);
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <div className="text-center">
                    <div className="text-destructive text-xl mb-4">Error al cargar productos</div>
                    <p className="text-muted-foreground">{error}</p>
                    <button
                        onClick={handleReload}
                        className="mt-4 bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
                    >
                        Reintentar
                    </button>
                </div>
            </div>
        );
    }

    // console.log('Productos: Renderizando contenido principal con', products?.length || 0, 'productos');

    return (
        <div className="min-h-screen bg-background">
            {/* Encabezado */}
            <section className="py-12 bg-muted/30">
                <div className="container mx-auto px-4">
                    <div className="max-w-4xl mx-auto text-center">
                        <h1 className="text-4xl md:text-6xl font-bold text-foreground mb-6 text-balance">
                            Todos los Productos
                        </h1>
                        <p className="text-xl text-muted-foreground mb-8 text-pretty max-w-2xl mx-auto">
                            Explora nuestra completa colección de productos de alta calidad.
                        </p>
                    </div>
                </div>
            </section>

            {/* Productos */}
            <section className="py-12 bg-background">
                <div className="container mx-auto px-4">
                    <div className="mb-8">
                        <h2 className="text-3xl font-bold text-foreground">
                            Todos los Productos ({products?.length || 0})
                        </h2>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
                        {products?.map((product) => (
                            <div key={product.id} className="bg-card rounded-lg shadow-sm overflow-hidden hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 border border-border">
                                <Link to={`/product/${product.id}`}>
                                    <div className="aspect-square bg-muted flex items-center justify-center relative overflow-hidden">
                                        <SignedImage
                                            src={product.imageUrl || '/placeholder-product.jpg'}
                                            alt={product.name}
                                            className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                                        />
                                        <div className="absolute inset-0 bg-black bg-opacity-0 hover:bg-opacity-10 transition-all duration-300"></div>
                                    </div>
                                </Link>
                                <div className="p-4">
                                    <Link to={`/product/${product.id}`}>
                                        <h3 className="font-bold text-lg mb-2 line-clamp-2 hover:text-primary transition-colors text-foreground">
                                            {product.name}
                                        </h3>
                                    </Link>
                                    <p className="text-muted-foreground text-sm mb-3 line-clamp-2">
                                        {product.description}
                                    </p>
                                    <div className="flex items-center justify-between">
                                        <span className="text-2xl font-bold text-primary">
                                            ${product.price}
                                        </span>
                                        <button
                                            onClick={() => handleAddToCart(product)}
                                            className="bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-md font-semibold transition-colors shadow-sm hover:shadow-md"
                                        >
                                            Agregar
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {(!products || products.length === 0) && (
                        <div className="text-center py-16">
                            <Package className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
                            <p className="text-muted-foreground text-xl">
                                No hay productos disponibles en este momento.
                            </p>
                            <button
                                onClick={handleReload}
                                className="mt-4 bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
                            >
                                Recargar productos
                            </button>
                        </div>
                    )}
                </div>

                {/* Paginación */}
                {products && products.length > 0 && (
                    <Pagination
                        currentPage={currentPage}
                        totalPages={totalPages}
                        totalElements={totalElements}
                        size={size}
                        onPageChange={handlePageChange}
                    />
                )}
            </section>
        </div>
    );
};

export default Products;
