import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useProductStore } from '../stores/productStore';
import { useCartStore } from '../stores/cartStore';
import { useNotificationStore } from '../stores/notificationStore';
import SignedImage from '../components/SignedImage';
import Pagination from '../components/Pagination';

const SearchResults = () => {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const {
    products,
    searchProducts,
    loading,
    error,
    currentPage,
    totalPages,
    totalElements,
    size
  } = useProductStore();
  const { addItem } = useCartStore();
  const { addNotification } = useNotificationStore();
  const [priceFilter, setPriceFilter] = useState('');

  useEffect(() => {
    if (query) {
      searchProducts(query, 0, 12);
    }
  }, [query, searchProducts]);

  const filteredProducts = (products || []).filter(product => {
    const matchesPrice = priceFilter ? product.price <= parseFloat(priceFilter) : true;
    return matchesPrice;
  });

  const handleAddToCart = async (product: any) => {
    try {
      await addItem({
        id: product.id,
        name: product.name,
        price: product.price,
        quantity: 1,
        productImage: product.imageUrl
      });
      addNotification({
        type: 'success',
        message: 'Producto agregado al carrito',
      });
      //alert('Producto agregado al carrito');
    } catch (error) {
      // Error handling removed for production mode
      addNotification({
        type: 'error',
        message: 'Error al agregar producto al carrito',
      });
      //alert('Error al agregar producto al carrito');
    }
  };

  const handlePageChange = (page: number) => {
    if (query) {
      searchProducts(query, page, 12);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto px-4 py-8">
          <div className="text-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Buscando productos...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto px-4 py-8">
          <div className="text-center py-16">
            <div className="text-red-500 mb-4">Error al buscar productos</div>
            <p className="text-muted-foreground mb-4">{error}</p>
            <Link
              to="/"
              className="bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
            >
              Volver al inicio
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-3xl font-bold text-foreground">
              Resultados para "{query}"
            </h1>
            <Link
              to="/"
              className="text-primary hover:text-primary/80 transition-colors"
            >
              ← Volver al inicio
            </Link>
          </div>

          {/* Filters */}
          <div className="flex items-center space-x-4 mb-6">
            <label className="text-foreground font-medium">Filtrar por precio:</label>
            <select
              value={priceFilter}
              onChange={(e) => setPriceFilter(e.target.value)}
              className="px-4 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring bg-background text-foreground"
            >
              <option value="">Todos los precios</option>
              <option value="50">Hasta $50</option>
              <option value="100">Hasta $100</option>
              <option value="200">Hasta $200</option>
            </select>
          </div>
        </div>

        {/* Results */}
        {filteredProducts.length === 0 ? (
          <div className="text-center py-16">
            <div className="text-6xl mb-4"></div>
            <p className="text-muted-foreground text-xl mb-4">
              No se encontraron productos para "{query}".
            </p>
            <Link
              to="/"
              className="bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
            >
              Ver todos los productos
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredProducts.map((product) => (
              <div key={product.id} className="bg-card rounded-lg shadow-sm overflow-hidden hover:shadow-md transition-all duration-300 border border-border">
                <div className="flex">
                  <Link to={`/product/${product.id}`} className="w-32 h-32 bg-muted flex-shrink-0">
                    <SignedImage
                      src={product.imageUrl || '/placeholder-product.jpg'}
                      alt={product.name}
                      className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                    />
                  </Link>
                  <div className="flex-1 p-4">
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
              </div>
            ))}
          </div>
        )}

        {/* Paginación */}
        {filteredProducts && filteredProducts.length > 0 && (
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalElements={totalElements}
            size={size}
            onPageChange={handlePageChange}
          />
        )}
      </div>
    </div>
  );
};

export default SearchResults;
