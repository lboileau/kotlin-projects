/**
 * W1 — RecipesPage routing (first MemoryRouter test in the project)
 *
 * This is the first routing test in the project. It renders RecipesPage inside
 * a MemoryRouter so react-router-dom's nested <Routes> work in jsdom.
 *
 * Copy-paste pattern for future routing tests (W14, W6):
 *
 *   render(
 *     <MemoryRouter initialEntries={['/recipes/abc']}>
 *       <Routes>
 *         <Route path="/recipes/*" element={<RecipesPage />} />
 *       </Routes>
 *     </MemoryRouter>
 *   );
 *
 * vitest globals: false — all imports (describe, it, expect, vi, beforeEach,
 * waitFor) must be explicit.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RecipesPage } from './RecipesPage';
import type { RecipeResponse, RecipeDetailResponse } from '../api/client';

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

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const mockRecipe: RecipeResponse = {
  id: 'abc',
  name: 'Trail Pasta',
  description: 'A campfire classic',
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

const mockRecipeDetail: RecipeDetailResponse = {
  id: 'abc',
  name: 'Trail Pasta',
  description: 'A campfire classic',
  webLink: null,
  baseServings: 4,
  status: 'published',
  createdBy: 'user1',
  duplicateOf: null,
  ingredients: [],
  meal: 'dinner',
  theme: 'pasta',
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
    </MemoryRouter>
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('RecipesPage routing (W1)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getRecipes.mockResolvedValue([mockRecipe]);
    mockApi.getIngredients.mockResolvedValue([]);
    mockApi.getRecipe.mockImplementation((id: string) =>
      id === 'abc'
        ? Promise.resolve(mockRecipeDetail)
        : Promise.reject(new Error('not found'))
    );
  });

  it('(1) /recipes — list view renders with recipe cards', async () => {
    renderAt('/recipes');
    await waitFor(() => expect(screen.getByText('Trail Pasta')).toBeInTheDocument());
    expect(screen.getByText('The Recipe Chest')).toBeInTheDocument();
  });

  it('(2) /recipes/new — create form renders', async () => {
    renderAt('/recipes/new');
    await waitFor(() => expect(screen.getByText('New Provision')).toBeInTheDocument());
  });

  it('(3) /recipes/import — import form renders', async () => {
    renderAt('/recipes/import');
    await waitFor(() => expect(screen.getByText('Import from URL')).toBeInTheDocument());
  });

  it('(4) /recipes/abc — detail view renders the recipe name', async () => {
    renderAt('/recipes/abc');
    // RecipeDetailView fetches via api.getRecipe then renders the name as a heading
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /Trail Pasta/i })).toBeInTheDocument()
    );
  });

  it('(5) /recipes/abc/edit — edit form renders pre-populated with recipe name', async () => {
    renderAt('/recipes/abc/edit');
    await waitFor(() => expect(screen.getByText('Edit Provision')).toBeInTheDocument());
    // Input should be pre-populated from the fetched recipe
    expect(screen.getByDisplayValue('Trail Pasta')).toBeInTheDocument();
  });

  it('(6) /recipes/missing — not-found state renders with back link', async () => {
    renderAt('/recipes/missing');
    await waitFor(() =>
      expect(screen.getByText('Recipe not found.')).toBeInTheDocument()
    );
    const backLink = screen.getByRole('link', { name: /back to recipes/i });
    expect(backLink).toBeInTheDocument();
    expect(backLink).toHaveAttribute('href', '/recipes');
  });
});
