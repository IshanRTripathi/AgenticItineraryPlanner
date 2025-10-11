import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { ChangeNotification } from './ChangeNotification';

interface Notification {
  id: string;
  message: string;
  type?: 'success' | 'error' | 'info' | 'warning';
  showUndo?: boolean;
  onUndo?: () => void;
  userName?: string;
  duration?: number;
}

interface NotificationContextType {
  showNotification: (notification: Omit<Notification, 'id'>) => void;
  hideNotification: (id: string) => void;
  clearAll: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notifications, setNotifications] = useState<Notification[]>([]);

  const showNotification = useCallback((notification: Omit<Notification, 'id'>) => {
    const id = `notification-${Date.now()}-${Math.random()}`;
    setNotifications(prev => [...prev, { ...notification, id }]);
  }, []);

  const hideNotification = useCallback((id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  const clearAll = useCallback(() => {
    setNotifications([]);
  }, []);

  return (
    <NotificationContext.Provider value={{ showNotification, hideNotification, clearAll }}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 space-y-2">
        {notifications.map((notification, index) => (
          <div key={notification.id} style={{ marginBottom: index > 0 ? '8px' : 0 }}>
            <ChangeNotification
              message={notification.message}
              type={notification.type}
              showUndo={notification.showUndo}
              onUndo={notification.onUndo}
              userName={notification.userName}
              duration={notification.duration}
              onClose={() => hideNotification(notification.id)}
            />
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  return context;
}
