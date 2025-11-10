import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { publicAPI } from '../services/api';
import { Laptop, Shirt, HomeIcon, Zap, BookOpen, Gift, Folder } from 'lucide-react';

interface Category {
    id: number;
    name: string;
    description?: string;
    imageUrl?: string;
}

const Categories = () => {
    // console.log('Categorías: Componente renderizado');
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // console.log('Categorías: Estado - categorías:', categories, 'cargando:', loading, 'error:', error);

    useEffect(() => {
        // console.log('Categorías: useEffect activado, llamando a fetchCategories');
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        // console.log('Categorías: fetchCategories llamado');
        setLoading(true);
        setError(null);
        try {
            // console.log('Categorías: Llamando a publicAPI.getCategories()');
            const response = await publicAPI.getCategories();
            // console.log('Categorías: Respuesta de API recibida:', response.data);
            setCategories(response.data || []);
            setLoading(false);
            // console.log('Categorías: Categorías establecidas:', response.data || []);
        } catch (error: any) {
            // Error handling removed for production mode
            setError(error.response?.data?.message || 'Error al obtener categorías');
            setLoading(false);
        }
    };

    if (loading) {
        // console.log('Categorías: Renderizando estado de carga');
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary mx-auto mb-4"></div>
                    <p className="text-xl text-muted-foreground">Cargando categorías...</p>
                </div>
            </div>
        );
    }

    if (error) {
        // console.log('Categorías: Renderizando estado de error:', error);
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <div className="text-center">
                    <div className="text-destructive text-xl mb-4">Error al cargar categorías</div>
                    <p className="text-muted-foreground">{error}</p>
                    <button
                        onClick={fetchCategories}
                        className="mt-4 bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
                    >
                        Reintentar
                    </button>
                </div>
            </div>
        );
    }

    // console.log('Categorías: Renderizando contenido principal con', categories.length, 'categorías');

    return (
        <div className="min-h-screen bg-background">
            {/* Encabezado */}
            <section className="py-12 bg-muted/30">
                <div className="container mx-auto px-4">
                    <div className="max-w-4xl mx-auto text-center">
                        <h1 className="text-4xl md:text-6xl font-bold text-foreground mb-6 text-balance">
                            Todas las Categorías
                        </h1>
                        <p className="text-xl text-muted-foreground mb-8 text-pretty max-w-2xl mx-auto">
                            Explora nuestras categorías y encuentra exactamente lo que buscas.
                        </p>
                    </div>
                </div>
            </section>

            {/* Categorías */}
            <section className="py-12 bg-background">
                <div className="container mx-auto px-4">
                    <div className="mb-8">
                        <h2 className="text-3xl font-bold text-foreground">
                            Categorías Disponibles ({categories.length})
                        </h2>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
                        {categories.map((category) => (
                            <Link
                                key={category.id}
                                to={`/products?category=${category.name}`}
                                className="bg-card rounded-lg shadow-sm overflow-hidden hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 border border-border group"
                            >
                                <div className="aspect-square bg-muted flex items-center justify-center relative overflow-hidden">
                                    <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                        {category.name.toLowerCase().includes('electrónica') || category.name.toLowerCase().includes('electronica') ? <Laptop className="w-8 h-8 text-primary" /> :
                                            category.name.toLowerCase().includes('ropa') ? <Shirt className="w-8 h-8 text-primary" /> :
                                                category.name.toLowerCase().includes('hogar') ? <HomeIcon className="w-8 h-8 text-primary" /> :
                                                    category.name.toLowerCase().includes('deporte') ? <Zap className="w-8 h-8 text-primary" /> :
                                                        category.name.toLowerCase().includes('libro') ? <BookOpen className="w-8 h-8 text-primary" /> :
                                                            category.name.toLowerCase().includes('juguete') ? <Gift className="w-8 h-8 text-primary" /> :
                                                                <Folder className="w-8 h-8 text-primary" />}
                                    </div>
                                    <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-10 transition-all duration-300"></div>
                                </div>
                                <div className="p-4">
                                    <h3 className="font-bold text-lg mb-2 text-center hover:text-primary transition-colors text-foreground">
                                        {category.name}
                                    </h3>
                                    {category.description && (
                                        <p className="text-muted-foreground text-sm text-center line-clamp-2">
                                            {category.description}
                                        </p>
                                    )}
                                </div>
                            </Link>
                        ))}
                    </div>

                    {categories.length === 0 && (
                        <div className="text-center py-16">
                            <Folder className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
                            <p className="text-muted-foreground text-xl">
                                No hay categorías disponibles en este momento.
                            </p>
                            <button
                                onClick={fetchCategories}
                                className="mt-4 bg-primary text-primary-foreground px-6 py-2 rounded-md hover:bg-primary/90 transition-colors"
                            >
                                Recargar categorías
                            </button>
                        </div>
                    )}
                </div>
            </section>
        </div>
    );
};

export default Categories;
