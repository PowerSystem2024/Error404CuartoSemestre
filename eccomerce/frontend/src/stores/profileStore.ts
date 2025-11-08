import { create } from 'zustand';
import { profileAPI } from '../services/api';

interface Profile {
    firstName: string;
    lastName: string;
    phone: string;
    email: string;
    username: string;
}

interface ProfileStore {
    profile: Profile | null;
    loading: boolean;
    loadProfile: () => Promise<void>;
    updateProfile: (profileData: Partial<Profile>) => Promise<void>;
}

export const useProfileStore = create<ProfileStore>((set, get) => ({
    profile: null,
    loading: false,

    loadProfile: async () => {
        try {
            set({ loading: true });
            const response = await profileAPI.getProfile();
            // console.log('👤 Profile loaded:', response.data);
            set({ profile: response.data });
        } catch (error) {
            // Error handling removed for production mode
        } finally {
            set({ loading: false });
        }
    },

    updateProfile: async (profileData) => {
        try {
            set({ loading: true });

            // Verificamos si el perfil actual existe
            const currentProfile = get().profile;
            if (!currentProfile) {
                throw new Error('No profile loaded');
            }

            // Creamos un objeto con los campos requeridos, 
            // asegurándonos que firstName y lastName estén definidos
            const dataToUpdate = {
                firstName: profileData.firstName ?? currentProfile.firstName,
                lastName: profileData.lastName ?? currentProfile.lastName,
                phone: profileData.phone
            };

            await profileAPI.updateProfile(dataToUpdate);
            // Reload profile after update
            await get().loadProfile();
        } catch (error) {
            // Error handling removed for production mode
            throw error;
        } finally {
            set({ loading: false });
        }
    },
}));