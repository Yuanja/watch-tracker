import { useState, useCallback } from 'react';
import { Outlet } from 'react-router-dom';
import { X, Bell } from 'lucide-react';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { useWebSocket } from '../../hooks/useWebSocket';

/**
 * AppShell provides the outer layout: a persistent sidebar on desktop,
 * a slide-in drawer on mobile, and a top bar with a hamburger menu.
 * The <Outlet /> renders the active page's content.
 */
interface ToastMessage {
  id: number;
  text: string;
}

export function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Subscribe to real-time notification alerts
  useWebSocket('/user/queue/notifications', useCallback((payload: unknown) => {
    const msg = payload as { message?: string; nlRule?: string };
    const text = msg.message ?? msg.nlRule ?? 'New notification match';
    const id = Date.now();
    setToasts((prev) => [...prev, { id, text }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  }, []));

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      {/* Desktop sidebar — always visible on lg+ */}
      <aside className="hidden lg:flex lg:flex-col lg:w-56 lg:shrink-0">
        <Sidebar />
      </aside>

      {/* Mobile sidebar — drawer overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => setSidebarOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer panel */}
          <div className="absolute inset-y-0 left-0 flex w-64 flex-col bg-white shadow-xl">
            {/* Close button */}
            <div className="flex items-center justify-end px-3 py-3 border-b border-gray-200">
              <button
                onClick={() => setSidebarOpen(false)}
                className="rounded-md p-1.5 text-gray-500 hover:bg-gray-100"
                aria-label="Close sidebar"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <Sidebar onClose={() => setSidebarOpen(false)} />
          </div>
        </div>
      )}

      {/* Main content area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Mobile top bar */}
        <TopBar onMenuToggle={() => setSidebarOpen(true)} />

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>

      {/* Notification toasts */}
      {toasts.length > 0 && (
        <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
          {toasts.map((toast) => (
            <div
              key={toast.id}
              className="flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 shadow-lg animate-in slide-in-from-right"
            >
              <Bell className="h-4 w-4 shrink-0 text-blue-600" />
              <p className="text-sm text-blue-800">{toast.text}</p>
              <button
                onClick={() => setToasts((prev) => prev.filter((t) => t.id !== toast.id))}
                className="ml-2 shrink-0 rounded p-0.5 text-blue-400 hover:text-blue-600"
                aria-label="Dismiss"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
