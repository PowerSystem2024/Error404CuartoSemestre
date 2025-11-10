import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUserStore } from '../stores/userStore';

const Register = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ username?: string, email?: string, password?: string }>({});
  const { register, loading, error, clearError } = useUserStore();
  const navigate = useNavigate();

  const validateEmail = (email: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validatePassword = (password: string) => {
    return password.length >= 6;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    setFieldErrors({});

    // console.log('📝 Registro: Intentando registro con email:', email, 'usuario:', username);

    // Validar campos
    const errors: { username?: string, email?: string, password?: string } = {};

    if (!username.trim()) {
      errors.username = 'El nombre de usuario es obligatorio';
    } else if (username.length < 3) {
      errors.username = 'El nombre de usuario debe tener al menos 3 caracteres';
    }

    if (!email.trim()) {
      errors.email = 'El email es obligatorio';
    } else if (!validateEmail(email)) {
      errors.email = 'Ingresa un email válido';
    }

    if (!password.trim()) {
      errors.password = 'La contraseña es obligatoria';
    } else if (!validatePassword(password)) {
      errors.password = 'La contraseña debe tener al menos 6 caracteres';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      // Error messages are already shown in the form fields
      return;
    }

    try {
      const result = await register(username, email, password);
      if (result.success) {
        navigate('/');
      } else {
        // El error ya está manejado en el store
        // console.log('❌ Registro: Registro falló con error:', result.error);
      }
    } catch (error: any) {
      // console.log('❌ Registro: FALLÓ - Error:', error);

      // El error ya está manejado en el store, pero podemos agregar lógica adicional aquí si es necesario
      // El mensaje de error específico ya se muestra en el componente
    }
  };

  return (
    <div className="container mx-auto p-4 max-w-md">
      <h1 className="text-2xl font-bold mb-4">Registro</h1>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          <strong>Error:</strong> {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Nombre de Usuario</label>
          <input
            type="text"
            value={username}
            onChange={(e) => {
              setUsername(e.target.value);
              if (fieldErrors.username) {
                setFieldErrors(prev => ({ ...prev, username: undefined }));
              }
            }}
            className={`w-full p-2 border rounded ${fieldErrors.username ? 'border-red-500 bg-red-50' : 'border-gray-300'}
              bg-white text-gray-900 placeholder-gray-500 caret-blue-600
              focus:outline-none focus:ring-2 focus:ring-blue-500
              dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400`}
            placeholder="Ingresa tu nombre de usuario"
            required
          />
          {fieldErrors.username && (
            <p className="text-red-500 text-sm mt-1">{fieldErrors.username}</p>
          )}
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (fieldErrors.email) {
                setFieldErrors(prev => ({ ...prev, email: undefined }));
              }
            }}
            className={`w-full p-2 border rounded ${fieldErrors.email ? 'border-red-500 bg-red-50' : 'border-gray-300'
              }
              bg-white text-gray-900 placeholder-gray-500 caret-blue-600
              focus:outline-none focus:ring-2 focus:ring-blue-500
              dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400`}
            placeholder="Ingresa tu email"
            required
          />
          {fieldErrors.email && (
            <p className="text-red-500 text-sm mt-1">{fieldErrors.email}</p>
          )}
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Contraseña</label>
          <input
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (fieldErrors.password) {
                setFieldErrors(prev => ({ ...prev, password: undefined }));
              }
            }}
            className={`w-full p-2 border rounded ${fieldErrors.password ? 'border-red-500 bg-red-50' : 'border-gray-300'
              }
              bg-white text-gray-900 placeholder-gray-500 caret-blue-600
              focus:outline-none focus:ring-2 focus:ring-blue-500
              dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400`}
            placeholder="Ingresa tu contraseña (mínimo 6 caracteres)"
            required
          />
          {fieldErrors.password && (
            <p className="text-red-500 text-sm mt-1">{fieldErrors.password}</p>
          )}
        </div>
        <button type="submit" disabled={loading} className="w-full bg-blue-500 text-white p-2 rounded disabled:opacity-50 hover:bg-blue-600 transition-colors">
          {loading ? 'Registrando...' : 'Registrarse'}
        </button>
      </form>

      <div className="mt-4 text-center">
        <p className="text-sm text-gray-600">
          ¿Ya tienes cuenta?{' '}
          <a href="/login" className="text-blue-500 hover:text-blue-700">
            Inicia sesión aquí
          </a>
        </p>
      </div>
    </div>
  );
};

export default Register;