import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { X } from 'lucide-react';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';

/**
 * AppShell provides the outer layout: a persistent sidebar on desktop,
 * a slide-in drawer on mobile, and a top bar with a hamburger menu.
 * The <Outlet /> renders the active page's content.
 */
export function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

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
    </div>
  );
}
