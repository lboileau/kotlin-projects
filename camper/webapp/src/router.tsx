import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { RequireAuth } from './auth/RequireAuth';
import { SignInPage } from './pages/sign-in/SignInPage';
import { AccountPage } from './pages/account/AccountPage';
import { PlansPage } from './pages/plans/PlansPage';
import { NewPlanSheet } from './pages/plans/NewPlanSheet';
import { PlanDetailPage } from './pages/plans/PlanDetailPage';
import { AddRecipeToPlanSheet } from './pages/plans/AddRecipeToPlanSheet';
import { EditPlanSheet } from './pages/plans/EditPlanSheet';
import { ShoppingPage } from './pages/shopping/ShoppingPage';
import { SwitchPlanSheet } from './pages/shopping/SwitchPlanSheet';
import { ShoppingRedirect } from './pages/shopping/ShoppingRedirect';
import { RecipesPage } from './pages/recipes/RecipesPage';
import { ImportRecipeSheet } from './pages/recipes/ImportRecipeSheet';
import { NewRecipePage } from './pages/recipes/NewRecipePage';
import { RecipeDetailPage } from './pages/recipes/RecipeDetailPage';
import { EditRecipePage } from './pages/recipes/EditRecipePage';
import { AddLineSheet } from './pages/recipes/AddLineSheet';
import { EditLineSheet } from './pages/recipes/EditLineSheet';
import { AddToPlanSheet } from './pages/recipes/AddToPlanSheet';
import { IngredientsPage } from './pages/ingredients/IngredientsPage';
import { NewIngredientSheet } from './pages/ingredients/NewIngredientSheet';
import { EditIngredientSheet } from './pages/ingredients/EditIngredientSheet';

// Full route table from the frontend plan (section 3). Pages not built
// yet render a small placeholder (see components/Placeholder.tsx) so
// navigation, the tab bar, and deep links can all be exercised now.
// Sheets are child routes rendered via <Outlet/> over their parent page.
export const router = createBrowserRouter([
  { path: '/sign-in', element: <SignInPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
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
