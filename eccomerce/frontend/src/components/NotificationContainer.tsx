import React from 'react';
import { useNotificationStore } from '../stores/notificationStore';
import { X, CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-react';

const NotificationContainer: React.FC = () => {
    const { notifications, removeNotification } = useNotificationStore();

    if (notifications.length === 0) return null;

    const getIcon = (type: string) => {
        switch (type) {
            case 'success':
                return <CheckCircle className="w-5 h-5" />;
            case 'error':
                return <AlertCircle className="w-5 h-5" />;
            case 'warning':
                return <AlertTriangle className="w-5 h-5" />;
            case 'info':
                return <Info className="w-5 h-5" />;
            default:
                return <Info className="w-5 h-5" />;
        }
    };

    const getStyles = (type: string) => {
        switch (type) {
            case 'success':
                return 'bg-green-50 border-green-200 text-green-800';
            case 'error':
                return 'bg-red-50 border-red-200 text-red-800';
            case 'warning':
                return 'bg-yellow-50 border-yellow-200 text-yellow-800';
            case 'info':
                return 'bg-blue-50 border-blue-200 text-blue-800';
            default:
                return 'bg-gray-50 border-gray-200 text-gray-800';
        }
    };

    return (
        <div className="fixed top-20 right-4 z-50 space-y-2">
            {notifications.map((notification) => (
                <div
                    key={notification.id}
                    className={`flex items-center p-4 border rounded-lg shadow-lg max-w-md animate-in slide-in-from-right-2 ${getStyles(notification.type)}`}
                >
                    <div className="flex-shrink-0 mr-3">
                        {getIcon(notification.type)}
                    </div>
                    <div className="flex-1 text-sm font-medium">
                        {notification.message}
                    </div>
                    <button
                        onClick={() => removeNotification(notification.id)}
                        className="flex-shrink-0 ml-3 p-1 rounded-full hover:bg-black hover:bg-opacity-10 transition-colors"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>
            ))}
        </div>
    );
};

export default NotificationContainer;
