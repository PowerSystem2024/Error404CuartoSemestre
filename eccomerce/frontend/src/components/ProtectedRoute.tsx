import { ReactNode, useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useUserStore } from '../stores/userStore';

interface ProtectedRouteProps {
  children: ReactNode;
  requireAdmin?: boolean;
}

const ProtectedRoute = ({ children, requireAdmin = false }: ProtectedRouteProps) => {
  const { user, token, initializeFromStorage } = useUserStore();
  const location = useLocation();
  const [isAuthChecked, setIsAuthChecked] = useState(false);

  // Intentar restaurar autenticación desde localStorage si no hay token
  useEffect(() => {
    if (!token || !user) {
      // console.log('ProtectedRoute: Intentando restaurar sesión desde localStorage');
      initializeFromStorage();
    }
    setIsAuthChecked(true);
  }, [token, user, initializeFromStorage]);

  // console.log('ProtectedRoute: Verificando acceso', {
  //   path: location.pathname,
  //   hasToken: !!token,
  //   user: user,
  //   requireAdmin,
  //   userRole: user?.role,
  //   roleCheck: requireAdmin && user?.role !== 'ADMIN',
  //   isAuthChecked
  // });

  // Mostrar un indicador de carga mientras verificamos la autenticación
  if (!isAuthChecked) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  // Verificar si el usuario está logueado
  if (!token) {
    // console.log('ProtectedRoute: ❌ No hay token, redirigiendo a login');
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Verificar si requiere rol admin
  if (requireAdmin && user?.role !== 'ADMIN') {
    // console.log('ProtectedRoute: ❌ Usuario no es admin, acceso denegado');
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-red-600 text-2xl"></span>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Acceso Denegado</h2>
          <p className="text-gray-600 mb-6">
            No tienes permisos para acceder a esta sección. Se requiere rol de administrador.
          </p>
          <button
            onClick={() => window.history.back()}
            className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition-colors"
          >
            Volver
          </button>
        </div>
      </div>
    );
  }

  // console.log('ProtectedRoute: ✅ Acceso permitido');
  return <>{children}</>;
};

export default ProtectedRoute;
