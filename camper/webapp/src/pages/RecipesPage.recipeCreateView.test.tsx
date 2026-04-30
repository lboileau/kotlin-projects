/**
 * W13 — RecipesPage.RecipeCreateView regression tests
 *
 * Verifies that the thin-wrapper RecipeCreateView still provides the correct
 * behavior after the form logic was extracted into RecipeForm.
 *
 * (1) /recipes/new renders the form (RecipeCreateView → RecipeForm).
 * (2) Submitting navigates to /recipes and fires the "Recipe saved as draft" toast.
 *
 * Uses the same MemoryRouter + vi.hoisted() pattern as RecipesPage.routing.test.tsx.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RecipesPage } from './RecipesPage';
import type { RecipeResponse } from '../api/client';

// ─── Mock api ────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getRecipes:     vi.fn(),
  getIngredients: vi.fn(),
  getRecipe:      vi.fn(),
  createRecipe:   vi.fn(),
}));

vi.mock('../api/client', () => ({
  api: mockApi,
}));

// ─── Mock useToast ────────────────────────────────────────────────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
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

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const mockCreatedRecipe: RecipeResponse = {
  id: 'r-created',
  name: 'Trail Pasta',
  description: null,
  webLink: null,
  baseServings: 4,
  status: 'draft',
  createdBy: 'user1',
  duplicateOfId: null,
  meal: null,
  theme: null,
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
    </MemoryRouter>,
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('RecipesPage — RecipeCreateView regression (W13)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getRecipes.mockResolvedValue([]);
    mockApi.getIngredients.mockResolvedValue([]);
    mockApi.createRecipe.mockResolvedValue(mockCreatedRecipe);
  });

  // ── (1) /recipes/new renders the form ────────────────────────────────────

  it('(1) /recipes/new renders the create form with "New Provision" title', async () => {
    renderAt('/recipes/new');

    await waitFor(() => {
      expect(screen.getByText('New Provision')).toBeInTheDocument();
    });

    // Form fields should be present
    expect(screen.getByPlaceholderText(/trailside oatmeal/i)).toBeInTheDocument();
  });

  // ── (2) Submit navigates to /recipes and fires toast ─────────────────────

  it('(2) submitting the form navigates to /recipes and fires "Recipe saved as draft" toast', async () => {
    const user = userEvent.setup();
    renderAt('/recipes/new');

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/trailside oatmeal/i)).toBeInTheDocument();
    });

    // Fill in the recipe name
    await user.type(screen.getByPlaceholderText(/trailside oatmeal/i), 'Trail Pasta');

    // Submit
    await user.click(screen.getByRole('button', { name: 'Add to Cookbook' }));

    await waitFor(() => {
      expect(mockApi.createRecipe).toHaveBeenCalledOnce();
    });

    // Toast fires
    expect(mockToast.success).toHaveBeenCalledWith('Recipe saved as draft');

    // Navigation to /recipes (list view is rendered — "The Recipe Chest" heading)
    await waitFor(() => {
      expect(screen.getByText('The Recipe Chest')).toBeInTheDocument();
    });
  });
});
