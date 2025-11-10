import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../services/api';

const VerifyEmail = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');

    const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'already-verified'>('loading');
    const [message, setMessage] = useState('');

    useEffect(() => {
        const verifyEmail = async () => {
            if (!token) {
                setStatus('error');
                setMessage('Token de verificación no encontrado');
                return;
            }

            try {
                console.log('🔍 Verificando token:', token.substring(0, 10) + '...');
                const response = await api.get(`/auth/verify-email?token=${token}`);
                console.log('✅ Respuesta del servidor:', response.data);

                if (response.data.message?.includes('ya ha sido verificado') ||
                    response.data.message?.includes('already verified')) {
                    setStatus('already-verified');
                    setMessage('Tu email ya estaba verificado anteriormente');
                } else {
                    setStatus('success');
                    setMessage('¡Email verificado exitosamente!');
                }

                // Redirigir al login después de 3 segundos
                setTimeout(() => {
                    navigate('/login');
                }, 3000);

            } catch (error: any) {
                console.error('❌ Error verificando email:', error);
                setStatus('error');
                if (error.response?.data?.message) {
                    setMessage(error.response.data.message);
                } else {
                    setMessage('Error al verificar el email. El token puede haber expirado.');
                }
            }
        };

        verifyEmail();
    }, [token, navigate]);

    return (
        <div className="min-h-screen bg-background flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8">
                <div className="text-center">
                    {status === 'loading' && (
                        <>
                            <div className="mx-auto h-20 w-20 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
                            <h2 className="mt-6 text-3xl font-bold text-foreground">
                                Verificando email...
                            </h2>
                            <p className="mt-2 text-sm text-muted-foreground">
                                Por favor espera mientras verificamos tu cuenta
                            </p>
                        </>
                    )}

                    {status === 'success' && (
                        <>
                            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-green-100 dark:bg-green-900">
                                <svg
                                    className="h-12 w-12 text-green-600 dark:text-green-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M5 13l4 4L19 7"
                                    />
                                </svg>
                            </div>
                            <h2 className="mt-6 text-3xl font-bold text-green-600 dark:text-green-400">
                                ¡Email verificado!
                            </h2>
                            <p className="mt-2 text-sm text-muted-foreground">
                                {message}
                            </p>
                            <p className="mt-4 text-sm text-muted-foreground">
                                Serás redirigido al login en unos segundos...
                            </p>
                        </>
                    )}

                    {status === 'already-verified' && (
                        <>
                            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900">
                                <svg
                                    className="h-12 w-12 text-blue-600 dark:text-blue-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                                    />
                                </svg>
                            </div>
                            <h2 className="mt-6 text-3xl font-bold text-blue-600 dark:text-blue-400">
                                Email ya verificado
                            </h2>
                            <p className="mt-2 text-sm text-muted-foreground">
                                {message}
                            </p>
                            <p className="mt-4 text-sm text-muted-foreground">
                                Serás redirigido al login en unos segundos...
                            </p>
                        </>
                    )}

                    {status === 'error' && (
                        <>
                            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-red-100 dark:bg-red-900">
                                <svg
                                    className="h-12 w-12 text-red-600 dark:text-red-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M6 18L18 6M6 6l12 12"
                                    />
                                </svg>
                            </div>
                            <h2 className="mt-6 text-3xl font-bold text-red-600 dark:text-red-400">
                                Error al verificar
                            </h2>
                            <p className="mt-2 text-sm text-muted-foreground">
                                {message}
                            </p>
                            <div className="mt-6">
                                <Link
                                    to="/login"
                                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
                                >
                                    Ir al login
                                </Link>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default VerifyEmail;
