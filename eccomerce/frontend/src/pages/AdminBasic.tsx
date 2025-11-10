import { useEffect, useState } from 'react';
import { useUserStore } from '../stores/userStore';
import { useTheme } from '../contexts/ThemeContext';
import { Link } from 'react-router-dom';

const AdminBasic = () => {
    const { user } = useUserStore();
    const { theme, setTheme } = useTheme();

    const [showAdminPage, setShowAdminPage] = useState(false);

    // Verificar si el usuario es administrador antes de renderizar el contenido
    useEffect(() => {
        if (user?.role === 'ADMIN') {
            setShowAdminPage(true);
        }
    }, [user]);

    // Si el usuario no es admin, mostrar mensaje de acceso denegado
    if (!showAdminPage) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center p-8 bg-white dark:bg-gray-800 rounded-lg shadow-lg">
                    <h1 className="text-2xl font-bold mb-4">Acceso Denegado</h1>
                    <p className="mb-6">No tienes permisos para acceder a esta página.</p>
                    <Link
                        to="/"
                        className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                    >
                        Volver al Inicio
                    </Link>
                </div>
            </div>
        );
    }

    // Cambiar el tema entre claro y oscuro
    const toggleTheme = () => {
        setTheme(theme === 'dark' ? 'light' : 'dark');
    };

    return (
        <div className="min-h-screen bg-gray-100 dark:bg-gray-900 text-gray-900 dark:text-gray-100">
            <div className="container mx-auto px-4 py-8">
                <header className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md mb-8 flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold">Panel de Administración (Modo Básico)</h1>
                        <p className="text-gray-600 dark:text-gray-300">Versión simplificada para resolver problemas de visualización</p>
                    </div>

                    <button
                        onClick={toggleTheme}
                        className="p-2 bg-gray-200 dark:bg-gray-700 rounded-full"
                        aria-label="Cambiar tema"
                    >
                        {theme === 'dark' ? (
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                            </svg>
                        ) : (
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                            </svg>
                        )}
                    </button>
                </header>

                <main>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Sección Productos */}
                        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                            <h2 className="text-2xl font-bold mb-4">Productos</h2>
                            <p className="mb-4">Administra los productos de tu tienda</p>
                            <div className="flex space-x-2">
                                <button className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700">
                                    Agregar Producto
                                </button>
                                <button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                                    Ver Todos
                                </button>
                            </div>
                        </div>

                        {/* Sección Órdenes */}
                        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                            <h2 className="text-2xl font-bold mb-4">Órdenes</h2>
                            <p className="mb-4">Administra los pedidos de tus clientes</p>
                            <div className="flex space-x-2">
                                <button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                                    Ver Órdenes
                                </button>
                            </div>
                        </div>

                        {/* Sección Usuarios */}
                        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                            <h2 className="text-2xl font-bold mb-4">Usuarios</h2>
                            <p className="mb-4">Administra los usuarios registrados</p>
                            <div className="flex space-x-2">
                                <button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                                    Ver Usuarios
                                </button>
                            </div>
                        </div>

                        {/* Sección Auditoría */}
                        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                            <h2 className="text-2xl font-bold mb-4">Auditoría</h2>
                            <p className="mb-4">Revisa los registros de actividad</p>
                            <div className="flex space-x-2">
                                <button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                                    Ver Registros
                                </button>
                            </div>
                        </div>
                    </div>

                    <div className="mt-8 bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md">
                        <h2 className="text-2xl font-bold mb-4">Estado de la Tienda</h2>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            <div className="bg-green-100 dark:bg-green-900 p-4 rounded-lg text-center">
                                <span className="block text-2xl font-bold">Productos</span>
                                <span className="text-4xl font-bold">--</span>
                            </div>
                            <div className="bg-blue-100 dark:bg-blue-900 p-4 rounded-lg text-center">
                                <span className="block text-2xl font-bold">Órdenes</span>
                                <span className="text-4xl font-bold">--</span>
                            </div>
                            <div className="bg-purple-100 dark:bg-purple-900 p-4 rounded-lg text-center">
                                <span className="block text-2xl font-bold">Usuarios</span>
                                <span className="text-4xl font-bold">--</span>
                            </div>
                        </div>
                    </div>

                    <div className="mt-8 p-4 bg-yellow-100 dark:bg-yellow-900 rounded-lg">
                        <p className="text-center">
                            Estás usando una versión simplificada del panel de administración.
                            Para acceder a todas las funcionalidades, necesitarás solucionar los problemas
                            en la versión completa.
                        </p>
                    </div>
                </main>
            </div>
        </div>
    );
};

export default AdminBasic;
