import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useProductStore } from '../stores/productStore';
import { useCartStore } from '../stores/cartStore';
import { useNotificationStore } from '../stores/notificationStore';
import { Laptop, Shirt, HomeIcon, Zap, BookOpen, Gift } from 'lucide-react';

const Home = () => {
  // console.log(' Inicio: Componente renderizado');
  const { products, fetchProducts, loading, error } = useProductStore();
  const { addItem } = useCartStore();
  const { addNotification } = useNotificationStore();
  const [priceFilter, setPriceFilter] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  // console.log('Inicio: Estado - productos:', products, 'cargando:', loading, 'error:', error);

  useEffect(() => {
    // console.log('Inicio: useEffect activado, llamando a fetchProducts');
    fetchProducts();
  }, [fetchProducts]);

  const filteredProducts = (products || []).filter(product => {
    const matchesPrice = priceFilter ? product.price <= parseFloat(priceFilter) : true;
    const matchesCategory = selectedCategory ? product.category?.name === selectedCategory : true;
    return matchesPrice && matchesCategory;
  });

  // console.log('Inicio: productos filtrados calculados:', filteredProducts.length, 'productos de', products?.length || 0, 'total');

  const handleAddToCart = async (product: any) => {
    try {
      // console.log('Inicio: handleAddToCart llamado con producto:', product);
      await addItem({
        id: product.id,
        name: product.name,
        price: product.price,
        quantity: 1,
      });
      addNotification({
        type: 'success',
        message: 'Producto agregado al carrito',
      });
      // console.log('Inicio: Producto agregado al carrito');
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

  if (loading) {
    // console.log('Inicio: Renderizando estado de carga');
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
    // console.log('Inicio: Renderizando estado de error:', error);
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="text-destructive text-xl mb-4">Error al cargar productos</div>
          <p className="text-muted-foreground">{error}</p>
          <button
            onClick={() => fetchProducts()}
            className="mt-4 bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
          >
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  // console.log('Inicio: Renderizando contenido principal con', filteredProducts.length, 'productos filtrados');

  return (
    <div className="min-h-screen bg-background">
      {/* Sección Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-background via-background to-muted/20">
        <div className="container mx-auto px-4 py-24 md:py-32">
          <div className="max-w-4xl mx-auto text-center">
            <h1 className="text-4xl md:text-6xl font-bold text-foreground mb-6 text-balance">
              Descubre Productos Increíbles
            </h1>
            <p className="text-xl text-muted-foreground mb-8 text-pretty max-w-2xl mx-auto">
              Compra las últimas tendencias y encuentra todo lo que necesitas en nuestra colección cuidadosamente curada de productos premium.
            </p>
          </div>
        </div>

        {/* Decorative elements */}
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-primary/5 rounded-full blur-3xl -z-10 pointer-events-none" />
        <div className="absolute top-1/4 right-1/4 w-64 h-64 bg-accent/10 rounded-full blur-2xl -z-10 pointer-events-none" />
      </section>

      {/* Categorías */}
      <section className="py-12 bg-muted/30">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold mb-8 text-center text-foreground">Categorías Populares</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-6">
            {['Electrónica', 'Ropa', 'Hogar', 'Deportes', 'Libros', 'Juguetes'].map((category) => (
              <div
                key={category}
                onClick={() => {
                  setSelectedCategory(selectedCategory === category ? null : category);
                }}
                className={`bg-card p-6 rounded-lg text-center cursor-pointer transition-all duration-300 shadow-sm hover:shadow-md transform hover:-translate-y-1 border ${selectedCategory === category
                  ? 'border-primary bg-primary/5 ring-2 ring-primary/20'
                  : 'border-border hover:bg-card/80'
                  }`}
              >
                <div className="w-16 h-16 bg-primary/10 rounded-full mx-auto mb-3 flex items-center justify-center text-2xl">
                  {category === 'Electrónica' ? <Laptop className="w-8 h-8 text-primary" /> :
                    category === 'Ropa' ? <Shirt className="w-8 h-8 text-primary" /> :
                      category === 'Hogar' ? <HomeIcon className="w-8 h-8 text-primary" /> :
                        category === 'Deportes' ? <Zap className="w-8 h-8 text-primary" /> :
                          category === 'Libros' ? <BookOpen className="w-8 h-8 text-primary" /> : <Gift className="w-8 h-8 text-primary" />}
                </div>
                <p className="font-semibold text-foreground">{category}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Productos */}
      <section className="py-12 bg-background">
        <div className="container mx-auto px-4">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8">
            <h2 className="text-3xl font-bold text-foreground">
              {selectedCategory ? `Productos de ${selectedCategory}` : 'Productos Destacados'}
            </h2>
            <div className="flex items-center space-x-4">
              <label className="text-foreground font-medium">Filtrar por precio:</label>
              <select
                value={priceFilter}
                onChange={(e) => {
                  setPriceFilter(e.target.value);
                  setSelectedCategory(null); // Clear category when filtering by price
                }}
                className="px-4 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring bg-background text-foreground"
              >
                <option value="">Todos los precios</option>
                <option value="50">Hasta $50</option>
                <option value="100">Hasta $100</option>
                <option value="200">Hasta $200</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
            {filteredProducts.map((product) => (
              <div key={product.id} className="bg-card rounded-lg shadow-sm overflow-hidden hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 border border-border">
                <Link to={`/product/${product.id}`}>
                  <div className="aspect-square bg-muted flex items-center justify-center relative overflow-hidden">
                    <img
                      src={product.imageUrl || '/placeholder-product.jpg'}
                      alt={product.name}
                      className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                      onError={(e) => {
                        e.currentTarget.src = '/placeholder-product.jpg';
                      }}
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

          {filteredProducts.length === 0 && (
            <div className="text-center py-16">
              <div className="text-6xl mb-4"></div>
              <p className="text-muted-foreground text-xl">
                {selectedCategory
                  ? `No hay productos disponibles en la categoría "${selectedCategory}".`
                  : 'No se encontraron productos con los criterios de búsqueda.'
                }
              </p>
              <button
                onClick={() => {
                  setPriceFilter('');
                  setSelectedCategory(null);
                  fetchProducts();
                }}
                className="mt-4 bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
              >
                Ver todos los productos
              </button>
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default Home;
