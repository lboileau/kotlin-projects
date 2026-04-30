/**
 * RecipeForm — shared recipe create form extracted from RecipesPage.RecipeCreateView.
 *
 * CSS note: form-specific styles live in RecipesPage.css (the "safe import" approach
 * described in phase-3-plan.md PR 2). Both RecipesPage.tsx and RecipeForm.tsx import
 * RecipesPage.css so the form renders correctly in all contexts (standalone page and
 * nested inside MealPlanModal). A follow-up PR can extract those classes into a
 * dedicated RecipeForm.css if desired.
 */

import { useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import {
  api,
  type IngredientResponse,
  type RecipeResponse,
  type CreateRecipeIngredientRequest,
} from '../api/client';
import { CATEGORIES, MEALS, THEMES, type DraftIngredient } from '../lib/recipeConstants';
import { UNITS } from '../lib/constants';
// Import the CSS that contains all .recipes-form* / .ingredient-picker* / .ingredient-pill* rules.
// Keeping the CSS in RecipesPage.css and importing from both files is the safe approach for this PR.
import '../pages/RecipesPage.css';

export interface RecipeFormProps {
  ingredients: IngredientResponse[];
  /** When the form creates a new ingredient via the picker, parent updates the shared list. */
  setIngredients: Dispatch<SetStateAction<IngredientResponse[]>>;
  /** Called after api.createRecipe resolves. The new RecipeResponse is passed back. */
  onSuccess: (newRecipe: RecipeResponse) => void;
  /** Called when the user clicks Cancel. */
  onCancel: () => void;
  /**
   * Hide the title/subtitle block (useful when the form is nested inside a Modal
   * that already shows a title). Default false.
   */
  hideHeader?: boolean;
  /** Override the submit button label. Default 'Add to Cookbook'. */
  submitLabel?: string;
}

export function RecipeForm({
  ingredients,
  setIngredients,
  onSuccess,
  onCancel,
  hideHeader = false,
  submitLabel = 'Add to Cookbook',
}: RecipeFormProps) {
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createLink, setCreateLink] = useState('');
  const [createServings, setCreateServings] = useState(4);
  const [createMeal, setCreateMeal] = useState('');
  const [createTheme, setCreateTheme] = useState('');
  const [draftIngredients, setDraftIngredients] = useState<DraftIngredient[]>([]);

  const [categorySearch, setCategorySearch] = useState('');
  const [pickerCreateMode, setPickerCreateMode] = useState(false);
  const [pickerNewName, setPickerNewName] = useState('');
  const [pickerNewCategory, setPickerNewCategory] = useState<string>('produce');
  const [pickerNewUnit, setPickerNewUnit] = useState<string>('pieces');
  const [pickerNewQty, setPickerNewQty] = useState<number>(1);
  const [pickerCreating, setPickerCreating] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  const ingredientsByCategory = CATEGORIES.reduce((acc, cat) => {
    const items = ingredients.filter(
      i =>
        i.category === cat &&
        (!categorySearch || i.name.toLowerCase().includes(categorySearch.toLowerCase())) &&
        !draftIngredients.some(d => d.ingredientId === i.id),
    );
    if (items.length > 0) acc.push({ category: cat, items });
    return acc;
  }, [] as { category: string; items: IngredientResponse[] }[]);

  const handleAddDraftIngredient = (ing?: IngredientResponse) => {
    const ingredient = ing ?? null;
    if (!ingredient) return;
    if (draftIngredients.some(d => d.ingredientId === ingredient.id)) {
      setCreateError('That ingredient is already added');
      return;
    }
    setDraftIngredients(prev => [
      ...prev,
      {
        ingredientId: ingredient.id,
        ingredientName: ingredient.name,
        quantity: 1,
        unit: ingredient.defaultUnit,
      },
    ]);
    setCreateError('');
  };

  const handlePickerCreateIngredient = async () => {
    if (!pickerNewName.trim()) return;
    setPickerCreating(true);
    setCreateError('');
    try {
      const newIng = await api.createIngredient({
        name: pickerNewName.trim(),
        category: pickerNewCategory,
        defaultUnit: pickerNewUnit,
      });
      setIngredients(prev => [...prev, newIng]);
      setDraftIngredients(prev => [
        ...prev,
        {
          ingredientId: newIng.id,
          ingredientName: newIng.name,
          quantity: pickerNewQty,
          unit: pickerNewUnit,
        },
      ]);
      setPickerCreateMode(false);
      setPickerNewName('');
      setPickerNewCategory('produce');
      setPickerNewUnit('pieces');
      setPickerNewQty(1);
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create ingredient');
    } finally {
      setPickerCreating(false);
    }
  };

  const handleRemoveDraftIngredient = (ingredientId: string) => {
    setDraftIngredients(prev => prev.filter(d => d.ingredientId !== ingredientId));
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createName.trim()) {
      setCreateError('Name is required');
      return;
    }
    if (createServings < 1) {
      setCreateError('Servings must be at least 1');
      return;
    }
    setCreating(true);
    setCreateError('');
    try {
      const ingredientsList: CreateRecipeIngredientRequest[] = draftIngredients.map(d => ({
        ingredientId: d.ingredientId,
        quantity: d.quantity,
        unit: d.unit,
      }));
      const newRecipe = await api.createRecipe({
        name: createName.trim(),
        description: createDesc.trim() || undefined,
        webLink: createLink.trim() || undefined,
        baseServings: createServings,
        meal: createMeal || undefined,
        theme: createTheme || undefined,
        ingredients: ingredientsList,
      });
      onSuccess(newRecipe);
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create recipe');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="recipes-form-container">
      {!hideHeader && (
        <>
          <h1 className="recipes-form-title">New Provision</h1>
          <p className="recipes-form-subtitle">Add a recipe to the camp cookbook</p>
        </>
      )}

      <form className="recipes-form" onSubmit={handleCreate}>
        <div className="recipes-form-section">
          <h3 className="recipes-form-section-title">Details</h3>

          <div className="recipes-field">
            <label className="recipes-label">Recipe name *</label>
            <input
              className="recipes-input"
              placeholder="e.g. Trailside Oatmeal..."
              value={createName}
              onChange={e => setCreateName(e.target.value)}
              autoFocus
            />
          </div>

          <div className="recipes-field">
            <label className="recipes-label">Description</label>
            <textarea
              className="recipes-input recipes-textarea"
              placeholder="A hearty breakfast for the trail..."
              value={createDesc}
              onChange={e => setCreateDesc(e.target.value)}
              rows={3}
            />
          </div>

          <div className="recipes-row">
            <div className="recipes-field recipes-field--half">
              <label className="recipes-label">Servings *</label>
              <input
                className="recipes-input"
                type="number"
                min={1}
                value={createServings}
                onChange={e => setCreateServings(Number(e.target.value))}
              />
            </div>
            <div className="recipes-field recipes-field--half">
              <label className="recipes-label">Source URL</label>
              <input
                className="recipes-input"
                placeholder="https://..."
                value={createLink}
                onChange={e => setCreateLink(e.target.value)}
              />
            </div>
          </div>

          <div className="recipes-row">
            <div className="recipes-field recipes-field--half">
              <label className="recipes-label">Meal</label>
              <select
                className="recipes-select"
                value={createMeal}
                onChange={e => setCreateMeal(e.target.value)}
              >
                <option value="">—</option>
                {MEALS.map(m => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </div>
            <div className="recipes-field recipes-field--half">
              <label className="recipes-label">Theme</label>
              <select
                className="recipes-select"
                value={createTheme}
                onChange={e => setCreateTheme(e.target.value)}
              >
                <option value="">—</option>
                {THEMES.map(t => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="recipes-form-section">
          <h3 className="recipes-form-section-title">Ingredients</h3>

          {draftIngredients.length > 0 && (
            <ul className="recipes-draft-ingredients">
              {draftIngredients.map(d => (
                <li key={d.ingredientId} className="recipes-draft-ingredient">
                  <input
                    className="recipes-input recipes-input--inline-qty"
                    type="number"
                    min={0.01}
                    step={0.01}
                    value={d.quantity}
                    onChange={e =>
                      setDraftIngredients(prev =>
                        prev.map(x =>
                          x.ingredientId === d.ingredientId
                            ? { ...x, quantity: Number(e.target.value) }
                            : x,
                        ),
                      )
                    }
                  />
                  <select
                    className="recipes-select--inline"
                    value={d.unit}
                    onChange={e =>
                      setDraftIngredients(prev =>
                        prev.map(x =>
                          x.ingredientId === d.ingredientId ? { ...x, unit: e.target.value } : x,
                        ),
                      )
                    }
                  >
                    {UNITS.map(u => (
                      <option key={u} value={u}>
                        {u}
                      </option>
                    ))}
                  </select>
                  <span className="recipes-draft-ingredient__name">{d.ingredientName}</span>
                  <button
                    type="button"
                    className="recipes-draft-ingredient__remove"
                    onClick={() => handleRemoveDraftIngredient(d.ingredientId)}
                  >
                    ×
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div className="ingredient-picker">
            <div className="ingredient-picker__search-wrap">
              <svg
                width="14"
                height="14"
                viewBox="0 0 14 14"
                className="ingredient-picker__search-icon"
              >
                <circle
                  cx="5.5"
                  cy="5.5"
                  r="4"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.3"
                />
                <line
                  x1="8.5"
                  y1="8.5"
                  x2="12"
                  y2="12"
                  stroke="currentColor"
                  strokeWidth="1.3"
                  strokeLinecap="round"
                />
              </svg>
              <input
                className="ingredient-picker__search"
                placeholder="Filter ingredients..."
                value={categorySearch}
                onChange={e => setCategorySearch(e.target.value)}
              />
              {categorySearch && (
                <button
                  type="button"
                  className="ingredient-picker__clear"
                  onClick={() => setCategorySearch('')}
                >
                  ×
                </button>
              )}
            </div>

            <div className="ingredient-picker__categories">
              {ingredientsByCategory.length === 0 ? (
                <p className="ingredient-picker__empty">
                  {categorySearch
                    ? 'No ingredients match your search.'
                    : 'All ingredients have been added!'}
                </p>
              ) : (
                ingredientsByCategory.map(({ category, items }) => (
                  <div key={category} className="ingredient-picker__category">
                    <h4 className="ingredient-picker__category-title">{category}</h4>
                    <div className="ingredient-picker__pills">
                      {items.map(ing => (
                        <button
                          key={ing.id}
                          type="button"
                          className="ingredient-pill"
                          onClick={() => handleAddDraftIngredient(ing)}
                        >
                          <span className="ingredient-pill__name">{ing.name}</span>
                          <span className="ingredient-pill__unit">{ing.defaultUnit}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                ))
              )}
            </div>

            {!pickerCreateMode ? (
              <button
                type="button"
                className="recipe-detail__add-create-btn"
                onClick={() => {
                  setPickerCreateMode(true);
                  setPickerNewName(categorySearch);
                }}
              >
                + Create new ingredient
              </button>
            ) : (
              <div className="recipe-detail__add-create-form">
                <div className="recipe-detail__add-create-row">
                  <input
                    type="text"
                    className="recipes-input"
                    placeholder="Ingredient name"
                    value={pickerNewName}
                    onChange={e => setPickerNewName(e.target.value)}
                    autoFocus
                  />
                </div>
                <div className="recipe-detail__add-create-row">
                  <select
                    className="recipes-select"
                    value={pickerNewCategory}
                    onChange={e => setPickerNewCategory(e.target.value)}
                  >
                    {CATEGORIES.map(c => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    className="recipes-input recipes-input--qty"
                    placeholder="Qty"
                    value={pickerNewQty}
                    onChange={e => setPickerNewQty(Number(e.target.value) || 0)}
                    min={0}
                    step="any"
                  />
                  <select
                    className="recipes-select"
                    value={pickerNewUnit}
                    onChange={e => setPickerNewUnit(e.target.value)}
                  >
                    {UNITS.map(u => (
                      <option key={u} value={u}>
                        {u}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="recipe-detail__add-create-actions">
                  <button
                    type="button"
                    className="recipe-detail__add-create-cancel"
                    onClick={() => setPickerCreateMode(false)}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="recipe-detail__add-create-save"
                    onClick={handlePickerCreateIngredient}
                    disabled={pickerCreating || !pickerNewName.trim()}
                  >
                    {pickerCreating ? 'Creating...' : 'Create & Add'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {createError && <p className="recipes-form-error">{createError}</p>}

        <div className="recipes-form-actions">
          <button type="button" className="recipes-cancel-btn" onClick={onCancel}>
            Cancel
          </button>
          <button
            type="submit"
            className="recipes-submit-btn"
            disabled={creating || !createName.trim()}
          >
            {creating ? 'Creating...' : submitLabel}
          </button>
        </div>
      </form>
    </div>
  );
}
