import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import { AppShell } from './components/layout/AppShell';
import { LoadingOverlay } from './components/common/LoadingSpinner';
import { LoginPage } from './pages/LoginPage';
import { ReplayPage } from './pages/ReplayPage';
import { ChatPage } from './pages/ChatPage';
import { ListingsPage } from './pages/ListingsPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { CostPage } from './pages/CostPage';
import { ReviewPage } from './pages/ReviewPage';
import { AdminPage } from './pages/AdminPage';
import type { UserRole } from './types/user';

/**
 * AuthGuard protects routes that require an authenticated user.
 * Shows a loading state while auth is being resolved, then redirects
 * to /login if not authenticated.
 */
function AuthGuard() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <LoadingOverlay message="Loading your account..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

/**
 * RoleGuard protects routes that require a specific minimum role.
 * Redirects to the root if the user's role is insufficient.
 */
interface RoleGuardProps {
  roles: UserRole[];
}

function RoleGuard({ roles }: RoleGuardProps) {
  const { user } = useAuth();

  const roleLevel: Record<UserRole, number> = {
    user: 0,
    admin: 1,
    uber_admin: 2,
  };

  const userLevel = roleLevel[user?.role ?? 'user'] ?? 0;
  const hasRequiredRole = roles.some(
    (role) => userLevel >= roleLevel[role]
  );

  if (!hasRequiredRole) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

/**
 * Placeholder pages for admin-managed sections.
 * These will be replaced by full implementations in later phases.
 */
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex h-full items-center justify-center p-8">
      <div className="text-center">
        <h1 className="text-xl font-semibold text-gray-900">{title}</h1>
        <p className="mt-2 text-sm text-gray-500">
          Implementation coming in a future phase.
        </p>
      </div>
    </div>
  );
}

export function App() {
  return (
    <Routes>
      {/* Public route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Authenticated routes */}
      <Route element={<AuthGuard />}>
        <Route element={<AppShell />}>
          {/* Default redirect */}
          <Route path="/" element={<Navigate to="/replay" replace />} />

          {/* User routes */}
          <Route path="/replay" element={<ReplayPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/listings" element={<ListingsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/costs" element={<CostPage />} />

          {/* Admin routes (admin+ only) */}
          <Route element={<RoleGuard roles={['admin', 'uber_admin']} />}>
            <Route path="/review" element={<ReviewPage />} />
            <Route
              path="/admin/categories"
              element={<PlaceholderPage title="Categories" />}
            />
            <Route
              path="/admin/manufacturers"
              element={<PlaceholderPage title="Manufacturers" />}
            />
            <Route
              path="/admin/units"
              element={<PlaceholderPage title="Units" />}
            />
            <Route
              path="/admin/conditions"
              element={<PlaceholderPage title="Conditions" />}
            />
            <Route
              path="/admin/jargon"
              element={<PlaceholderPage title="Jargon Dictionary" />}
            />
          </Route>

          {/* Uber admin routes (uber_admin only) */}
          <Route element={<RoleGuard roles={['uber_admin']} />}>
            <Route
              path="/admin/users"
              element={<PlaceholderPage title="User Management" />}
            />
            <Route
              path="/admin/chats"
              element={<PlaceholderPage title="All Chats" />}
            />
            <Route
              path="/admin/costs"
              element={<PlaceholderPage title="Cost Report" />}
            />
            <Route
              path="/admin/audit"
              element={<PlaceholderPage title="Audit Log" />}
            />
            <Route
              path="/admin/groups"
              element={<PlaceholderPage title="WhatsApp Groups" />}
            />
            <Route path="/admin" element={<AdminPage />} />
          </Route>

          {/* Catch-all — redirect unknown paths to replay */}
          <Route path="*" element={<Navigate to="/replay" replace />} />
        </Route>
      </Route>

      {/* Catch-all for unauthenticated users */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
