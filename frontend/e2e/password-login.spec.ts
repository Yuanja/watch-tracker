import { test, expect } from '@playwright/test';

/**
 * End-to-end tests for the username/password login flow.
 * Runs against the Spring Boot backend at localhost:8080 (serves both API and SPA).
 * Uses the seeded admin/password88 account (uber_admin role).
 */
test.describe('Password Login', () => {
  test.use({ baseURL: 'http://localhost:8080' });

  test.beforeEach(async ({ page }) => {
    // Clear any stored tokens
    await page.addInitScript(() => {
      localStorage.removeItem('trade_intel_token');
    });
  });

  test('login page shows username and password fields', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: 'Trade Intel' })).toBeVisible();
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible();
    await expect(page.getByRole('button', { name: /Continue with Google/i })).toBeVisible();
  });

  test('login with valid credentials redirects to app', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('password88');
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Should redirect away from login after successful auth
    await expect(page).not.toHaveURL(/\/login/, { timeout: 10_000 });

    // Should have stored the JWT token
    const token = await page.evaluate(() => localStorage.getItem('trade_intel_token'));
    expect(token).toBeTruthy();
    expect(token).toContain('eyJ'); // JWT prefix
  });

  test('login with invalid credentials shows error', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page.getByText('Invalid username or password')).toBeVisible();

    // Should still be on login page
    await expect(page).toHaveURL(/\/login/);
  });

  test('authenticated user can access /api/auth/me', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('password88');
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Wait for redirect
    await expect(page).not.toHaveURL(/\/login/, { timeout: 10_000 });

    // Verify the user profile is loaded (sidebar should show admin email)
    await expect(page.getByText('admin@tradeintel.local')).toBeVisible({ timeout: 5_000 });
  });

  test('authenticated uber_admin sees admin navigation', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('password88');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).not.toHaveURL(/\/login/, { timeout: 10_000 });

    // uber_admin should see admin menu items
    await expect(page.getByText('Review Queue')).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText('Users')).toBeVisible();
  });
});
