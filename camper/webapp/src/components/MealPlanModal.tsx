import { useState, useEffect, useCallback, useRef } from 'react';
import {
  api,
  type MealPlanDetailResponse,
  type MealPlanDayResponse,
  type MealPlanResponse,
  type RecipeResponse,
  type RecipeDetailResponse,
  type ShoppingListResponse,
  type ShoppingListItemResponse,
  type MealsByTypeResponse,
} from '../api/client';
import { Button } from './ui/Button';
import { Modal } from './ui/Modal';
import './MealPlanModal.css';

type ViewTab = 'overview' | 'recipes' | 'shopping';
type MealType = keyof MealsByTypeResponse;

const MEAL_TYPES: { key: MealType; label: string; icon: string }[] = [
  { key: 'breakfast', label: 'Breakfast', icon: '\u2600' },
  { key: 'lunch', label: 'Lunch', icon: '\u25D0' },
  { key: 'dinner', label: 'Dinner', icon: '\u263D' },
  { key: 'snack', label: 'Snacks', icon: '\u2726' },
];

interface MealPlanModalProps {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
}

export function MealPlanModal({ isOpen, onClose, planId }: MealPlanModalProps) {
  const [activeView, setActiveView] = useState<ViewTab>('overview');
  const [mealPlan, setMealPlan] = useState<MealPlanDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeDay, setActiveDay] = useState(0);

  // Recipe Book state
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeDetailResponse | null>(null);
  const [recipeSearch, setRecipeSearch] = useState('');
  const [mealFilter, setMealFilter] = useState<string | null>(null);
  const [themeFilter, setThemeFilter] = useState<string | null>(null);
  const [addingToMeal, setAddingToMeal] = useState<{ recipeId: string; recipeName: string } | null>(null);
  const [addDay, setAddDay] = useState('');
  const [addMealType, setAddMealType] = useState<MealType>('breakfast');

  // Shopping List state
  const [shoppingList, setShoppingList] = useState<ShoppingListResponse | null>(null);
  const [resetConfirm, setResetConfirm] = useState(false);

  // Create form state
  const [createName, setCreateName] = useState('');
  const [createServings, setCreateServings] = useState(4);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Template state
  const [templates, setTemplates] = useState<MealPlanResponse[]>([]);
  const [showSaveTemplate, setShowSaveTemplate] = useState(false);
  const [templateName, setTemplateName] = useState('');
  const [savingTemplate, setSavingTemplate] = useState(false);
  const [showLoadTemplate, setShowLoadTemplate] = useState(false);
  const [loadingTemplate, setLoadingTemplate] = useState(false);
  const [, setReplaceTemplateId] = useState<string | null>(null);
  const [templatePreview, setTemplatePreview] = useState<MealPlanDetailResponse | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);

  const loadMealPlan = useCallback(async () => {
    try {
      const data = await api.getMealPlanForTrip(planId);
      setMealPlan(data);
      if (data && data.days.length > 0) {
        setActiveDay(prev => prev >= data.days.length ? 0 : prev);
      }
    } catch {
      setMealPlan(null);
    } finally {
      setLoading(false);
    }
  }, [planId]);

  const loadRecipes = useCallback(async () => {
    try {
      const data = await api.getRecipes();
      setRecipes(data.filter(r => r.status === 'published'));
    } catch {
      // fail silently
    }
  }, []);

  const loadShoppingList = useCallback(async () => {
    if (!mealPlan) return;
    try {
      const data = await api.getShoppingList(mealPlan.id);
      setShoppingList(data);
    } catch {
      setShoppingList(null);
    }
  }, [mealPlan]);

  const loadTemplates = useCallback(async () => {
    try {
      const data = await api.getTemplates();
      setTemplates(data);
    } catch {
      setTemplates([]);
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      loadMealPlan();
      loadRecipes();
      loadTemplates();
    }
  }, [isOpen, loadMealPlan, loadRecipes, loadTemplates]);

  useEffect(() => {
    if (activeView === 'shopping' && mealPlan) {
      loadShoppingList();
    }
  }, [activeView, mealPlan, loadShoppingList]);

  const handleCreate = async () => {
    if (!createName.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const created = await api.createMealPlan({
        name: createName.trim(),
        servings: createServings,
        planId,
      });
      const detail = await api.getMealPlanDetail(created.id);
      setMealPlan(detail);
      setActiveDay(0);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create meal plan');
    } finally {
      setCreating(false);
    }
  };

  const handleUpdateName = async (name: string) => {
    if (!mealPlan) return;
    const trimmed = name.trim();
    if (!trimmed || trimmed === mealPlan.name) return;
    try {
      await api.updateMealPlan(mealPlan.id, { name: trimmed });
      await loadMealPlan();
    } catch { /* */ }
  };

  const handleUpdateServings = async (delta: number) => {
    if (!mealPlan) return;
    const newServings = Math.max(1, mealPlan.servings + delta);
    if (newServings === mealPlan.servings) return;
    try {
      await api.updateMealPlan(mealPlan.id, { servings: newServings });
      await loadMealPlan();
    } catch { /* */ }
  };

  const handleAddDay = async () => {
    if (!mealPlan) return;
    const nextDayNumber = mealPlan.days.length > 0
      ? Math.max(...mealPlan.days.map(d => d.dayNumber)) + 1
      : 1;
    try {
      await api.addMealPlanDay(mealPlan.id, nextDayNumber);
      await loadMealPlan();
      setActiveDay(mealPlan.days.length); // will be the new last index after reload
    } catch { /* */ }
  };

  const handleRemoveDay = async (day: MealPlanDayResponse) => {
    if (!mealPlan) return;
    try {
      await api.removeMealPlanDay(mealPlan.id, day.id);
      if (activeDay >= mealPlan.days.length - 1) {
        setActiveDay(Math.max(0, mealPlan.days.length - 2));
      }
      await loadMealPlan();
    } catch { /* */ }
  };

  const handleRemoveRecipe = async (mealPlanRecipeId: string) => {
    try {
      await api.removeRecipeFromMeal(mealPlanRecipeId);
      await loadMealPlan();
    } catch { /* */ }
  };

  const handleSelectRecipe = async (recipe: RecipeResponse) => {
    try {
      const detail = await api.getRecipe(recipe.id);
      setSelectedRecipe(detail);
    } catch { /* */ }
  };

  const handleAddRecipeToMeal = async () => {
    if (!mealPlan || !addingToMeal || !addDay) return;
    try {
      await api.addRecipeToMeal(mealPlan.id, addDay, {
        mealType: addMealType,
        recipeId: addingToMeal.recipeId,
      });
      setAddingToMeal(null);
      setAddDay('');
      await loadMealPlan();
    } catch { /* */ }
  };

  const handleAddRecipeInline = async (dayId: string, mealType: MealType, recipeId: string) => {
    if (!mealPlan) return;
    try {
      await api.addRecipeToMeal(mealPlan.id, dayId, {
        mealType,
        recipeId,
      });
      await loadMealPlan();
    } catch { /* */ }
  };

  const handleTogglePurchase = async (entries: { ingredientId: string; unit: string; quantityRequired: number; quantityPurchased: number }[]) => {
    if (!mealPlan) return;
    // If all entries are fully purchased, unmark all; otherwise mark all as purchased
    const allDone = entries.every(e => e.quantityPurchased >= e.quantityRequired);
    try {
      await Promise.all(entries.map(e =>
        api.updatePurchase(mealPlan.id, {
          ingredientId: e.ingredientId,
          unit: e.unit,
          quantityPurchased: allDone ? 0 : e.quantityRequired,
        })
      ));
      await loadShoppingList();
    } catch { /* */ }
  };

  const handleResetPurchases = async () => {
    if (!mealPlan) return;
    try {
      await api.resetPurchases(mealPlan.id);
      setResetConfirm(false);
      await loadShoppingList();
    } catch { /* */ }
  };

  const handleSaveAsTemplate = async () => {
    if (!mealPlan || !templateName.trim()) return;
    setSavingTemplate(true);
    try {
      await api.saveAsTemplate(mealPlan.id, { name: templateName.trim() });
      setShowSaveTemplate(false);
      setTemplateName('');
      await loadTemplates();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save template');
    } finally {
      setSavingTemplate(false);
    }
  };

  const handlePreviewTemplate = async (templateId: string) => {
    setLoadingPreview(true);
    try {
      const detail = await api.getMealPlanDetail(templateId);
      setTemplatePreview(detail);
      setReplaceTemplateId(templateId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load template preview');
    } finally {
      setLoadingPreview(false);
    }
  };

  const handleLoadTemplate = async (templateId: string) => {
    setLoadingTemplate(true);
    setError(null);
    try {
      // Preserve the existing meal plan's name and servings
      const previousName = mealPlan?.name;
      const previousServings = mealPlan?.servings ?? createServings;
      // If there's an existing meal plan, delete it first
      if (mealPlan) {
        await api.deleteMealPlan(mealPlan.id);
      }
      const detail = await api.copyTemplateToTrip(templateId, {
        planId,
        servings: previousServings,
      });
      // Rename back to the original name (don't adopt the template name)
      if (previousName && previousName !== detail.name) {
        await api.updateMealPlan(detail.id, { name: previousName });
        detail.name = previousName;
      }
      setMealPlan(detail);
      setActiveDay(0);
      setShowLoadTemplate(false);
      setReplaceTemplateId(null);
      setTemplatePreview(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load template');
      // Reload in case the delete succeeded but copy failed
      await loadMealPlan();
    } finally {
      setLoadingTemplate(false);
    }
  };

  const currentDay = mealPlan?.days[activeDay] ?? null;

  // Filter recipes for book view
  const filteredRecipes = recipes.filter(r => {
    if (recipeSearch && !r.name.toLowerCase().includes(recipeSearch.toLowerCase())) return false;
    if (mealFilter && r.meal !== mealFilter) return false;
    if (themeFilter && r.theme !== themeFilter) return false;
    return true;
  });

  const uniqueMeals = [...new Set(recipes.map(r => r.meal).filter(Boolean))] as string[];
  const uniqueThemes = [...new Set(recipes.map(r => r.theme).filter(Boolean))] as string[];

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="xl" className="meal-plan-modal">
        {/* Header */}
        <div className="mp-header">
          <div className="mp-header-left">
            <div className="mp-header-icon">
              <svg width="32" height="32" viewBox="0 0 48 48">
                <ellipse cx="24" cy="28" rx="16" ry="5" fill="var(--charcoal-light)" />
                <path d="M8,28 Q8,42 24,42 Q40,42 40,28" fill="var(--charcoal-light)" />
                <path d="M18,18 Q16,10 20,4" fill="none" stroke="rgba(255,255,255,0.4)" strokeWidth="2" strokeLinecap="round" />
                <path d="M24,16 Q22,8 26,2" fill="none" stroke="rgba(255,255,255,0.3)" strokeWidth="2" strokeLinecap="round" />
                <path d="M30,18 Q28,10 32,6" fill="none" stroke="rgba(255,255,255,0.25)" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <h2 className="mp-title">Camp Kitchen</h2>
          </div>

          <div className="mp-tabs">
            {([
              { key: 'overview' as ViewTab, label: 'Overview' },
              { key: 'recipes' as ViewTab, label: 'Recipe Book' },
              { key: 'shopping' as ViewTab, label: 'Shopping List' },
            ]).map(tab => (
              <button
                key={tab.key}
                className={`mp-tab ${activeView === tab.key ? 'mp-tab--active' : ''}`}
                onClick={() => setActiveView(tab.key)}
              >
                {tab.label}
              </button>
            ))}
          </div>

        </div>

        {/* Body */}
        <div className="mp-body">
          {loading ? (
            <div className="mp-loading">
              <div className="mp-loading-pot" />
              <p>Heating up the camp stove...</p>
            </div>
          ) : activeView === 'overview' ? (
            <OverviewView
              mealPlan={mealPlan}
              currentDay={currentDay}
              activeDay={activeDay}
              setActiveDay={setActiveDay}
              onAddDay={handleAddDay}
              onRemoveDay={handleRemoveDay}
              onRemoveRecipe={handleRemoveRecipe}
              onUpdateName={handleUpdateName}
              onUpdateServings={handleUpdateServings}
              createName={createName}
              setCreateName={setCreateName}
              createServings={createServings}
              setCreateServings={setCreateServings}
              creating={creating}
              onCreate={handleCreate}
              error={error}
              templates={templates}
              showLoadTemplate={showLoadTemplate}
              setShowLoadTemplate={setShowLoadTemplate}
              loadingTemplate={loadingTemplate}
              onLoadTemplate={handleLoadTemplate}
              onPreviewTemplate={handlePreviewTemplate}
              templatePreview={templatePreview}
              loadingPreview={loadingPreview}
              setReplaceTemplateId={setReplaceTemplateId}
              setTemplatePreview={setTemplatePreview}
              showSaveTemplate={showSaveTemplate}
              setShowSaveTemplate={setShowSaveTemplate}
              templateName={templateName}
              setTemplateName={setTemplateName}
              savingTemplate={savingTemplate}
              onSaveAsTemplate={handleSaveAsTemplate}
              recipes={recipes}
              onAddRecipeInline={handleAddRecipeInline}
            />
          ) : activeView === 'recipes' ? (
            <RecipeBookView
              recipes={filteredRecipes}
              selectedRecipe={selectedRecipe}
              onSelectRecipe={handleSelectRecipe}
              recipeSearch={recipeSearch}
              setRecipeSearch={setRecipeSearch}
              mealFilter={mealFilter}
              setMealFilter={setMealFilter}
              themeFilter={themeFilter}
              setThemeFilter={setThemeFilter}
              uniqueMeals={uniqueMeals}
              uniqueThemes={uniqueThemes}
              addingToMeal={addingToMeal}
              setAddingToMeal={setAddingToMeal}
              addDay={addDay}
              setAddDay={setAddDay}
              addMealType={addMealType}
              setAddMealType={setAddMealType}
              onAddRecipeToMeal={handleAddRecipeToMeal}
              mealPlan={mealPlan}
              activeDay={activeDay}
            />
          ) : (
            <ShoppingListView
              shoppingList={shoppingList}
              mealPlan={mealPlan}
              onTogglePurchase={handleTogglePurchase}
              onResetPurchases={handleResetPurchases}
              resetConfirm={resetConfirm}
              setResetConfirm={setResetConfirm}
            />
          )}
        </div>
    </Modal>
  );
}

// ── View 1: Overview ──────────────────────────

interface OverviewProps {
  mealPlan: MealPlanDetailResponse | null;
  currentDay: MealPlanDayResponse | null;
  activeDay: number;
  setActiveDay: (i: number) => void;
  onAddDay: () => void;
  onRemoveDay: (day: MealPlanDayResponse) => void;
  onRemoveRecipe: (id: string) => void;
  onUpdateName: (name: string) => void;
  onUpdateServings: (delta: number) => void;
  createName: string;
  setCreateName: (v: string) => void;
  createServings: number;
  setCreateServings: (v: number) => void;
  creating: boolean;
  onCreate: () => void;
  error: string | null;
  templates: MealPlanResponse[];
  showLoadTemplate: boolean;
  setShowLoadTemplate: (v: boolean) => void;
  loadingTemplate: boolean;
  onLoadTemplate: (templateId: string) => void;
  onPreviewTemplate: (templateId: string) => void;
  templatePreview: MealPlanDetailResponse | null;
  loadingPreview: boolean;
  setReplaceTemplateId: (v: string | null) => void;
  setTemplatePreview: (v: MealPlanDetailResponse | null) => void;
  showSaveTemplate: boolean;
  setShowSaveTemplate: (v: boolean) => void;
  templateName: string;
  setTemplateName: (v: string) => void;
  savingTemplate: boolean;
  onSaveAsTemplate: () => void;
  recipes: RecipeResponse[];
  onAddRecipeInline: (dayId: string, mealType: MealType, recipeId: string) => void;
}

function OverviewView({
  mealPlan, currentDay, activeDay, setActiveDay,
  onAddDay, onRemoveDay, onRemoveRecipe, onUpdateName, onUpdateServings,
  createName, setCreateName, createServings, setCreateServings,
  creating, onCreate, error,
  templates, showLoadTemplate, setShowLoadTemplate, loadingTemplate, onLoadTemplate,
  onPreviewTemplate, templatePreview, loadingPreview,
  setReplaceTemplateId, setTemplatePreview,
  showSaveTemplate, setShowSaveTemplate, templateName, setTemplateName, savingTemplate, onSaveAsTemplate,
  recipes, onAddRecipeInline,
}: OverviewProps) {
  const [addingMealType, setAddingMealType] = useState<MealType | null>(null);
  const [inlineSearch, setInlineSearch] = useState('');
  const [editName, setEditName] = useState(mealPlan?.name ?? '');
  // Sync editName when mealPlan name changes externally
  useEffect(() => { if (mealPlan) setEditName(mealPlan.name); }, [mealPlan?.name]);
  if (!mealPlan) {
    return (
      <div className="mp-empty-state">
        <svg width="64" height="64" viewBox="0 0 64 64" className="mp-empty-icon">
          <ellipse cx="32" cy="38" rx="22" ry="7" fill="var(--charcoal-light)" opacity="0.3" />
          <path d="M10,38 Q10,56 32,56 Q54,56 54,38" fill="var(--charcoal-light)" opacity="0.3" />
          <path d="M24,26 Q22,16 26,6" fill="none" stroke="var(--ember)" strokeWidth="2.5" strokeLinecap="round" opacity="0.5" />
          <path d="M32,24 Q30,14 34,4" fill="none" stroke="var(--ember)" strokeWidth="2.5" strokeLinecap="round" opacity="0.4" />
          <path d="M40,26 Q38,16 42,8" fill="none" stroke="var(--ember)" strokeWidth="2.5" strokeLinecap="round" opacity="0.3" />
        </svg>
        <p className="mp-empty-text">No meal plan yet — let's cook up something delicious!</p>
        <div className="mp-create-form">
          <input
            className="mp-create-input"
            value={createName}
            onChange={e => setCreateName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') onCreate(); }}
            placeholder="Meal plan name..."
            disabled={creating}
          />
          <div className="mp-servings-stepper">
            <span className="mp-servings-label">Servings</span>
            <button className="mp-stepper-btn" onClick={() => setCreateServings(Math.max(1, createServings - 1))} disabled={creating}>-</button>
            <span className="mp-stepper-value">{createServings}</span>
            <button className="mp-stepper-btn" onClick={() => setCreateServings(createServings + 1)} disabled={creating}>+</button>
          </div>
          {error && <p className="mp-error">{error}</p>}
          <Button
            className="mp-create-btn"
            onClick={onCreate}
            disabled={creating || !createName.trim()}
          >
            {creating ? 'Creating...' : 'Start Cooking'}
          </Button>
          {templates.length > 0 && (
            <>
              <div className="mp-template-divider">
                <span>or</span>
              </div>
              {showLoadTemplate ? (
                templatePreview ? (
                  <TemplatePreview
                    preview={templatePreview}
                    onConfirm={() => onLoadTemplate(templatePreview.id)}
                    onBack={() => { setTemplatePreview(null); setReplaceTemplateId(null); }}
                    loading={loadingTemplate}
                    isReplace={false}
                  />
                ) : (
                  <div className="mp-template-picker">
                    <p className="mp-template-picker-title">Load from template</p>
                    {templates.map(t => (
                      <button
                        key={t.id}
                        className="mp-template-option"
                        onClick={() => onPreviewTemplate(t.id)}
                        disabled={loadingPreview}
                      >
                        <span className="mp-template-option-name">{t.name}</span>
                        <span className="mp-template-preview-icon" title="View template">
                          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <circle cx="7" cy="7" r="4.5" stroke="currentColor" strokeWidth="1.5" />
                            <line x1="10.5" y1="10.5" x2="14" y2="14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                          </svg>
                        </span>
                      </button>
                    ))}
                    <Button
                      variant="secondary"
                      className="mp-template-cancel"
                      onClick={() => setShowLoadTemplate(false)}
                    >
                      Cancel
                    </Button>
                  </div>
                )
              ) : (
                <Button
                  variant="secondary"
                  className="mp-load-template-btn"
                  onClick={() => setShowLoadTemplate(true)}
                >
                  Load from Template
                </Button>
              )}
            </>
          )}
        </div>
      </div>
    );
  }

  const filteredInlineRecipes = inlineSearch
    ? recipes.filter(r => r.name.toLowerCase().includes(inlineSearch.toLowerCase()))
    : recipes;

  return (
    <div className="mp-overview">
      {/* Plan header: name + servings, then template links */}
      <div className="mp-plan-header">
        <input
          className="mp-plan-name-input"
          value={editName}
          onChange={e => setEditName(e.target.value)}
          onBlur={() => onUpdateName(editName)}
          onKeyDown={e => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur(); }}
        />
        <div className="mp-plan-servings-row">
          <div className="mp-servings-stepper mp-servings-stepper--inline">
            <button className="mp-stepper-btn" onClick={() => onUpdateServings(-1)}>-</button>
            <span className="mp-stepper-value">{mealPlan.servings}</span>
            <button className="mp-stepper-btn" onClick={() => onUpdateServings(1)}>+</button>
            <span className="mp-servings-label">servings per recipe</span>
          </div>
        </div>
        <div className="mp-plan-template-row">
          {/* Template actions */}
          {showSaveTemplate ? (
            <div className="mp-save-template-form">
              <input
                className="mp-save-template-input"
                value={templateName}
                onChange={e => setTemplateName(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') onSaveAsTemplate(); }}
                placeholder="Template name..."
                disabled={savingTemplate}
              />
              <Button
                className="mp-save-template-confirm"
                onClick={onSaveAsTemplate}
                disabled={savingTemplate || !templateName.trim()}
              >
                {savingTemplate ? 'Saving...' : 'Save'}
              </Button>
              <Button
                variant="secondary"
                className="mp-save-template-cancel"
                onClick={() => { setShowSaveTemplate(false); setTemplateName(''); }}
              >
                Cancel
              </Button>
            </div>
          ) : showLoadTemplate ? (
            <div className="mp-replace-template">
              {templatePreview ? (
                <TemplatePreview
                  preview={templatePreview}
                  onConfirm={() => onLoadTemplate(templatePreview.id)}
                  onBack={() => { setTemplatePreview(null); setReplaceTemplateId(null); }}
                  loading={loadingTemplate}
                  isReplace={true}
                />
              ) : (
                <div className="mp-template-picker mp-template-picker--inline">
                  <p className="mp-template-picker-title">Load from template</p>
                  {templates.map(t => (
                    <button
                      key={t.id}
                      className="mp-template-option"
                      onClick={() => onPreviewTemplate(t.id)}
                      disabled={loadingPreview}
                    >
                      <span className="mp-template-option-name">{t.name}</span>
                      <span className="mp-template-preview-icon" title="View template">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                          <circle cx="7" cy="7" r="4.5" stroke="currentColor" strokeWidth="1.5" />
                          <line x1="10.5" y1="10.5" x2="14" y2="14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                        </svg>
                      </span>
                    </button>
                  ))}
                  <Button
                    variant="secondary"
                    className="mp-template-cancel"
                    onClick={() => setShowLoadTemplate(false)}
                  >
                    Cancel
                  </Button>
                </div>
              )}
            </div>
          ) : (
            <div className="mp-template-links">
              <button
                className="mp-template-link"
                onClick={() => { setShowSaveTemplate(true); setTemplateName(mealPlan.name + ' Template'); }}
              >
                Save as Template
              </button>
              {templates.length > 0 && (
                <>
                  <span className="mp-template-sep">|</span>
                  <button
                    className="mp-template-link"
                    onClick={() => setShowLoadTemplate(true)}
                  >
                    Load from Template
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Day tabs */}
      <div className="mp-day-tabs">
        {mealPlan.days.map((day, i) => (
          <div key={day.id} className={`mp-day-tab ${i === activeDay ? 'mp-day-tab--active' : ''}`}>
            <button className="mp-day-tab-btn" onClick={() => setActiveDay(i)}>
              Day {day.dayNumber}
            </button>
            {mealPlan.days.length > 1 && (
              <button className="mp-day-tab-remove" onClick={() => onRemoveDay(day)} title="Remove day">
                <svg width="10" height="10" viewBox="0 0 10 10">
                  <path d="M2,2 L8,8 M8,2 L2,8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </button>
            )}
          </div>
        ))}
        <div className="mp-day-tab mp-day-tab--add">
          <button className="mp-day-tab-btn" onClick={onAddDay} title="Add day">+</button>
        </div>
      </div>

      {/* Meal type sections */}
      {currentDay && MEAL_TYPES.map(({ key, label, icon }) => {
        const dayRecipes = currentDay.meals[key];
        const isAdding = addingMealType === key;
        return (
          <div key={key} className="mp-meal-section">
            <div className="mp-meal-header">
              <span className="mp-meal-icon">{icon}</span>
              <span className="mp-meal-label">{label}</span>
            </div>
            {dayRecipes.length > 0 && (
              <div className="mp-recipe-list">
                {dayRecipes.map(recipe => (
                  <div key={recipe.id} className="mp-recipe-row">
                    <span className="mp-recipe-name">{recipe.recipeName}</span>
                    <button className="mp-recipe-remove" onClick={() => onRemoveRecipe(recipe.id)} title="Remove recipe">
                      <svg width="12" height="12" viewBox="0 0 12 12">
                        <path d="M3,3 L9,9 M9,3 L3,9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}
            {isAdding ? (
              <div className="mp-inline-add">
                <input
                  className="mp-inline-add-search"
                  value={inlineSearch}
                  onChange={e => setInlineSearch(e.target.value)}
                  placeholder="Search recipes..."
                  autoFocus
                />
                <div className="mp-inline-add-results">
                  {filteredInlineRecipes.length > 0 ? (
                    filteredInlineRecipes.map(r => (
                      <button
                        key={r.id}
                        className="mp-inline-add-option"
                        onClick={() => {
                          onAddRecipeInline(currentDay.id, key, r.id);
                          setAddingMealType(null);
                          setInlineSearch('');
                        }}
                      >
                        {r.name}
                      </button>
                    ))
                  ) : (
                    <p className="mp-inline-add-empty">No recipes found</p>
                  )}
                </div>
                <button
                  className="mp-inline-add-cancel"
                  onClick={() => { setAddingMealType(null); setInlineSearch(''); }}
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button
                className="mp-meal-add-btn"
                onClick={() => { setAddingMealType(key); setInlineSearch(''); }}
              >
                + Add Recipe
              </button>
            )}
          </div>
        );
      })}

      {!currentDay && mealPlan.days.length === 0 && (
        <div className="mp-no-days">
          <p>Add a day to start planning meals.</p>
        </div>
      )}
    </div>
  );
}

// ── Template Preview ─────────────────────────

interface TemplatePreviewProps {
  preview: MealPlanDetailResponse;
  onConfirm: () => void;
  onBack: () => void;
  loading: boolean;
  isReplace: boolean;
}

function TemplatePreview({ preview, onConfirm, onBack, loading, isReplace }: TemplatePreviewProps) {
  return (
    <div className="mp-template-preview">
      <div className="mp-template-preview-header">
        <h3 className="mp-template-preview-name">{preview.name}</h3>
        <span className="mp-template-preview-meta">
          {preview.days.length} {preview.days.length === 1 ? 'day' : 'days'} · {preview.servings} servings
        </span>
      </div>

      <div className="mp-template-preview-days">
        {preview.days.map(day => {
          const hasAny = MEAL_TYPES.some(mt => day.meals[mt.key].length > 0);
          return (
            <div key={day.id} className="mp-template-preview-day">
              <span className="mp-template-preview-day-label">Day {day.dayNumber}</span>
              {!hasAny ? (
                <span className="mp-template-preview-empty">No recipes</span>
              ) : (
                <div className="mp-template-preview-meals">
                  {MEAL_TYPES.map(mt => {
                    const recipes = day.meals[mt.key];
                    if (recipes.length === 0) return null;
                    return (
                      <div key={mt.key} className="mp-template-preview-meal-group">
                        <div className="mp-template-preview-meal-header">
                          <span className="mp-template-preview-meal-icon">{mt.icon}</span>
                          <span className="mp-template-preview-meal-label">{mt.label}</span>
                        </div>
                        <div className="mp-template-preview-meal-recipes">
                          {recipes.map((r, i) => (
                            <span key={i} className="mp-template-preview-recipe-name">{r.recipeName}</span>
                          ))}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {isReplace && (
        <p className="mp-replace-warning">
          This will replace your entire meal plan — all days, recipes, and purchases will be lost.
        </p>
      )}

      <div className="mp-template-preview-actions">
        <Button
          variant="secondary"
          onClick={onBack}
          disabled={loading}
        >
          Back
        </Button>
        <Button
          variant={isReplace ? 'danger' : 'primary'}
          className={isReplace ? 'mp-reset-yes' : 'mp-create-btn'}
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? 'Loading...' : isReplace ? 'Replace Meal Plan' : 'Use This Template'}
        </Button>
      </div>
    </div>
  );
}

// ── View 2: Recipe Book ──────────────────────

interface RecipeBookProps {
  recipes: RecipeResponse[];
  selectedRecipe: RecipeDetailResponse | null;
  onSelectRecipe: (r: RecipeResponse) => void;
  recipeSearch: string;
  setRecipeSearch: (v: string) => void;
  mealFilter: string | null;
  setMealFilter: (v: string | null) => void;
  themeFilter: string | null;
  setThemeFilter: (v: string | null) => void;
  uniqueMeals: string[];
  uniqueThemes: string[];
  addingToMeal: { recipeId: string; recipeName: string } | null;
  setAddingToMeal: (v: { recipeId: string; recipeName: string } | null) => void;
  addDay: string;
  setAddDay: (v: string) => void;
  addMealType: MealType;
  setAddMealType: (v: MealType) => void;
  onAddRecipeToMeal: () => void;
  mealPlan: MealPlanDetailResponse | null;
  activeDay: number;
}

function RecipeBookView({
  recipes, selectedRecipe, onSelectRecipe,
  recipeSearch, setRecipeSearch,
  mealFilter, setMealFilter,
  themeFilter, setThemeFilter,
  uniqueMeals, uniqueThemes,
  addingToMeal, setAddingToMeal,
  addDay, setAddDay, addMealType, setAddMealType,
  onAddRecipeToMeal, mealPlan, activeDay,
}: RecipeBookProps) {
  const popoverRef = useRef<HTMLDivElement>(null);

  return (
    <div className="mp-book">
      {/* Left page */}
      <div className="mp-book-page mp-book-left">
        <input
          className="mp-book-search"
          value={recipeSearch}
          onChange={e => setRecipeSearch(e.target.value)}
          placeholder="Search recipes..."
        />
        {(uniqueMeals.length > 0 || uniqueThemes.length > 0) && (
          <div className="mp-book-filters">
            {uniqueMeals.map(meal => (
              <button
                key={meal}
                className={`mp-filter-pill ${mealFilter === meal ? 'mp-filter-pill--active' : ''}`}
                onClick={() => setMealFilter(mealFilter === meal ? null : meal)}
              >
                {meal}
              </button>
            ))}
            {uniqueThemes.map(theme => (
              <button
                key={theme}
                className={`mp-filter-pill mp-filter-pill--theme ${themeFilter === theme ? 'mp-filter-pill--active' : ''}`}
                onClick={() => setThemeFilter(themeFilter === theme ? null : theme)}
              >
                {theme}
              </button>
            ))}
          </div>
        )}
        <div className="mp-book-list">
          {recipes.length === 0 ? (
            <p className="mp-book-empty">No recipes found</p>
          ) : (
            recipes.map(recipe => (
              <button
                key={recipe.id}
                className={`mp-book-item ${selectedRecipe?.id === recipe.id ? 'mp-book-item--selected' : ''}`}
                onClick={() => onSelectRecipe(recipe)}
              >
                <span className="mp-book-item-name">{recipe.name}</span>
                {recipe.meal && <span className="mp-book-item-badge">{recipe.meal}</span>}
              </button>
            ))
          )}
        </div>
      </div>

      {/* Spine */}
      <div className="mp-book-spine" />

      {/* Right page */}
      <div className="mp-book-page mp-book-right">
        {selectedRecipe ? (
          <>
          <div className="mp-book-detail">
            <h3 className="mp-book-detail-name">{selectedRecipe.name}</h3>
            {selectedRecipe.description && (
              <p className="mp-book-detail-desc">{selectedRecipe.description}</p>
            )}
            <div className="mp-book-detail-meta">
              <span>Base servings: {selectedRecipe.baseServings}</span>
              {selectedRecipe.meal && <span className="mp-book-item-badge">{selectedRecipe.meal}</span>}
              {selectedRecipe.theme && <span className="mp-book-item-badge mp-filter-pill--theme">{selectedRecipe.theme}</span>}
            </div>

            {selectedRecipe.ingredients.length > 0 && (
              <div className="mp-book-ingredients">
                <h4 className="mp-book-ingredients-title">Ingredients</h4>
                <ul className="mp-book-ingredients-list">
                  {selectedRecipe.ingredients.map(ing => (
                    <li key={ing.id}>
                      {ing.quantity > 0 && <span className="mp-book-ing-qty">{fmt(ing.quantity)} {ing.unit}</span>}
                      {' '}
                      {ing.ingredient?.name || ing.originalText || 'Unknown ingredient'}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          {mealPlan ? (
            <div className="mp-book-add-area" ref={popoverRef}>
              {addingToMeal?.recipeId === selectedRecipe.id ? (
                <div className="mp-add-popover">
                  <select
                    className="mp-add-select"
                    value={addDay}
                    onChange={e => setAddDay(e.target.value)}
                  >
                    <option value="">Select day...</option>
                    {mealPlan.days.map(day => (
                      <option key={day.id} value={day.id}>Day {day.dayNumber}</option>
                    ))}
                  </select>
                  <select
                    className="mp-add-select"
                    value={addMealType}
                    onChange={e => setAddMealType(e.target.value as MealType)}
                  >
                    {MEAL_TYPES.map(mt => (
                      <option key={mt.key} value={mt.key}>{mt.icon} {mt.label}</option>
                    ))}
                  </select>
                  <div className="mp-add-popover-actions">
                    <Button className="mp-add-confirm" onClick={onAddRecipeToMeal} disabled={!addDay}>
                      Add
                    </Button>
                    <Button variant="secondary" className="mp-add-cancel" onClick={() => setAddingToMeal(null)}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : (
                <Button
                  className="mp-add-to-plan-btn"
                  onClick={() => {
                    setAddingToMeal({ recipeId: selectedRecipe.id, recipeName: selectedRecipe.name });
                    const currentDay = mealPlan.days[activeDay] ?? mealPlan.days[0];
                    if (currentDay) setAddDay(currentDay.id);
                  }}
                >
                  Add to Meal Plan
                </Button>
              )}
            </div>
          ) : (
            <p className="mp-book-no-plan">Create a meal plan first to add recipes</p>
          )}
          </>
        ) : (
          <div className="mp-book-placeholder">
            <svg width="48" height="48" viewBox="0 0 48 48" opacity="0.3">
              <rect x="8" y="4" width="32" height="40" rx="3" fill="none" stroke="var(--tan-dark)" strokeWidth="2" />
              <line x1="14" y1="14" x2="34" y2="14" stroke="var(--tan-dark)" strokeWidth="1.5" />
              <line x1="14" y1="20" x2="30" y2="20" stroke="var(--tan-dark)" strokeWidth="1.5" />
              <line x1="14" y1="26" x2="32" y2="26" stroke="var(--tan-dark)" strokeWidth="1.5" />
              <line x1="14" y1="32" x2="26" y2="32" stroke="var(--tan-dark)" strokeWidth="1.5" />
            </svg>
            <p>Choose a recipe from the left to peek inside</p>
          </div>
        )}
      </div>
    </div>
  );
}

// ── View 3: Shopping List ────────────────────

interface ShoppingListProps {
  shoppingList: ShoppingListResponse | null;
  mealPlan: MealPlanDetailResponse | null;
  onTogglePurchase: (entries: { ingredientId: string; unit: string; quantityRequired: number; quantityPurchased: number }[]) => void;
  onResetPurchases: () => void;
  resetConfirm: boolean;
  setResetConfirm: (v: boolean) => void;
}

// Merge items with same ingredientId but different units into a single display row
interface MergedShoppingItem {
  ingredientId: string;
  ingredientName: string;
  entries: { unit: string; quantityRequired: number; quantityPurchased: number; status: string }[];
  overallStatus: string;
  usedInRecipes: string[];
}

function mergeItemsByIngredient(items: ShoppingListItemResponse[]): MergedShoppingItem[] {
  const grouped = new Map<string, MergedShoppingItem>();
  for (const item of items) {
    // Skip orphaned 0/0 entries (no_longer_needed with no quantity)
    const isOrphan = item.quantityRequired === 0 && item.quantityPurchased === 0;
    if (isOrphan) continue;

    const existing = grouped.get(item.ingredientId);
    if (existing) {
      existing.entries.push({
        unit: item.unit,
        quantityRequired: item.quantityRequired,
        quantityPurchased: item.quantityPurchased,
        status: item.status,
      });
      for (const r of item.usedInRecipes) {
        if (!existing.usedInRecipes.includes(r)) existing.usedInRecipes.push(r);
      }
    } else {
      grouped.set(item.ingredientId, {
        ingredientId: item.ingredientId,
        ingredientName: item.ingredientName,
        entries: [{
          unit: item.unit,
          quantityRequired: item.quantityRequired,
          quantityPurchased: item.quantityPurchased,
          status: item.status,
        }],
        overallStatus: item.status,
        usedInRecipes: [...item.usedInRecipes],
      });
    }
  }
  // Derive overall status from entries
  for (const item of grouped.values()) {
    if (item.entries.every(e => e.status === 'done')) {
      item.overallStatus = 'done';
    } else if (item.entries.some(e => e.status === 'done' || e.status === 'more_needed')) {
      item.overallStatus = 'more_needed';
    } else if (item.entries.every(e => e.status === 'no_longer_needed')) {
      item.overallStatus = 'no_longer_needed';
    } else {
      item.overallStatus = 'not_purchased';
    }
  }
  return [...grouped.values()];
}

function fmt(n: number): string {
  return parseFloat(n.toFixed(2)).toString();
}

function statusClass(status: string): string {
  switch (status) {
    case 'done': return 'mp-status--done';
    case 'more_needed': return 'mp-status--partial';
    case 'no_longer_needed': return 'mp-status--no-longer';
    default: return 'mp-status--pending';
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'done': return 'done';
    case 'more_needed': return 'more needed';
    case 'no_longer_needed': return 'removed';
    default: return 'needed';
  }
}

function ShoppingListView({
  shoppingList, mealPlan, onTogglePurchase, onResetPurchases,
  resetConfirm, setResetConfirm,
}: ShoppingListProps) {
  if (!mealPlan) {
    return (
      <div className="mp-shopping-empty">
        <p>Create a meal plan first to generate a shopping list.</p>
      </div>
    );
  }

  if (!shoppingList || shoppingList.totalItems === 0) {
    return (
      <div className="mp-shopping-empty">
        <p>Add recipes to your meal plan to generate a shopping list.</p>
      </div>
    );
  }

  const progressPct = shoppingList.totalItems > 0
    ? (shoppingList.fullyPurchasedCount / shoppingList.totalItems) * 100
    : 0;

  return (
    <div className="mp-shopping">
      {/* Progress */}
      <div className="mp-shopping-progress">
        <div className="mp-shopping-progress-text">
          {shoppingList.fullyPurchasedCount} of {shoppingList.totalItems} purchased
        </div>
        <div className="mp-shopping-progress-bar">
          <div className="mp-shopping-progress-fill" style={{ width: `${progressPct}%` }} />
        </div>
      </div>

      {/* Categories */}
      {shoppingList.categories.map(cat => {
        const mergedItems = mergeItemsByIngredient(cat.items);
        return (
          <div key={cat.category} className="mp-shopping-category">
            <h4 className="mp-shopping-category-title">{cat.category}</h4>
            {mergedItems.map(item => {
              const isDone = item.overallStatus === 'done';
              const isNoLongerNeeded = item.overallStatus === 'no_longer_needed';

              return (
                <div key={item.ingredientId} className={`mp-shopping-item ${isDone ? 'mp-shopping-item--done' : ''} ${isNoLongerNeeded ? 'mp-shopping-item--done' : ''}`}>
                  <button
                    className={`mp-shopping-check ${isDone ? 'mp-shopping-check--checked' : ''}`}
                    onClick={() => onTogglePurchase(item.entries.map(e => ({
                      ingredientId: item.ingredientId,
                      unit: e.unit,
                      quantityRequired: e.quantityRequired,
                      quantityPurchased: e.quantityPurchased,
                    })))}
                  >
                    {isDone && (
                      <svg width="12" height="12" viewBox="0 0 12 12">
                        <path d="M2,6 L5,9 L10,3" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    )}
                  </button>
                  <span className={`mp-shopping-name ${isNoLongerNeeded ? 'mp-shopping-name--struck' : ''}`}>{item.ingredientName}</span>
                  <span className="mp-shopping-qty">
                    {item.entries.map((e, i) => (
                      <span key={e.unit}>
                        {i > 0 && ' + '}
                        {fmt(e.quantityPurchased)}/{fmt(e.quantityRequired)} {e.unit}
                      </span>
                    ))}
                  </span>
                  <span className={`mp-shopping-status ${statusClass(item.overallStatus)}`}>
                    {statusLabel(item.overallStatus)}
                  </span>
                </div>
              );
            })}
          </div>
        );
      })}

      {/* Reset */}
      <div className="mp-shopping-reset">
        {resetConfirm ? (
          <div className="mp-reset-confirm">
            <span>Reset all purchases?</span>
            <Button variant="danger" className="mp-reset-yes" onClick={onResetPurchases}>Yes, Reset</Button>
            <Button variant="secondary" className="mp-reset-no" onClick={() => setResetConfirm(false)}>Cancel</Button>
          </div>
        ) : (
          <Button variant="secondary" className="mp-reset-btn" onClick={() => setResetConfirm(true)}>
            Reset All Purchases
          </Button>
        )}
      </div>
    </div>
  );
}
