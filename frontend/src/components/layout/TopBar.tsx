import { Menu, LogOut } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

interface TopBarProps {
  onMenuToggle: () => void;
}

export function TopBar({ onMenuToggle }: TopBarProps) {
  const { user, logout } = useAuth();

  return (
    <header className="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-4 lg:hidden">
      <button
        onClick={onMenuToggle}
        className="rounded-md p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        aria-label="Open sidebar"
      >
        <Menu className="h-5 w-5" />
      </button>

      <span className="text-sm font-semibold text-gray-900">Trade Intel</span>

      {user && (
        <button
          onClick={logout}
          className="rounded-md p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          aria-label="Log out"
          title="Log out"
        >
          <LogOut className="h-5 w-5" />
        </button>
      )}
    </header>
  );
}
