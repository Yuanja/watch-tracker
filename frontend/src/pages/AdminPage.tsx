import { Link } from 'react-router-dom';
import {
  Users,
  ScrollText,
  MessageSquare,
  BarChart2,
  Tag,
  Factory,
  Ruler,
  ShieldCheck,
  BookOpen,
} from 'lucide-react';

interface AdminCard {
  title: string;
  description: string;
  href: string;
  icon: React.ReactNode;
  iconBg: string;
  iconColor: string;
  roleNote?: string;
}

const adminCards: AdminCard[] = [
  {
    title: 'User Management',
    description: 'View all users, assign roles, and toggle account access.',
    href: '/admin/users',
    icon: <Users className="h-6 w-6" />,
    iconBg: 'bg-purple-100',
    iconColor: 'text-purple-600',
    roleNote: 'Uber Admin',
  },
  {
    title: 'Audit Log',
    description: 'Browse a paginated log of all privileged actions.',
    href: '/admin/audit',
    icon: <ScrollText className="h-6 w-6" />,
    iconBg: 'bg-gray-100',
    iconColor: 'text-gray-600',
    roleNote: 'Uber Admin',
  },
  {
    title: 'WhatsApp Groups',
    description: 'Add, view, and deactivate monitored WhatsApp groups.',
    href: '/admin/groups',
    icon: <MessageSquare className="h-6 w-6" />,
    iconBg: 'bg-green-100',
    iconColor: 'text-green-600',
    roleNote: 'Uber Admin',
  },
  {
    title: 'Cost Report',
    description: 'Review OpenAI token usage and costs across all users.',
    href: '/admin/costs',
    icon: <BarChart2 className="h-6 w-6" />,
    iconBg: 'bg-blue-100',
    iconColor: 'text-blue-600',
    roleNote: 'Uber Admin',
  },
  {
    title: 'Categories',
    description: 'Manage the product category taxonomy for listings.',
    href: '/admin/categories',
    icon: <Tag className="h-6 w-6" />,
    iconBg: 'bg-teal-100',
    iconColor: 'text-teal-600',
  },
  {
    title: 'Manufacturers',
    description: 'Manage known manufacturers and their aliases.',
    href: '/admin/manufacturers',
    icon: <Factory className="h-6 w-6" />,
    iconBg: 'bg-orange-100',
    iconColor: 'text-orange-600',
  },
  {
    title: 'Units',
    description: 'Manage units of measurement used in listings.',
    href: '/admin/units',
    icon: <Ruler className="h-6 w-6" />,
    iconBg: 'bg-yellow-100',
    iconColor: 'text-yellow-600',
  },
  {
    title: 'Conditions',
    description: 'Manage item condition values (New, Used, Refurbished, etc.).',
    href: '/admin/conditions',
    icon: <ShieldCheck className="h-6 w-6" />,
    iconBg: 'bg-indigo-100',
    iconColor: 'text-indigo-600',
  },
  {
    title: 'Jargon Dictionary',
    description: 'Review and edit industry acronyms and their expansions.',
    href: '/admin/jargon',
    icon: <BookOpen className="h-6 w-6" />,
    iconBg: 'bg-red-100',
    iconColor: 'text-red-600',
  },
];

export function AdminPage() {
  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-purple-100">
            <Users className="h-5 w-5 text-purple-600" />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-gray-900">
              Admin Dashboard
            </h1>
            <p className="text-xs text-gray-500">
              Platform management and configuration
            </p>
          </div>
        </div>
      </div>

      {/* Card grid */}
      <div className="flex-1 overflow-auto p-6">
        <div className="mx-auto max-w-4xl">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {adminCards.map((card) => (
              <Link
                key={card.href}
                to={card.href}
                className="group flex flex-col gap-3 rounded-xl border border-gray-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md hover:border-gray-300"
              >
                <div className="flex items-start justify-between">
                  <div
                    className={`flex h-11 w-11 items-center justify-center rounded-xl ${card.iconBg} ${card.iconColor}`}
                  >
                    {card.icon}
                  </div>
                  {card.roleNote && (
                    <span className="rounded-full bg-purple-50 px-2 py-0.5 text-xs font-medium text-purple-700 ring-1 ring-purple-200">
                      {card.roleNote}
                    </span>
                  )}
                </div>
                <div>
                  <h2 className="text-sm font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                    {card.title}
                  </h2>
                  <p className="mt-1 text-xs text-gray-500 leading-relaxed">
                    {card.description}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
