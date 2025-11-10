import { create } from 'zustand';
import { authAPI, decodeJWT, isTokenExpired } from '../services/api';

interface User {
    id: number;
    name: string;
    email: string;
    role: string;
}

interface UserStore {
    user: User | null;
    token: string | null;
    loading: boolean;
    error: string | null;
    login: (identifier: string, password: string) => Promise<{ success: boolean; user?: User; error?: string }>;
    register: (username: string, email: string, password: string) => Promise<{ success: boolean; error?: string }>;
    logout: () => void;
    clearError: () => void;
    initializeFromStorage: () => void;
    debugToken: () => void;
}

// Inicializar desde localStorage
const initializeFromStorage = () => {
    // console.log('👤 TiendaUsuario: Inicializando desde localStorage');
    const token = localStorage.getItem('token');
    // console.log('👤 TiendaUsuario: Token en localStorage:', token ? 'existe' : 'null');

    if (token) {
        // Check if token is expired
        if (isTokenExpired(token)) {
            // Warning removed for production mode
            localStorage.removeItem('token');
            return {
                user: null,
                token: null,
                loading: false,
                error: null,
            };
        }

        try {
            const decoded = decodeJWT(token);
            // console.log('👤 TiendaUsuario: Token decodificado:', decoded);

            if (decoded) {
                const fullName = `${decoded.firstName || ''} ${decoded.lastName || ''}`.trim();
                const displayName = fullName || decoded.sub || 'Usuario';

                const user: User = {
                    id: decoded.id,
                    name: displayName,
                    email: decoded.email || decoded.sub || '',
                    role: (decoded.role || 'USER').replace('ROLE_', '') // Remove ROLE_ prefix
                };

                // console.log('👤 TiendaUsuario: Usuario inicializado desde token:', user);
                return {
                    user,
                    token,
                    loading: false,
                    error: null,
                };
            }
        } catch (error) {
            // Error handling removed for production mode
            localStorage.removeItem('token'); // Limpiar token inválido
        }
    }

    return {
        user: null,
        token: null,
        loading: false,
        error: null,
    };
};

export const useUserStore = create<UserStore>((set) => ({
    ...initializeFromStorage(),
    login: async (identifier: string, password: string) => {
        // console.log('👤 TiendaUsuario: 🔐 INTENTO DE INICIO DE SESIÓN - Identificador:', identifier);
        set({ loading: true, error: null });
        try {
            // console.log('👤 TiendaUsuario: 📡 Llamando a authAPI.login...');
            const response = await authAPI.login(identifier, password);
            // console.log('👤 TiendaUsuario: ✅ Respuesta de API recibida:', response.data);

            const { token } = response.data;
            // console.log('👤 TiendaUsuario: 🔑 Token recibido:', token ? 'SÍ' : 'NO');

            // Decodificar JWT para obtener información del usuario
            const decoded = decodeJWT(token);
            // console.log('👤 TiendaUsuario: 🔓 JWT decodificado completo:', decoded);
            // console.log('👤 TiendaUsuario: Campos disponibles en JWT:', Object.keys(decoded || {}));
            // console.log('👤 TiendaUsuario: 📋 Subject (username):', decoded.sub);
            // console.log('👤 TiendaUsuario: 🆔 ID:', decoded.id);
            // console.log('👤 TiendaUsuario: 👤 FirstName:', decoded.firstName);
            // console.log('👤 TiendaUsuario: 👤 LastName:', decoded.lastName);
            // console.log('👤 TiendaUsuario: 👤 Role:', decoded.role);

            if (!decoded) {
                throw new Error('Token inválido recibido');
            }

            // Crear objeto usuario desde claims del JWT
            // El backend envía: subject=username, firstName, lastName, id
            const fullName = `${decoded.firstName || ''} ${decoded.lastName || ''}`.trim();
            const displayName = fullName || decoded.sub || 'Usuario';

            const user: User = {
                id: decoded.id,
                name: displayName,
                email: decoded.email || decoded.sub || '', // El backend no envía email, usar username como fallback
                role: (decoded.role || 'USER').replace('ROLE_', '') // Remove ROLE_ prefix
            };

            // console.log('👤 TiendaUsuario: 👤 Nombre completo del JWT:', fullName);
            // console.log('👤 TiendaUsuario: 👤 Nombre para mostrar:', displayName);
            // console.log('👤 TiendaUsuario: 📧 Email/username del JWT:', decoded.sub);

            // console.log('👤 TiendaUsuario: 👤 Datos del usuario:', user);

            localStorage.setItem('token', token);
            set({ user, token, loading: false });

            // console.log('👤 TiendaUsuario: 🎉 ¡INICIO DE SESIÓN EXITOSO! Usuario:', user.name, 'Token guardado en localStorage');
            // console.log('👤 TiendaUsuario: 🔄 Debería redirigir a la página principal ahora');

            return { success: true, user };

        } catch (error: any) {
            // Error handling removed for production mode

            // Extraer mensaje de error del backend
            let errorMessage = 'Error al iniciar sesión';

            if (error.response?.data?.message) {
                errorMessage = error.response.data.message;
            } else if (error.response?.status === 401) {
                errorMessage = 'Credenciales inválidas';
            } else if (error.response?.status === 400) {
                errorMessage = 'Datos inválidos. Verifica la información ingresada.';
            } else if (error.message) {
                errorMessage = error.message;
            }

            set({ error: errorMessage, loading: false });

            // console.log('👤 TiendaUsuario: 🚫 ERROR DE INICIO DE SESIÓN:', errorMessage);
            return { success: false, error: errorMessage };
        }
    },
    register: async (username: string, email: string, password: string) => {
        // console.log('👤 TiendaUsuario: 📝 INTENTO DE REGISTRO - Usuario:', username, 'Email:', email);
        set({ loading: true, error: null });
        try {
            // console.log('👤 TiendaUsuario: 📡 Llamando a authAPI.register...');
            // const response = await authAPI.register(username, email, password);
            await authAPI.register(username, email, password);
            // console.log('👤 TiendaUsuario: ✅ Respuesta de API de registro recibida:', response.data);

            // Para registro, no esperamos token inmediatamente, solo confirmación
            set({ loading: false });

            // console.log('👤 TiendaUsuario: 🎉 ¡REGISTRO EXITOSO! Usuario registrado, email de verificación enviado');
            return { success: true };

        } catch (error: any) {
            // Error handling removed for production mode

            // Extraer mensaje de error del backend
            let errorMessage = 'Error al registrarse';

            if (error.response?.data?.message) {
                errorMessage = error.response.data.message;
            } else if (error.response?.status === 400) {
                // Manejar errores de validación específicos
                if (error.response.data) {
                    // Si es un objeto con errores de campo
                    const fieldErrors = error.response.data;
                    if (typeof fieldErrors === 'object') {
                        const errorMessages = Object.values(fieldErrors).join(', ');
                        errorMessage = errorMessages || 'Datos inválidos. Verifica la información ingresada.';
                    } else {
                        errorMessage = error.response.data;
                    }
                } else {
                    errorMessage = 'Datos inválidos. Verifica la información ingresada.';
                }
            } else if (error.response?.status === 409) {
                errorMessage = 'El usuario ya existe. Intenta con otro email o nombre de usuario.';
            } else if (error.response?.status === 500) {
                errorMessage = 'Error del servidor. Inténtalo de nuevo más tarde.';
            } else if (error.message) {
                errorMessage = error.message;
            }

            set({ error: errorMessage, loading: false });

            // console.log('👤 TiendaUsuario: 🚫 ERROR DE REGISTRO:', errorMessage);
            throw new Error(errorMessage); // Re-throw para que el componente pueda manejar
        }
    },
    logout: () => {
        // console.log('👤 TiendaUsuario: cerrar sesión llamado');
        localStorage.removeItem('token');
        set({ user: null, token: null });
    },
    clearError: () => set({ error: null }),
    initializeFromStorage: () => {
        // console.log('👤 TiendaUsuario: inicialización manual desde localStorage llamada');
        const state = initializeFromStorage();
        set(state);
    },
    debugToken: () => {
        // console.log('🔍 Debug: Inspeccionando token JWT');
        const token = localStorage.getItem('token');
        if (token) {
            try {
                const base64Url = token.split('.')[1];
                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));

                // const decoded = JSON.parse(jsonPayload);
                JSON.parse(jsonPayload);
                // console.log('🔍 Token JWT decodificado:', decoded);
                // console.log('👤 Rol en el token:', decoded.role);
                // console.log('👤 Rol sin prefijo:', decoded.role?.replace('ROLE_', ''));
                // console.log('👤 Usuario actual en store:', useUserStore.getState().user);
            } catch (error) {
                // Error handling removed for production mode
            }
        } else {
            // console.log('❌ No hay token en localStorage');
        }
    },
}));