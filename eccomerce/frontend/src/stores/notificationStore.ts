import { create } from 'zustand';

export interface Notification {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
    duration?: number; // in milliseconds
}

interface NotificationStore {
    notifications: Notification[];
    addNotification: (notification: Omit<Notification, 'id'>) => void;
    removeNotification: (id: string) => void;
    clearAll: () => void;
}

export const useNotificationStore = create<NotificationStore>((set, get) => ({
    notifications: [],

    addNotification: (notification) => {
        const id = Date.now().toString() + Math.random().toString(36).substr(2, 9);
        const newNotification: Notification = {
            id,
            duration: 5000, // Default 5 seconds
            ...notification,
        };

        set((state) => ({
            notifications: [...state.notifications, newNotification],
        }));

        // Auto-remove after duration
        if (newNotification.duration && newNotification.duration > 0) {
            setTimeout(() => {
                get().removeNotification(id);
            }, newNotification.duration);
        }
    },

    removeNotification: (id) => {
        set((state) => ({
            notifications: state.notifications.filter((n) => n.id !== id),
        }));
    },

    clearAll: () => {
        set({ notifications: [] });
    },
}));
