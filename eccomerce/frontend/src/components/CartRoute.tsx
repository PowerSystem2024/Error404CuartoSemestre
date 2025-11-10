import { ReactNode, useEffect, useState } from 'react';
import { useUserStore } from '../stores/userStore';

interface CartRouteProps {
    children: ReactNode;
}

const CartRoute = ({ children }: CartRouteProps) => {
    const { user, token, initializeFromStorage } = useUserStore();
    const [isAuthChecked, setIsAuthChecked] = useState(false);

    // Intentar restaurar autenticación desde localStorage si no hay token
    useEffect(() => {
        if (!token || !user) {
            initializeFromStorage();
        }
        setIsAuthChecked(true);
    }, [token, user, initializeFromStorage]);

    // Mostrar un indicador de carga mientras verificamos la autenticación
    if (!isAuthChecked) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-blue-500"></div>
            </div>
        );
    }

    // Permitir acceso completo al carrito tanto para usuarios logueados como no logueados
    return <>{children}</>;
};

export default CartRoute;
