import { useEffect, useState } from 'react';
import { useUserStore } from '../stores/userStore';
import { profileAPI } from '../services/api';

const Profile = () => {
  const { user } = useUserStore();
  const [profileData, setProfileData] = useState({
    firstName: '',
    lastName: '',
    phone: '',
    email: user?.email || '',
    username: ''
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadProfile = async () => {
      if (!user) return;

      try {
        setLoading(true);
        const response = await profileAPI.getProfile();
        const data = response.data;

        setProfileData({
          firstName: data.firstName || '',
          lastName: data.lastName || '',
          phone: data.phone || '',
          email: data.email || user.email,
          username: data.username || ''
        });
      } catch (error) {
        // Error handling removed for production mode
        // Fallback a datos del JWT
        const nameParts = user.name.split(' ');
        setProfileData({
          firstName: nameParts[0] || '',
          lastName: nameParts.slice(1).join(' ') || '',
          phone: '',
          email: user.email,
          username: ''
        });
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [user]);

  if (!user) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-800 mb-4">Acceso Denegado</h1>
          <p className="text-gray-600">Debes iniciar sesión para ver tu perfil.</p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Cargando perfil...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-2xl mx-auto bg-white rounded-lg shadow-md p-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">👤 Mi Perfil</h1>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Nombre de Usuario
            </label>
            <p className="text-lg text-gray-900">{profileData.username || 'No especificado'}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Nombre
            </label>
            <p className="text-lg text-gray-900">{profileData.firstName}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Apellido
            </label>
            <p className="text-lg text-gray-900">{profileData.lastName}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <p className="text-lg text-gray-900">{profileData.email}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Teléfono
            </label>
            <p className="text-lg text-gray-900">{profileData.phone || 'No especificado'}</p>
          </div>
        </div>

        <div className="mt-6">
          <a
            href="/profile/edit"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            ✏️ Editar Perfil
          </a>
        </div>
      </div>
    </div>
  );
};

export default Profile;