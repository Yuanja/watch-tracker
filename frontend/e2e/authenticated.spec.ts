import { test, expect, Page } from '@playwright/test';

const MOCK_USER_UBER_ADMIN = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'test@example.com',
  displayName: 'Test User',
  avatarUrl: null,
  role: 'uber_admin',
  isActive: true,
};

/**
 * Sets up API mocking and localStorage token BEFORE the page loads,
 * so AuthContext immediately finds a token and getMe() is intercepted.
 *
 * IMPORTANT: Playwright matches routes in reverse registration order
 * (last registered is checked first). Register the catch-all FIRST
 * so specific routes registered after take priority.
 */
async function setupAuth(
  page: Page,
  role: 'user' | 'admin' | 'uber_admin' = 'uber_admin'
) {
  const user = { ...MOCK_USER_UBER_ADMIN, role };

  // Set localStorage before any page loads
  await page.addInitScript(
    ({ token }) => {
      localStorage.setItem('trade_intel_token', token);
    },
    { token: 'mock-jwt-token' }
  );

  const emptyPage = {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 50,
    last: true,
  };

  // Single route handler using a predicate to match only real API calls
  // (paths starting with /api/), NOT Vite module paths like /src/api/*.ts
  await page.route(
    (url) => new URL(url).pathname.startsWith('/api/'),
    (route) => {
      const pathname = new URL(route.request().url()).pathname;

      if (pathname === '/api/auth/me') {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(user),
        });
      }

      if (pathname === '/api/messages/groups') {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        });
      }

      // Default: return empty paginated response
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(emptyPage),
      });
    }
  );
}

test.describe('Authenticated Navigation', () => {
  test('root path redirects to /replay', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/');

    await expect(page).toHaveURL(/\/replay/);
  });

  test('sidebar shows main navigation links', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/replay');

    await expect(page.getByText('Message Replay')).toBeVisible();
    await expect(page.getByText('Ask AI')).toBeVisible();
    await expect(page.getByText('Listings')).toBeVisible();
    await expect(page.getByText('Notifications')).toBeVisible();
    await expect(page.getByText('My Usage')).toBeVisible();
  });

  test('sidebar shows admin sections for uber_admin', async ({ page }) => {
    await setupAuth(page, 'uber_admin');
    await page.goto('/replay');

    // Admin section
    await expect(page.getByText('Review Queue')).toBeVisible();
    await expect(page.getByText('Categories')).toBeVisible();

    // Uber admin section
    await expect(page.getByText('Users')).toBeVisible();
    await expect(page.getByText('Audit Log')).toBeVisible();
  });

  test('sidebar hides admin section for regular user', async ({ page }) => {
    await setupAuth(page, 'user');
    await page.goto('/replay');

    // Wait for the page to fully render
    await expect(page.getByText('Message Replay')).toBeVisible();

    // Admin-only items should not be visible
    await expect(page.getByText('Review Queue')).not.toBeVisible();
    await expect(page.getByText('Users')).not.toBeVisible();
  });
});

test.describe('Replay Page', () => {
  test('shows group list and empty state', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/replay');

    await expect(
      page.getByRole('heading', { name: 'Groups' })
    ).toBeVisible();
    await expect(page.getByText('Select a group')).toBeVisible();
  });
});

test.describe('Listings Page', () => {
  test('loads listings page', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/listings');

    await expect(
      page.getByRole('heading', { name: 'Listings' })
    ).toBeVisible();
  });
});

test.describe('Chat Page', () => {
  test('loads chat interface', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/chat');

    await expect(
      page.getByRole('heading', { name: /Ask AI/i })
    ).toBeVisible();
  });
});

test.describe('Notifications Page', () => {
  test('loads notifications page', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/notifications');

    await expect(
      page.getByRole('heading', { name: 'Notifications' })
    ).toBeVisible();
  });
});

test.describe('Cost Page', () => {
  test('loads cost/usage page', async ({ page }) => {
    await setupAuth(page);
    await page.goto('/costs');

    await expect(
      page.getByRole('heading', { name: 'My Usage' })
    ).toBeVisible();
  });
});

test.describe('Admin Pages (admin+)', () => {
  test('review queue loads for admin', async ({ page }) => {
    await setupAuth(page, 'admin');
    await page.goto('/review');

    await expect(
      page.getByRole('heading', { name: /Review Queue/i })
    ).toBeVisible();
  });

  test('categories admin page loads', async ({ page }) => {
    await setupAuth(page, 'admin');
    await page.goto('/admin/categories');

    await expect(page.getByRole('heading', { name: 'Categories' })).toBeVisible();
  });

  test('manufacturers admin page loads', async ({ page }) => {
    await setupAuth(page, 'admin');
    await page.goto('/admin/manufacturers');

    await expect(page.getByRole('heading', { name: 'Manufacturers' })).toBeVisible();
  });

  test('jargon admin page loads', async ({ page }) => {
    await setupAuth(page, 'admin');
    await page.goto('/admin/jargon');

    await expect(page.getByRole('heading', { name: 'Jargon' })).toBeVisible();
  });

  test('regular user is denied access to review queue', async ({ page }) => {
    await setupAuth(page, 'user');
    await page.goto('/review');

    // Should redirect away from /review since user lacks admin role
    await expect(page).not.toHaveURL(/\/review/);
  });
});

test.describe('Uber Admin Pages', () => {
  test('user management page loads', async ({ page }) => {
    await setupAuth(page, 'uber_admin');
    await page.goto('/admin/users');

    await expect(page.getByText('User Management')).toBeVisible();
  });

  test('audit log page loads', async ({ page }) => {
    await setupAuth(page, 'uber_admin');
    await page.goto('/admin/audit');

    await expect(page.getByRole('heading', { name: 'Audit Log' })).toBeVisible();
  });

  test('cost report page loads', async ({ page }) => {
    await setupAuth(page, 'uber_admin');
    await page.goto('/admin/costs');

    await expect(page.getByRole('heading', { name: 'Cost Report' })).toBeVisible();
  });

  test('group management page loads', async ({ page }) => {
    await setupAuth(page, 'uber_admin');
    await page.goto('/admin/groups');

    await expect(page.getByRole('heading', { name: 'WhatsApp Groups' })).toBeVisible();
  });

  test('admin user is denied access to uber admin pages', async ({ page }) => {
    await setupAuth(page, 'admin');
    await page.goto('/admin/users');

    // Should redirect away since admin lacks uber_admin role
    await expect(page).not.toHaveURL(/\/admin\/users/);
  });
});
