import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test('shows login page with branding and Google sign-in button', async ({ page }) => {
    await page.goto('/login');

    // Should display the app title (use heading role to be specific)
    await expect(
      page.getByRole('heading', { name: 'Trade Intel' })
    ).toBeVisible();
    await expect(
      page.getByText('WhatsApp Trade Intelligence Platform')
    ).toBeVisible();

    // Should display the sign-in prompt
    await expect(
      page.getByText('Sign in to access the platform')
    ).toBeVisible();

    // Should display the Google sign-in button
    const googleButton = page.getByRole('button', {
      name: /Continue with Google/i,
    });
    await expect(googleButton).toBeVisible();
  });

  test('unauthenticated user is redirected to login', async ({ page }) => {
    await page.goto('/replay');
    await expect(page).toHaveURL(/\/login/);
  });

  test('unauthenticated user accessing chat is redirected to login', async ({
    page,
  }) => {
    await page.goto('/chat');
    await expect(page).toHaveURL(/\/login/);
  });

  test('unauthenticated user accessing listings is redirected to login', async ({
    page,
  }) => {
    await page.goto('/listings');
    await expect(page).toHaveURL(/\/login/);
  });

  test('unauthenticated user accessing admin is redirected to login', async ({
    page,
  }) => {
    await page.goto('/admin/users');
    await expect(page).toHaveURL(/\/login/);
  });

  test('login page has correct footer text', async ({ page }) => {
    await page.goto('/login');
    await expect(
      page.getByText('Access is restricted to approved accounts only.')
    ).toBeVisible();
  });
});
