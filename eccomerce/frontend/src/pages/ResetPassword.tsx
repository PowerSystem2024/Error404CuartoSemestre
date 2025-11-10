import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../services/api';
import { useNotificationStore } from '../stores/notificationStore';

const ResetPassword: React.FC = () => {
    const navigate = useNavigate();
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [token, setToken] = useState('');
    const { addNotification } = useNotificationStore();

    useEffect(() => {
        // Obtener el token directamente de la query string (sin parámetro nombrado)
        const queryString = window.location.search;
        const tokenParam = queryString.startsWith('?') ? queryString.substring(1) : '';

        if (!tokenParam) {
            addNotification({
                type: 'error',
                message: 'Token de restablecimiento no válido o expirado'
            });
            navigate('/login');
            return;
        }
        setToken(tokenParam);
    }, [navigate, addNotification]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!newPassword.trim()) {
            addNotification({
                type: 'error',
                message: 'Por favor ingresa la nueva contraseña'
            });
            return;
        }

        if (newPassword.length < 6) {
            addNotification({
                type: 'error',
                message: 'La contraseña debe tener al menos 6 caracteres'
            });
            return;
        }

        if (newPassword !== confirmPassword) {
            addNotification({
                type: 'error',
                message: 'Las contraseñas no coinciden'
            });
            return;
        }

        setIsLoading(true);

        try {
            await authAPI.resetPassword(token, newPassword.trim());
            addNotification({
                type: 'success',
                message: 'Contraseña restablecida exitosamente'
            });
            navigate('/login');
        } catch (error: any) {
            // Error handling removed for production mode
            addNotification({
                type: 'error',
                message: error.response?.data?.message || 'Error al restablecer la contraseña. El token puede haber expirado.'
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8">
                <div>
                    <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
                        Restablecer contraseña
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600 dark:text-gray-400">
                        Ingresa tu nueva contraseña.
                    </p>
                </div>

                <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
                    <div className="space-y-4">
                        <div>
                            <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                                Nueva contraseña
                            </label>
                            <input
                                id="newPassword"
                                name="newPassword"
                                type="password"
                                autoComplete="new-password"
                                required
                                className="mt-1 appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 placeholder-gray-500 dark:placeholder-gray-400 text-gray-900 dark:text-white bg-white dark:bg-gray-800 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
                                placeholder="Nueva contraseña"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                disabled={isLoading}
                            />
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                                Confirmar contraseña
                            </label>
                            <input
                                id="confirmPassword"
                                name="confirmPassword"
                                type="password"
                                autoComplete="new-password"
                                required
                                className="mt-1 appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 placeholder-gray-500 dark:placeholder-gray-400 text-gray-900 dark:text-white bg-white dark:bg-gray-800 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
                                placeholder="Confirmar contraseña"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                disabled={isLoading}
                            />
                        </div>
                    </div>

                    <div>
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isLoading ? (
                                <div className="flex items-center">
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                                    Restableciendo...
                                </div>
                            ) : (
                                'Restablecer contraseña'
                            )}
                        </button>
                    </div>

                    <div className="text-center">
                        <button
                            type="button"
                            onClick={() => navigate('/login')}
                            className="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
                        >
                            Volver al inicio de sesión
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ResetPassword;