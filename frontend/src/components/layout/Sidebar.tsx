import { NavLink } from 'react-router-dom';
import { clsx } from 'clsx';
import {
  MessageSquare,
  Bot,
  List,
  Bell,
  DollarSign,
  ClipboardList,
  Tag,
  Factory,
  Ruler,
  Wrench,
  BookOpen,
  Users,
  MessagesSquare,
  BarChart2,
  ScrollText,
  Smartphone,
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import type { UserRole } from '../../types/user';

interface NavItem {
  to: string;
  icon: React.ReactNode;
  label: string;
  minRole?: UserRole;
}

const mainNavItems: NavItem[] = [
  {
    to: '/replay',
    icon: <MessageSquare className="h-4 w-4" />,
    label: 'Message Replay',
  },
  {
    to: '/chat',
    icon: <Bot className="h-4 w-4" />,
    label: 'Ask AI',
  },
  {
    to: '/listings',
    icon: <List className="h-4 w-4" />,
    label: 'Listings',
  },
  {
    to: '/notifications',
    icon: <Bell className="h-4 w-4" />,
    label: 'Notifications',
  },
  {
    to: '/costs',
    icon: <DollarSign className="h-4 w-4" />,
    label: 'My Usage',
  },
];

const adminNavItems: NavItem[] = [
  {
    to: '/review',
    icon: <ClipboardList className="h-4 w-4" />,
    label: 'Review Queue',
    minRole: 'admin',
  },
  {
    to: '/admin/categories',
    icon: <Tag className="h-4 w-4" />,
    label: 'Categories',
    minRole: 'admin',
  },
  {
    to: '/admin/manufacturers',
    icon: <Factory className="h-4 w-4" />,
    label: 'Manufacturers',
    minRole: 'admin',
  },
  {
    to: '/admin/units',
    icon: <Ruler className="h-4 w-4" />,
    label: 'Units',
    minRole: 'admin',
  },
  {
    to: '/admin/conditions',
    icon: <Wrench className="h-4 w-4" />,
    label: 'Conditions',
    minRole: 'admin',
  },
  {
    to: '/admin/jargon',
    icon: <BookOpen className="h-4 w-4" />,
    label: 'Jargon Dict',
    minRole: 'admin',
  },
];

const uberAdminNavItems: NavItem[] = [
  {
    to: '/admin/users',
    icon: <Users className="h-4 w-4" />,
    label: 'Users',
    minRole: 'uber_admin',
  },
  {
    to: '/admin/chats',
    icon: <MessagesSquare className="h-4 w-4" />,
    label: 'All Chats',
    minRole: 'uber_admin',
  },
  {
    to: '/admin/costs',
    icon: <BarChart2 className="h-4 w-4" />,
    label: 'Cost Report',
    minRole: 'uber_admin',
  },
  {
    to: '/admin/audit',
    icon: <ScrollText className="h-4 w-4" />,
    label: 'Audit Log',
    minRole: 'uber_admin',
  },
  {
    to: '/admin/groups',
    icon: <Smartphone className="h-4 w-4" />,
    label: 'WhatsApp Groups',
    minRole: 'uber_admin',
  },
];

function roleLevel(role: UserRole): number {
  const levels: Record<UserRole, number> = {
    user: 0,
    admin: 1,
    uber_admin: 2,
  };
  return levels[role] ?? 0;
}

function hasRole(userRole: UserRole, minRole: UserRole): boolean {
  return roleLevel(userRole) >= roleLevel(minRole);
}

function NavItemLink({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.to}
      className={({ isActive }) =>
        clsx(
          'flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors',
          isActive
            ? 'bg-blue-50 text-blue-700'
            : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
        )
      }
    >
      {item.icon}
      {item.label}
    </NavLink>
  );
}

interface SidebarProps {
  onClose?: () => void;
}

export function Sidebar({ onClose: _onClose }: SidebarProps) {
  const { user } = useAuth();
  const userRole: UserRole = user?.role ?? 'user';

  const showAdminSection =
    userRole === 'admin' || userRole === 'uber_admin';
  const showUberAdminSection = userRole === 'uber_admin';

  return (
    <nav className="flex h-full flex-col bg-white border-r border-gray-200">
      {/* Logo / App name */}
      <div className="flex items-center gap-2 px-4 py-4 border-b border-gray-200">
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-blue-600">
          <MessageSquare className="h-4 w-4 text-white" />
        </div>
        <span className="text-base font-semibold text-gray-900">Trade Intel</span>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-y-auto px-3 py-3 space-y-1">
        {/* Main nav */}
        {mainNavItems.map((item) => (
          <NavItemLink key={item.to} item={item} />
        ))}

        {/* Admin section */}
        {showAdminSection && (
          <>
            <div className="pt-4 pb-1 px-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">
                Admin
              </span>
            </div>
            {adminNavItems
              .filter(
                (item) =>
                  !item.minRole || hasRole(userRole, item.minRole)
              )
              .map((item) => (
                <NavItemLink key={item.to} item={item} />
              ))}
          </>
        )}

        {/* Uber admin section */}
        {showUberAdminSection && (
          <>
            <div className="pt-4 pb-1 px-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">
                Uber Admin
              </span>
            </div>
            {uberAdminNavItems
              .filter(
                (item) =>
                  !item.minRole || hasRole(userRole, item.minRole)
              )
              .map((item) => (
                <NavItemLink key={item.to} item={item} />
              ))}
          </>
        )}
      </div>

      {/* User info at bottom */}
      {user && (
        <div className="border-t border-gray-200 px-4 py-3">
          <div className="flex items-center gap-3">
            {user.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt={user.displayName ?? user.email}
                className="h-8 w-8 rounded-full object-cover"
              />
            ) : (
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-200 text-xs font-medium text-gray-600">
                {(user.displayName ?? user.email).charAt(0).toUpperCase()}
              </div>
            )}
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-gray-900">
                {user.displayName ?? user.email}
              </p>
              <p className="truncate text-xs text-gray-500">{user.email}</p>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
