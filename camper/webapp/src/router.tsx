import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { RouteFallback } from './components/RouteFallback';
import { RouteError } from './components/RouteError';
import { RequireAuth } from './auth/RequireAuth';

// Route-level code splitting, grouped by area (see pages/*/index.ts
// barrels): the shell, tab bar, auth, http, query client and sync stay
// in the main chunk via ordinary static imports above and elsewhere;
// everything below only downloads when its area is first visited. Every
// route in an area shares one dynamic import() specifier, so they all
// resolve from the same chunk — opening a sheet after its page has
// already loaded needs no further network fetch.
const SignInPage = lazy(() => import('./pages/sign-in/SignInPage').then((m) => ({ default: m.SignInPage })));
const AccountPage = lazy(() => import('./pages/account/AccountPage').then((m) => ({ default: m.AccountPage })));

const PlansPage = lazy(() => import('./pages/plans').then((m) => ({ default: m.PlansPage })));
const NewPlanSheet = lazy(() => import('./pages/plans').then((m) => ({ default: m.NewPlanSheet })));
const PlanDetailPage = lazy(() => import('./pages/plans').then((m) => ({ default: m.PlanDetailPage })));
const AddRecipeToPlanSheet = lazy(() => import('./pages/plans').then((m) => ({ default: m.AddRecipeToPlanSheet })));
const EditPlanSheet = lazy(() => import('./pages/plans').then((m) => ({ default: m.EditPlanSheet })));

const ShoppingPage = lazy(() => import('./pages/shopping').then((m) => ({ default: m.ShoppingPage })));
const SwitchPlanSheet = lazy(() => import('./pages/shopping').then((m) => ({ default: m.SwitchPlanSheet })));
const ShoppingRedirect = lazy(() => import('./pages/shopping').then((m) => ({ default: m.ShoppingRedirect })));

const RecipesPage = lazy(() => import('./pages/recipes').then((m) => ({ default: m.RecipesPage })));
const ImportRecipeSheet = lazy(() => import('./pages/recipes').then((m) => ({ default: m.ImportRecipeSheet })));
const NewRecipePage = lazy(() => import('./pages/recipes').then((m) => ({ default: m.NewRecipePage })));
const RecipeDetailPage = lazy(() => import('./pages/recipes').then((m) => ({ default: m.RecipeDetailPage })));
const EditRecipePage = lazy(() => import('./pages/recipes').then((m) => ({ default: m.EditRecipePage })));
const AddLineSheet = lazy(() => import('./pages/recipes').then((m) => ({ default: m.AddLineSheet })));
const EditLineSheet = lazy(() => import('./pages/recipes').then((m) => ({ default: m.EditLineSheet })));
const AddToPlanSheet = lazy(() => import('./pages/recipes').then((m) => ({ default: m.AddToPlanSheet })));

const IngredientsPage = lazy(() => import('./pages/ingredients').then((m) => ({ default: m.IngredientsPage })));
const NewIngredientSheet = lazy(() =>
  import('./pages/ingredients').then((m) => ({ default: m.NewIngredientSheet })),
);
const EditIngredientSheet = lazy(() =>
  import('./pages/ingredients').then((m) => ({ default: m.EditIngredientSheet })),
);

// Full route table from the frontend plan (section 3). Pages not built
// yet render a small placeholder (see components/Placeholder.tsx) so
// navigation, the tab bar, and deep links can all be exercised now.
// Sheets are child routes rendered via <Outlet/> over their parent page.
export const router = createBrowserRouter([
  {
    path: '/sign-in',
    element: (
      <Suspense fallback={<RouteFallback />}>
        <SignInPage />
      </Suspense>
    ),
    errorElement: <RouteError />,
  },
  {
    element: <RequireAuth />,
    // Covers every guarded route below (AppShell and all its children):
    // an error without its own errorElement bubbles up to the nearest
    // ancestor that has one, so nothing further down needs to repeat this.
    errorElement: <RouteError />,
    children: [
      {
        // AppShell wraps its <Outlet/> in the one Suspense boundary
        // every lazy route below renders through.
        element: <AppShell />,
        children: [
          {
            path: 'plans',
            element: <PlansPage />,
            children: [{ path: 'new', element: <NewPlanSheet /> }],
          },
          {
            path: 'plans/:planId',
            element: <PlanDetailPage />,
            children: [
              { path: 'add', element: <AddRecipeToPlanSheet /> },
              { path: 'edit', element: <EditPlanSheet /> },
            ],
          },
          {
            path: 'plans/:planId/shopping',
            element: <ShoppingPage />,
            children: [{ path: 'switch', element: <SwitchPlanSheet /> }],
          },
          { path: 'shopping', element: <ShoppingRedirect /> },
          {
            path: 'recipes',
            element: <RecipesPage />,
            children: [{ path: 'import', element: <ImportRecipeSheet /> }],
          },
          { path: 'recipes/new', element: <NewRecipePage /> },
          {
            path: 'recipes/:recipeId',
            element: <RecipeDetailPage />,
            children: [
              { path: 'lines/new', element: <AddLineSheet /> },
              { path: 'lines/:lineId', element: <EditLineSheet /> },
              { path: 'add-to-plan', element: <AddToPlanSheet /> },
            ],
          },
          {
            path: 'recipes/:recipeId/edit',
            element: <EditRecipePage />,
            children: [
              { path: 'lines/new', element: <AddLineSheet /> },
              { path: 'lines/:lineId', element: <EditLineSheet /> },
            ],
          },
          {
            path: 'ingredients',
            element: <IngredientsPage />,
            children: [
              { path: 'new', element: <NewIngredientSheet /> },
              { path: ':ingredientId', element: <EditIngredientSheet /> },
            ],
          },
          { path: 'account', element: <AccountPage /> },
          { path: '*', element: <Navigate to="/plans" replace /> },
        ],
      },
    ],
  },
]);
