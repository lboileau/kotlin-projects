/**
 * W14 — RecipesPage search/filter URL persistence
 *
 * Tests that RecipesListView reads search and meal-tab state from URL search
 * params (?q=, ?meal=) and writes back via setSearchParams({ replace: true }).
 *
 * ── UrlProbe pattern ──────────────────────────────────────────────────────────
 * To assert URL changes inside a MemoryRouter, render a small "UrlProbe"
 * component as a sibling to the Routes block. It calls useLocation() and
 * surfaces location.search to the test via a data-testid element.
 *
 *   function UrlProbe() {
 *     const loc = useLocation();
 *     return <div data-testid="url-search">{loc.search}</div>;
 *   }
 *
 *   render(
 *     <MemoryRouter initialEntries={['/recipes']}>
 *       <Routes>
 *         <Route path="/recipes/*" element={<RecipesPage />} />
 *       </Routes>
 *       <UrlProbe />                   ← sibling, same router context
 *     </MemoryRouter>
 *   );
 *
 *   // After user interaction:
 *   await waitFor(() => {
 *     expect(screen.getByTestId('url-search')).toHaveTextContent('?q=pasta');
 *   });
 *
 * This pattern is reusable for W6 (hash assertions) — document it inline.
 * Pioneered here (W14) as the second MemoryRouter test in the project.
 * ─────────────────────────────────────────────────────────────────────────────
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { RecipesPage } from './RecipesPage';
import type { RecipeResponse } from '../api/client';

// ─── Mock api ────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getRecipes: vi.fn(),
  getIngredients: vi.fn(),
  getRecipe: vi.fn(),
}));

vi.mock('../api/client', () => ({ api: mockApi }));

// ─── Mock useToast ────────────────────────────────────────────────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
  info: vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
}));

// ─── Mock useAuth (required by AppHeader) ────────────────────────────────────

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user1',
      email: 'test@example.com',
      username: 'Tester',
      avatar: null,
      profileCompleted: true,
      avatarSeed: null,
      dietaryRestrictions: [],
      experienceLevel: null,
      createdAt: '',
      updatedAt: '',
    },
    logout: vi.fn(),
    login: vi.fn(),
    isAuthenticated: true,
  }),
}));

// ─── UrlProbe ─────────────────────────────────────────────────────────────────
// Sibling component rendered inside the same MemoryRouter. Surfaces the current
// location.search so tests can assert URL changes without coupling to internals.

function UrlProbe() {
  const loc = useLocation();
  return <div data-testid="url-search">{loc.search}</div>;
}

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const dinnerPasta: RecipeResponse = {
  id: 'r1',
  name: 'Trail Pasta',
  description: 'Campfire spaghetti',
  webLink: null,
  baseServings: 4,
  status: 'published',
  createdBy: 'user1',
  duplicateOfId: null,
  meal: 'dinner',
  theme: 'pasta',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const breakfastPasta: RecipeResponse = {
  id: 'r2',
  name: 'Breakfast Pasta',
  description: 'Morning carbs',
  webLink: null,
  baseServings: 2,
  status: 'published',
  createdBy: 'user1',
  duplicateOfId: null,
  meal: 'breakfast',
  theme: 'pasta',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const beefStew: RecipeResponse = {
  id: 'r3',
  name: 'Camp Beef Stew',
  description: 'Hearty stew',
  webLink: null,
  baseServings: 6,
  status: 'published',
  createdBy: 'user1',
  duplicateOfId: null,
  meal: 'dinner',
  theme: 'beef',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

// ─── Helper ───────────────────────────────────────────────────────────────────

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/recipes/*" element={<RecipesPage />} />
      </Routes>
      <UrlProbe />
    </MemoryRouter>
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('RecipesPage search persistence (W14)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getRecipes.mockResolvedValue([dinnerPasta, breakfastPasta, beefStew]);
    mockApi.getIngredients.mockResolvedValue([]);
    mockApi.getRecipe.mockRejectedValue(new Error('not found'));
  });

  it('(1) ?q=pasta pre-populates search input and filters recipe list', async () => {
    renderAt('/recipes?q=pasta');

    // Wait for list to load
    await waitFor(() => expect(screen.getByText('Trail Pasta')).toBeInTheDocument());

    // Search input should be pre-populated from URL
    const input = screen.getByPlaceholderText('Search provisions...');
    expect(input).toHaveValue('pasta');

    // Both pasta recipes should be visible; beef stew should be filtered out
    expect(screen.getByText('Trail Pasta')).toBeInTheDocument();
    expect(screen.getByText('Breakfast Pasta')).toBeInTheDocument();
    expect(screen.queryByText('Camp Beef Stew')).not.toBeInTheDocument();
  });

  it('(2) typing in search input updates URL ?q= via setSearchParams', async () => {
    const user = userEvent.setup();
    renderAt('/recipes');

    // Wait for list to finish loading
    await waitFor(() => expect(screen.getByText('Trail Pasta')).toBeInTheDocument());

    const input = screen.getByPlaceholderText('Search provisions...');
    await user.type(input, 'pasta');

    // UrlProbe should reflect the updated search param
    await waitFor(() => {
      expect(screen.getByTestId('url-search')).toHaveTextContent('?q=pasta');
    });
  });

  it('(3) ?q=pasta&meal=dinner pre-populates both filters', async () => {
    renderAt('/recipes?q=pasta&meal=dinner');

    // Wait for list to load
    await waitFor(() => expect(screen.getByText('Trail Pasta')).toBeInTheDocument());

    // Search input should show "pasta"
    const input = screen.getByPlaceholderText('Search provisions...');
    expect(input).toHaveValue('pasta');

    // Only the dinner pasta recipe should pass both filters
    expect(screen.getByText('Trail Pasta')).toBeInTheDocument();
    expect(screen.queryByText('Breakfast Pasta')).not.toBeInTheDocument();
    expect(screen.queryByText('Camp Beef Stew')).not.toBeInTheDocument();

    // The "dinner" tab pill should have the active class
    // (meal tabs render when >1 meal type passes the search filter)
    const dinnerTab = screen.getByRole('button', { name: 'dinner' });
    expect(dinnerTab).toHaveClass('recipes-meal-tab--active');
  });

  it('(4) clearing search input removes ?q from URL', async () => {
    const user = userEvent.setup();
    renderAt('/recipes?q=beef');

    // Wait for list to load; initial URL has ?q=beef
    await waitFor(() => {
      expect(screen.getByTestId('url-search')).toHaveTextContent('?q=beef');
    });

    const input = screen.getByPlaceholderText('Search provisions...');
    await user.clear(input);

    // After clearing, q should be removed from URL
    await waitFor(() => {
      const probe = screen.getByTestId('url-search');
      expect(probe.textContent).not.toContain('q=');
    });
  });
});
