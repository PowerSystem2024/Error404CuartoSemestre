import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useUserStore } from '../stores/userStore';

const Login = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ identifier?: string, password?: string }>({});
  const { login, loading, error, clearError } = useUserStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  // Revisar si hay un mensaje o una ruta de redirección en el estado
  useEffect(() => {
    const state = location.state as { from?: string; message?: string } | null;
    if (state?.message) {
      setStatusMessage(state.message);
    }
  }, [location]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    setFieldErrors({});

    // console.log('🔐 InicioSesión: Intentando iniciar sesión con identificador:', identifier);

    // Validar campos
    const errors: { identifier?: string, password?: string } = {};

    if (!identifier.trim()) {
      errors.identifier = 'El email o nombre de usuario es obligatorio';
    }

    if (!password.trim()) {
      errors.password = 'La contraseña es obligatoria';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      // Error message is already shown in the form fields
      return;
    }

    const result = await login(identifier, password);

    if (result.success) {
      // Revisar si hay una ruta de redirección en el estado
      const state = location.state as { from?: string } | null;
      if (state?.from) {
        // console.log('🔐 Login: Redirigiendo a ruta previa:', state.from);
        navigate(state.from);
      } else {
        // console.log('🔐 Login: Redirigiendo al inicio');
        navigate('/');
      }
    } else {
      // console.log('❌ InicioSesión: FALLÓ - Error:', result.error);
      // El error ya está establecido en el store, se mostrará
    }
  };

  return (
    <div className="container mx-auto p-4 max-w-md">
      <h1 className="text-2xl font-bold mb-4">Iniciar Sesión</h1>

      {statusMessage && (
        <div className="bg-blue-100 border border-blue-300 text-blue-800 px-4 py-3 rounded-md mb-4">
          <p>{statusMessage}</p>
        </div>
      )}

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded-md mb-4">
          <strong>Error:</strong> {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-foreground mb-1">Email o Nombre de Usuario</label>
          <input
            type="text"
            value={identifier}
            onChange={(e) => {
              setIdentifier(e.target.value);
              if (fieldErrors.identifier) {
                setFieldErrors(prev => ({ ...prev, identifier: undefined }));
              }
            }}
            className={`w-full p-2 border rounded-md bg-background text-foreground ${fieldErrors.identifier ? 'border-destructive bg-destructive/10' : 'border-border'
              } focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors`}
            placeholder="Ingresa tu email o nombre de usuario"
            required
          />
          {fieldErrors.identifier && (
            <p className="text-destructive text-sm mt-1">{fieldErrors.identifier}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-foreground mb-1">Contraseña</label>
          <input
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (fieldErrors.password) {
                setFieldErrors(prev => ({ ...prev, password: undefined }));
              }
            }}
            className={`w-full p-2 border rounded-md bg-background text-foreground ${fieldErrors.password ? 'border-destructive bg-destructive/10' : 'border-border'
              } focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors`}
            placeholder="Ingresa tu contraseña"
            required
          />
          {fieldErrors.password && (
            <p className="text-destructive text-sm mt-1">{fieldErrors.password}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-primary text-primary-foreground p-2 rounded-md disabled:opacity-50 hover:bg-primary/90 transition-colors font-medium"
        >
          {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
        </button>
      </form>

      <div className="mt-4 text-center">
        <p className="text-sm text-muted-foreground mb-2">
          <a href="/forgot-password" className="text-primary hover:text-primary/80 font-medium transition-colors">
            ¿Olvidaste tu contraseña?
          </a>
        </p>
        <p className="text-sm text-muted-foreground">
          ¿No tienes cuenta?{' '}
          <a href="/register" className="text-primary hover:text-primary/80 font-medium transition-colors">
            Regístrate aquí
          </a>
        </p>
      </div>
    </div>
  );
};

export default Login;