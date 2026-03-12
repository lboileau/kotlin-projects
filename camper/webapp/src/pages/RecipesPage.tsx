import { useEffect, useState } from 'react';
import {
  api,
  type RecipeResponse,
  type RecipeDetailResponse,
  type IngredientResponse,
  type CreateRecipeIngredientRequest,
  type CreateIngredientRequest,
} from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AppHeader } from '../components/AppHeader';
import './RecipesPage.css';
import '../components/Modal.css';

const UNITS = ['g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'oz', 'lb', 'pieces', 'whole', 'bunch', 'can', 'clove', 'pinch', 'slice', 'sprig'] as const;
const CATEGORIES = ['produce', 'dairy', 'meat', 'seafood', 'pantry', 'spice', 'condiment', 'frozen', 'bakery', 'other'] as const;
const MEALS = ['breakfast', 'lunch', 'dinner', 'snack', 'dessert', 'appetizer', 'side', 'drink'] as const;
const THEMES = ['chicken', 'beef', 'pork', 'fish', 'seafood', 'vegetarian', 'vegan', 'pasta', 'soup', 'salad', 'other'] as const;

type View = 'list' | 'detail' | 'create' | 'edit' | 'import';

interface DraftIngredient {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}

export function RecipesPage() {
  const { user } = useAuth();

  const [view, setView] = useState<View>('list');
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeDetailResponse | null>(null);
  const [ingredients, setIngredients] = useState<IngredientResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [mealTab, setMealTab] = useState<string | null>(null);

  // Create form state
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createLink, setCreateLink] = useState('');
  const [createServings, setCreateServings] = useState(4);
  const [createMeal, setCreateMeal] = useState('');
  const [createTheme, setCreateTheme] = useState('');
  const [draftIngredients, setDraftIngredients] = useState<DraftIngredient[]>([]);
  const [ingredientSearch, setIngredientSearch] = useState('');
  const [ingredientSearchOpen, setIngredientSearchOpen] = useState(false);
  const [pendingIngredient, setPendingIngredient] = useState<IngredientResponse | null>(null);
  const [pendingQty, setPendingQty] = useState<number>(1);
  const [pendingUnit, setPendingUnit] = useState<string>('pieces');
  const [categorySearch, setCategorySearch] = useState('');
  const [pickerCreateMode, setPickerCreateMode] = useState(false);
  const [pickerNewName, setPickerNewName] = useState('');
  const [pickerNewCategory, setPickerNewCategory] = useState<string>('produce');
  const [pickerNewUnit, setPickerNewUnit] = useState<string>('pieces');
  const [pickerNewQty, setPickerNewQty] = useState<number>(1);
  const [pickerCreating, setPickerCreating] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  // Edit form state
  const [editName, setEditName] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editServings, setEditServings] = useState(4);
  const [editMeal, setEditMeal] = useState('');
  const [editTheme, setEditTheme] = useState('');
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState('');

  // Import state
  const [importUrl, setImportUrl] = useState('');
  const [importing, setImporting] = useState(false);
  const [importError, setImportError] = useState('');

  // Ingredient resolve modal state
  const [resolveModalIngredient, setResolveModalIngredient] = useState<RecipeDetailResponse['ingredients'][0] | null>(null);
  const [resolveSelectedId, setResolveSelectedId] = useState<string | null>(null);
  const [resolveSearchQuery, setResolveSearchQuery] = useState('');
  const [resolveCreateMode, setResolveCreateMode] = useState(false);
  const [newIngName, setNewIngName] = useState('');
  const [newIngCategory, setNewIngCategory] = useState<string>('produce');
  const [newIngUnit, setNewIngUnit] = useState<string>('pieces');
  const [resolveQty, setResolveQty] = useState<number>(1);
  const [resolveUnit, setResolveUnit] = useState<string>('pieces');
  const [resolving, setResolving] = useState(false);
  const [resolveError, setResolveError] = useState('');
  const [publishing, setPublishing] = useState(false);

  // Delete confirmation
  const [deletingRecipe, setDeletingRecipe] = useState<RecipeDetailResponse | null>(null);

  // Add ingredient (detail view)
  const [addIngredientOpen, setAddIngredientOpen] = useState(false);
  const [addIngredientSearch, setAddIngredientSearch] = useState('');
  const [addCreateMode, setAddCreateMode] = useState(false);
  const [addNewName, setAddNewName] = useState('');
  const [addNewCategory, setAddNewCategory] = useState<string>('produce');
  const [addNewUnit, setAddNewUnit] = useState<string>('pieces');
  const [addNewQty, setAddNewQty] = useState<number>(1);
  const [addCreating, setAddCreating] = useState(false);
  const [addError, setAddError] = useState('');

  useEffect(() => {
    Promise.all([api.getRecipes(), api.getIngredients()])
      .then(([r, i]) => {
        setRecipes(r);
        setIngredients(i);
      })
      .catch(() => setError('Failed to load recipes'))
      .finally(() => setLoading(false));
  }, []);

  const handleViewRecipe = async (recipe: RecipeResponse) => {
    setDetailLoading(true);
    setError('');
    try {
      const detail = await api.getRecipe(recipe.id);
      setSelectedRecipe(detail);
      setResolveModalIngredient(null);
      setResolveError('');
      setView('detail');
    } catch {
      setError('Failed to load recipe details');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleBackToList = () => {
    setView('list');
    setSelectedRecipe(null);
    setError('');
  };

  const handleOpenCreate = () => {
    setCreateName('');
    setCreateDesc('');
    setCreateLink('');
    setCreateServings(4);
    setDraftIngredients([]);
    setIngredientSearch('');
    setPendingIngredient(null);
    setPendingQty(1);
    setPendingUnit('pieces');
    setCreateError('');
    setView('create');
  };

  const handleOpenImport = () => {
    setImportUrl('');
    setImportError('');
    setView('import');
  };

  const handleAddDraftIngredient = (ing?: IngredientResponse) => {
    const ingredient = ing ?? pendingIngredient;
    if (!ingredient) return;
    if (draftIngredients.some(d => d.ingredientId === ingredient.id)) {
      setCreateError('That ingredient is already added');
      return;
    }
    setDraftIngredients(prev => [...prev, {
      ingredientId: ingredient.id,
      ingredientName: ingredient.name,
      quantity: ing ? 1 : pendingQty,
      unit: ing ? ingredient.defaultUnit : pendingUnit,
    }]);
    if (!ing) {
      setPendingIngredient(null);
      setIngredientSearch('');
      setPendingQty(1);
      setPendingUnit('pieces');
    }
    setCreateError('');
  };

  const handlePickerCreateIngredient = async () => {
    if (!pickerNewName.trim()) return;
    setPickerCreating(true);
    setCreateError('');
    try {
      const newIng = await api.createIngredient({ name: pickerNewName.trim(), category: pickerNewCategory, defaultUnit: pickerNewUnit });
      setIngredients(prev => [...prev, newIng]);
      setDraftIngredients(prev => [...prev, {
        ingredientId: newIng.id,
        ingredientName: newIng.name,
        quantity: pickerNewQty,
        unit: pickerNewUnit,
      }]);
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
    if (!createName.trim()) { setCreateError('Name is required'); return; }
    if (createServings < 1) { setCreateError('Servings must be at least 1'); return; }
    setCreating(true);
    setCreateError('');
    try {
      const ingredientsList: CreateRecipeIngredientRequest[] = draftIngredients.map(d => ({
        ingredientId: d.ingredientId,
        quantity: d.quantity,
        unit: d.unit,
      }));
      const recipe = await api.createRecipe({
        name: createName.trim(),
        description: createDesc.trim() || undefined,
        webLink: createLink.trim() || undefined,
        baseServings: createServings,
        meal: createMeal || undefined,
        theme: createTheme || undefined,
        ingredients: ingredientsList,
      });
      setRecipes(prev => [recipe, ...prev]);
      setView('list');
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create recipe');
    } finally {
      setCreating(false);
    }
  };

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!importUrl.trim()) return;
    setImporting(true);
    setImportError('');
    try {
      const detail = await api.importRecipe({ url: importUrl.trim() });
      const recipeEntry: RecipeResponse = {
        id: detail.id,
        name: detail.name,
        description: detail.description,
        webLink: detail.webLink,
        baseServings: detail.baseServings,
        status: detail.status,
        createdBy: detail.createdBy,
        duplicateOfId: detail.duplicateOf?.id ?? null,
        createdAt: detail.createdAt,
        updatedAt: detail.updatedAt,
      };
      setRecipes(prev => [recipeEntry, ...prev]);
      setSelectedRecipe(detail);
      setResolveModalIngredient(null);
      setResolveError('');
      setView('detail');
    } catch (err) {
      setImportError(err instanceof Error ? err.message : 'Failed to import recipe');
    } finally {
      setImporting(false);
    }
  };

  const handleOpenEdit = () => {
    if (!selectedRecipe) return;
    setEditName(selectedRecipe.name);
    setEditDesc(selectedRecipe.description || '');
    setEditServings(selectedRecipe.baseServings);
    setEditMeal(selectedRecipe.meal || '');
    setEditTheme(selectedRecipe.theme || '');
    setEditError('');
    setView('edit');
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRecipe) return;
    if (!editName.trim()) { setEditError('Name is required'); return; }
    setSaving(true);
    setEditError('');
    try {
      const updated = await api.updateRecipe(selectedRecipe.id, {
        name: editName.trim(),
        description: editDesc.trim() || undefined,
        baseServings: editServings,
        meal: editMeal || undefined,
        theme: editTheme || undefined,
      });
      setRecipes(prev => prev.map(r => r.id === updated.id ? updated : r));
      const detail = await api.getRecipe(updated.id);
      setSelectedRecipe(detail);
      setView('detail');
    } catch (err) {
      setEditError(err instanceof Error ? err.message : 'Failed to save changes');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deletingRecipe) return;
    try {
      await api.deleteRecipe(deletingRecipe.id);
      setRecipes(prev => prev.filter(r => r.id !== deletingRecipe.id));
      if (selectedRecipe?.id === deletingRecipe.id) {
        setSelectedRecipe(null);
        setView('list');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete recipe');
    } finally {
      setDeletingRecipe(null);
    }
  };

  const refreshDetail = async (recipeId: string) => {
    const detail = await api.getRecipe(recipeId);
    setSelectedRecipe(detail);
    const entry: RecipeResponse = {
      id: detail.id,
      name: detail.name,
      description: detail.description,
      webLink: detail.webLink,
      baseServings: detail.baseServings,
      status: detail.status,
      createdBy: detail.createdBy,
      duplicateOfId: detail.duplicateOf?.id ?? null,
      createdAt: detail.createdAt,
      updatedAt: detail.updatedAt,
    };
    setRecipes(prev => prev.map(r => r.id === entry.id ? entry : r));
  };

  const openResolveModal = (ri: RecipeDetailResponse['ingredients'][0]) => {
    setResolveModalIngredient(ri);
    setResolveSelectedId(ri.matchedIngredient?.id ?? ri.ingredient?.id ?? null);
    setResolveSearchQuery('');
    setResolveCreateMode(false);
    setNewIngName(ri.suggestedIngredientName ?? '');
    setNewIngCategory(ri.suggestedCategory ?? 'produce');
    setNewIngUnit(ri.suggestedUnit ?? ri.matchedIngredient?.defaultUnit ?? ri.ingredient?.defaultUnit ?? 'pieces');
    setResolveQty(ri.quantity);
    setResolveUnit(ri.unit);
    setResolveError('');
  };

  const closeResolveModal = () => {
    setResolveModalIngredient(null);
    setResolveError('');
  };

  const handleResolveSave = async () => {
    if (!selectedRecipe || !resolveModalIngredient) return;
    setResolving(true);
    setResolveError('');
    try {
      if (resolveCreateMode) {
        if (!newIngName.trim()) { setResolveError('Ingredient name is required'); setResolving(false); return; }
        const newIngredient: CreateIngredientRequest = {
          name: newIngName.trim(),
          category: newIngCategory,
          defaultUnit: newIngUnit,
        };
        await api.resolveIngredient(selectedRecipe.id, resolveModalIngredient.id, { action: 'CREATE_NEW', newIngredient, quantity: resolveQty, unit: resolveUnit });
        const updatedIngredients = await api.getIngredients();
        setIngredients(updatedIngredients);
      } else if (resolveSelectedId) {
        // Is this the matched ingredient? Use CONFIRM_MATCH. Otherwise SELECT_EXISTING.
        if (resolveSelectedId === resolveModalIngredient.matchedIngredient?.id) {
          await api.resolveIngredient(selectedRecipe.id, resolveModalIngredient.id, { action: 'CONFIRM_MATCH', quantity: resolveQty, unit: resolveUnit });
        } else {
          await api.resolveIngredient(selectedRecipe.id, resolveModalIngredient.id, { action: 'SELECT_EXISTING', ingredientId: resolveSelectedId, quantity: resolveQty, unit: resolveUnit });
        }
      } else {
        setResolveError('Select an ingredient or create a new one');
        setResolving(false);
        return;
      }
      await refreshDetail(selectedRecipe.id);
      closeResolveModal();
    } catch (err) {
      setResolveError(err instanceof Error ? err.message : 'Failed to resolve ingredient');
    } finally {
      setResolving(false);
    }
  };

  const handleRemoveIngredient = async (recipeIngredientId?: string) => {
    if (!selectedRecipe) return;
    const targetId = recipeIngredientId || resolveModalIngredient?.id;
    if (!targetId) return;
    setResolving(true);
    setResolveError('');
    try {
      await api.removeRecipeIngredient(selectedRecipe.id, targetId);
      await refreshDetail(selectedRecipe.id);
      if (resolveModalIngredient) closeResolveModal();
    } catch (err) {
      setResolveError(err instanceof Error ? err.message : 'Failed to remove ingredient');
    } finally {
      setResolving(false);
    }
  };

  const [acceptingId, setAcceptingId] = useState<string | null>(null);

  // Per-ingredient inline edit state for pending items
  interface PendingEdit {
    qty: number;
    unit: string;
    name: string;
    category: string;
  }
  const [pendingEdits, setPendingEdits] = useState<Record<string, PendingEdit>>({});

  const getPendingEdit = (ri: RecipeDetailResponse['ingredients'][0]): PendingEdit => {
    if (pendingEdits[ri.id]) return pendingEdits[ri.id];
    const matchedName = ri.matchedIngredient?.name ?? ri.suggestedIngredientName ?? '';
    return {
      qty: ri.quantity,
      unit: ri.unit,
      name: matchedName,
      category: ri.matchedIngredient?.category ?? ri.suggestedCategory ?? 'other',
    };
  };

  const updatePendingEdit = (riId: string, patch: Partial<PendingEdit>) => {
    setPendingEdits(prev => {
      const current = prev[riId];
      // If no current entry, we need the default — but we can't access ri here,
      // so the caller should pass the full default on first edit
      return { ...prev, [riId]: { ...current!, ...patch } };
    });
  };

  const initPendingEdit = (ri: RecipeDetailResponse['ingredients'][0], patch: Partial<PendingEdit>) => {
    setPendingEdits(prev => {
      const current = prev[ri.id] ?? getPendingEdit(ri);
      return { ...prev, [ri.id]: { ...current, ...patch } };
    });
  };

  // Determine if the edited name still matches the original suggestion
  const isStillExistingMatch = (ri: RecipeDetailResponse['ingredients'][0], edit: PendingEdit): boolean => {
    if (!ri.matchedIngredient) return false;
    return edit.name.trim().toLowerCase() === ri.matchedIngredient.name.toLowerCase();
  };

  const handleAcceptPending = async (ri: RecipeDetailResponse['ingredients'][0]) => {
    if (!selectedRecipe || acceptingId) return;
    setAcceptingId(ri.id);
    const edit = getPendingEdit(ri);
    try {
      if (ri.matchedIngredient && isStillExistingMatch(ri, edit)) {
        await api.resolveIngredient(selectedRecipe.id, ri.id, {
          action: 'CONFIRM_MATCH',
          quantity: edit.qty,
          unit: edit.unit,
        });
      } else {
        const newIngredient: CreateIngredientRequest = {
          name: edit.name.trim(),
          category: edit.category,
          defaultUnit: edit.unit,
        };
        await api.resolveIngredient(selectedRecipe.id, ri.id, {
          action: 'CREATE_NEW',
          newIngredient,
          quantity: edit.qty,
          unit: edit.unit,
        });
        const updatedIngredients = await api.getIngredients();
        setIngredients(updatedIngredients);
      }
      // Clean up edit state
      setPendingEdits(prev => {
        const next = { ...prev };
        delete next[ri.id];
        return next;
      });
      await refreshDetail(selectedRecipe.id);
    } catch (err) {
      setResolveError(err instanceof Error ? err.message : 'Failed to resolve ingredient');
    } finally {
      setAcceptingId(null);
    }
  };

  const handleAcceptAllSuggestions = async () => {
    if (!selectedRecipe) return;
    const pending = selectedRecipe.ingredients.filter(ri => ri.status === 'pending_review' && (ri.matchedIngredient || ri.suggestedIngredientName));
    for (const ri of pending) {
      await handleAcceptPending(ri);
    }
  };

  const handleAddIngredientToRecipe = async (ing: IngredientResponse) => {
    if (!selectedRecipe) return;
    setAddError('');
    try {
      await api.addRecipeIngredient(selectedRecipe.id, {
        ingredientId: ing.id,
        quantity: 1,
        unit: ing.defaultUnit,
      });
      await refreshDetail(selectedRecipe.id);
    } catch (err) {
      setAddError(err instanceof Error ? err.message : 'Failed to add ingredient');
    }
  };

  const handleCreateAndAddIngredient = async () => {
    if (!selectedRecipe || !addNewName.trim()) return;
    setAddCreating(true);
    setAddError('');
    try {
      const newIng = await api.createIngredient({ name: addNewName.trim(), category: addNewCategory, defaultUnit: addNewUnit });
      setIngredients(prev => [...prev, newIng]);
      await api.addRecipeIngredient(selectedRecipe.id, {
        ingredientId: newIng.id,
        quantity: addNewQty,
        unit: addNewUnit,
      });
      await refreshDetail(selectedRecipe.id);
      setAddCreateMode(false);
      setAddNewName('');
      setAddNewCategory('produce');
      setAddNewUnit('pieces');
      setAddNewQty(1);
    } catch (err) {
      setAddError(err instanceof Error ? err.message : 'Failed to create ingredient');
    } finally {
      setAddCreating(false);
    }
  };

  const handleResolveDuplicate = async (action: 'NOT_DUPLICATE' | 'USE_EXISTING') => {
    if (!selectedRecipe) return;
    setResolving(true);
    setResolveError('');
    try {
      await api.resolveDuplicate(selectedRecipe.id, { action });
      if (action === 'USE_EXISTING') {
        setRecipes(prev => prev.filter(r => r.id !== selectedRecipe.id));
        setSelectedRecipe(null);
        setView('list');
      } else {
        await refreshDetail(selectedRecipe.id);
      }
    } catch (err) {
      setResolveError(err instanceof Error ? err.message : 'Failed to resolve duplicate');
    } finally {
      setResolving(false);
    }
  };

  const handlePublish = async () => {
    if (!selectedRecipe) return;
    setPublishing(true);
    setResolveError('');
    try {
      const updated = await api.publishRecipe(selectedRecipe.id);
      setRecipes(prev => prev.map(r => r.id === updated.id ? updated : r));
      await refreshDetail(updated.id);
    } catch (err) {
      setResolveError(err instanceof Error ? err.message : 'Failed to publish recipe');
    } finally {
      setPublishing(false);
    }
  };

  const filteredIngredients = ingredients.filter(i =>
    i.name.toLowerCase().includes(ingredientSearch.toLowerCase())
  );

  // Group ingredients by category for the pill picker
  const ingredientsByCategory = CATEGORIES.reduce((acc, cat) => {
    const items = ingredients.filter(i =>
      i.category === cat &&
      (!categorySearch || i.name.toLowerCase().includes(categorySearch.toLowerCase())) &&
      !draftIngredients.some(d => d.ingredientId === i.id)
    );
    if (items.length > 0) acc.push({ category: cat, items });
    return acc;
  }, [] as { category: string; items: IngredientResponse[] }[]);

  const filteredRecipes = recipes.filter(r =>
    r.name.toLowerCase().includes(search.toLowerCase()) &&
    (mealTab === null || (r.meal ?? 'uncategorized') === mealTab)
  );

  // Compute which meal tabs exist in the current recipe set (respecting search filter)
  const searchFilteredRecipes = recipes.filter(r =>
    r.name.toLowerCase().includes(search.toLowerCase())
  );
  const availableMealTabs = (() => {
    const mealOrder = ['breakfast', 'lunch', 'dinner', 'snack', 'dessert', 'appetizer', 'side', 'drink', 'uncategorized'];
    const meals = new Set(searchFilteredRecipes.map(r => r.meal ?? 'uncategorized'));
    return mealOrder.filter(m => meals.has(m));
  })();

  const pendingCount = selectedRecipe?.ingredients.filter(i => i.status === 'pending_review').length ?? 0;

  // For the resolve modal: suggested matches and search results
  const resolveModalSuggestions = resolveModalIngredient
    ? [resolveModalIngredient.matchedIngredient, resolveModalIngredient.ingredient]
        .filter((x): x is IngredientResponse => x != null && x.id !== undefined)
        .filter((x, i, arr) => arr.findIndex(a => a.id === x.id) === i)
    : [];
  const resolveSearchResults = resolveSearchQuery.length > 0
    ? ingredients.filter(i => i.name.toLowerCase().includes(resolveSearchQuery.toLowerCase()) && !resolveModalSuggestions.some(s => s.id === i.id))
    : [];
  const resolvePreviewIngredient = resolveCreateMode
    ? null
    : ingredients.find(i => i.id === resolveSelectedId) ?? resolveModalSuggestions.find(s => s.id === resolveSelectedId) ?? null;
  const canPublish = selectedRecipe?.status === 'draft'
    && pendingCount === 0
    && !selectedRecipe.duplicateOf;

  return (
    <div className="recipes-page">
      <ParallaxBackground variant="dusk" />

      <div className="recipes-content">
        <AppHeader
          pageTitle="Camp Provisions"
          pageIcon={
            <svg width="22" height="22" viewBox="0 0 22 22">
              <rect x="3" y="4" width="16" height="14" rx="2" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
              <line x1="7" y1="8" x2="15" y2="8" stroke="var(--ember)" strokeWidth="1.2" strokeLinecap="round" opacity="0.8" />
              <line x1="7" y1="11" x2="15" y2="11" stroke="var(--ember)" strokeWidth="1.2" strokeLinecap="round" opacity="0.6" />
              <line x1="7" y1="14" x2="12" y2="14" stroke="var(--ember)" strokeWidth="1.2" strokeLinecap="round" opacity="0.4" />
            </svg>
          }
        />

        {/* ── List View ── */}
        {view === 'list' && (
          <div className="recipes-list-view">
            <div className="recipes-hero">
              <h1 className="recipes-title">The Recipe Chest</h1>
              <p className="recipes-subtitle">Fuel your expedition with trail-tested provisions</p>
            </div>

            <div className="recipes-toolbar">
              <div className="recipes-search-wrap">
                <svg width="16" height="16" viewBox="0 0 16 16" className="recipes-search-icon">
                  <circle cx="6.5" cy="6.5" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
                  <line x1="10" y1="10" x2="14" y2="14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
                <input
                  className="recipes-search"
                  placeholder="Search provisions..."
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                />
              </div>
              <button className="recipes-import-btn" onClick={handleOpenImport}>
                <svg width="15" height="15" viewBox="0 0 15 15">
                  <path d="M4,2 L2,2 Q1,2 1,3 L1,12 Q1,13 2,13 L10,13 Q11,13 11,12 L11,9" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                  <path d="M8,1 L14,1 L14,7" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
                  <line x1="7" y1="8" x2="14" y2="1" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                </svg>
                Import URL
              </button>
              <button className="recipes-create-btn" onClick={handleOpenCreate}>
                <svg width="16" height="16" viewBox="0 0 16 16">
                  <line x1="8" y1="2" x2="8" y2="14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  <line x1="2" y1="8" x2="14" y2="8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
                New Recipe
              </button>
            </div>

            {loading ? (
              <div className="recipes-loading">
                <div className="recipes-loading-flame" />
                <p>Consulting the recipe chest...</p>
              </div>
            ) : (
              <>
                {error && <p className="recipes-error">{error}</p>}
                {detailLoading && <p className="recipes-loading-inline">Loading recipe...</p>}

                {availableMealTabs.length > 1 && (
                  <div className="recipes-meal-tabs">
                    <button
                      className={`recipes-meal-tab ${mealTab === null ? 'recipes-meal-tab--active' : ''}`}
                      onClick={() => setMealTab(null)}
                    >
                      All
                    </button>
                    {availableMealTabs.map(m => (
                      <button
                        key={m}
                        className={`recipes-meal-tab ${mealTab === m ? 'recipes-meal-tab--active' : ''}`}
                        onClick={() => setMealTab(m)}
                      >
                        {m === 'uncategorized' ? 'Other' : m}
                      </button>
                    ))}
                  </div>
                )}

                {filteredRecipes.length === 0 ? (
                  <div className="recipes-empty">
                    <svg width="72" height="72" viewBox="0 0 72 72" className="recipes-empty-icon">
                      <rect x="12" y="16" width="48" height="40" rx="4" fill="none" stroke="var(--tan-deep)" strokeWidth="2" />
                      <line x1="22" y1="28" x2="50" y2="28" stroke="var(--tan-deep)" strokeWidth="1.5" strokeLinecap="round" opacity="0.6" />
                      <line x1="22" y1="35" x2="50" y2="35" stroke="var(--tan-deep)" strokeWidth="1.5" strokeLinecap="round" opacity="0.4" />
                      <line x1="22" y1="42" x2="38" y2="42" stroke="var(--tan-deep)" strokeWidth="1.5" strokeLinecap="round" opacity="0.3" />
                    </svg>
                    <p className="recipes-empty-text">
                      {search || mealTab ? 'No provisions match your filter.' : 'No recipes yet. Add the first one!'}
                    </p>
                  </div>
                ) : (
                  <div className="recipes-grid">
                    {[...filteredRecipes].sort((a, b) => (a.theme ?? '').localeCompare(b.theme ?? '')).map((recipe, i) => (
                      <button
                        key={recipe.id}
                        className={`recipe-card ${recipe.status === 'draft' ? 'recipe-card--draft' : ''}`}
                        style={{ animationDelay: `${i * 0.06}s` }}
                        onClick={() => handleViewRecipe(recipe)}
                      >
                        <div className="recipe-card__header">
                          <h3 className="recipe-card__name">{recipe.name}</h3>
                          {recipe.status === 'draft' && (
                            <span className="recipe-card__draft-badge">Draft</span>
                          )}
                        </div>
                        {recipe.description && (
                          <p className="recipe-card__desc">{recipe.description}</p>
                        )}
                        <div className="recipe-card__meta">
                          <span className="recipe-card__servings">
                            <svg width="13" height="13" viewBox="0 0 13 13">
                              <circle cx="6.5" cy="4" r="2.5" fill="none" stroke="currentColor" strokeWidth="1.2" />
                              <path d="M1.5,11.5 Q1.5,8 6.5,8 Q11.5,8 11.5,11.5" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                            </svg>
                            Serves&nbsp;{recipe.baseServings}
                          </span>
                          {recipe.webLink && (
                            <span className="recipe-card__imported">
                              <svg width="12" height="12" viewBox="0 0 12 12">
                                <path d="M5,2 L2,2 Q1,2 1,3 L1,10 Q1,11 2,11 L9,11 Q10,11 10,10 L10,7" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                                <path d="M7,1 L11,1 L11,5" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
                                <line x1="6" y1="6" x2="11" y2="1" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                              </svg>
                              Imported
                            </span>
                          )}
                        </div>
                        {(recipe.meal || recipe.theme) && (
                          <div className="recipe-card__tags">
                            {recipe.meal && <span className="recipe-card__tag">{recipe.meal}</span>}
                            {recipe.theme && <span className="recipe-card__tag">{recipe.theme}</span>}
                          </div>
                        )}
                        <div className="recipe-card__arrow">
                          <svg width="16" height="16" viewBox="0 0 16 16">
                            <path d="M5,3 L11,8 L5,13" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                          </svg>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* ── Detail View ── */}
        {view === 'detail' && selectedRecipe && (
          <div className="recipes-detail-view">
            <div className="recipes-detail-header">
              <button className="recipes-back-btn" onClick={handleBackToList}>
                <svg width="18" height="18" viewBox="0 0 18 18">
                  <path d="M11,4 L6,9 L11,14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                All Recipes
              </button>
              <div className="recipes-detail-actions">
                <button className="recipes-action-btn recipes-action-btn--edit" onClick={handleOpenEdit}>
                  <svg width="15" height="15" viewBox="0 0 15 15">
                    <path d="M10,2 L13,5 L5,13 L2,13 L2,10 Z" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round" />
                    <line x1="8" y1="4" x2="11" y2="7" stroke="currentColor" strokeWidth="1.3" />
                  </svg>
                  Edit
                </button>
                <button className="recipes-action-btn recipes-action-btn--delete" onClick={() => setDeletingRecipe(selectedRecipe)}>
                  <svg width="15" height="15" viewBox="0 0 15 15">
                    <path d="M3,4 L12,4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                    <path d="M6,4 L6,2.5 Q6,2 6.5,2 L8.5,2 Q9,2 9,2.5 L9,4" stroke="currentColor" strokeWidth="1.2" fill="none" />
                    <path d="M4,4 L4.8,12 Q4.8,13 5.8,13 L9.2,13 Q10.2,13 10.2,12 L11,4" stroke="currentColor" strokeWidth="1.2" fill="none" />
                  </svg>
                  Delete
                </button>
              </div>
            </div>

            <div className="recipe-detail">
              {/* Recipe metadata */}
              <div className="recipe-detail__hero">
                <div className="recipe-detail__title-row">
                  <h1 className="recipe-detail__name">{selectedRecipe.name}</h1>
                  {selectedRecipe.status === 'draft' && (
                    <span className="recipe-detail__draft-badge">Draft</span>
                  )}
                </div>
                {selectedRecipe.description && (
                  <p className="recipe-detail__description">{selectedRecipe.description}</p>
                )}
                <div className="recipe-detail__attrs">
                  <div className="recipe-detail__attr">
                    <svg width="16" height="16" viewBox="0 0 16 16">
                      <circle cx="8" cy="5.5" r="3" fill="none" stroke="var(--sage-deep)" strokeWidth="1.5" />
                      <path d="M2,14 Q2,10 8,10 Q14,10 14,14" fill="none" stroke="var(--sage-deep)" strokeWidth="1.5" strokeLinecap="round" />
                    </svg>
                    <span>Serves {selectedRecipe.baseServings}</span>
                  </div>
                  {selectedRecipe.webLink && (
                    <a
                      className="recipe-detail__attr recipe-detail__attr--link"
                      href={selectedRecipe.webLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={e => e.stopPropagation()}
                    >
                      <svg width="14" height="14" viewBox="0 0 14 14">
                        <path d="M6,2 L2,2 Q1,2 1,3 L1,11 Q1,12 2,12 L10,12 Q11,12 11,11 L11,8" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                        <path d="M8,1 L13,1 L13,6" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
                        <line x1="7" y1="7" x2="13" y2="1" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                      </svg>
                      View source
                    </a>
                  )}
                  {selectedRecipe.meal && (
                    <div className="recipe-detail__attr">
                      <span className="recipe-detail__tag">{selectedRecipe.meal}</span>
                    </div>
                  )}
                  {selectedRecipe.theme && (
                    <div className="recipe-detail__attr">
                      <span className="recipe-detail__tag">{selectedRecipe.theme}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Duplicate warning */}
              {selectedRecipe.duplicateOf && (
                <div className="recipe-detail__duplicate-banner">
                  <div className="recipe-detail__duplicate-banner-icon">
                    <svg width="20" height="20" viewBox="0 0 20 20">
                      <path d="M10,3 L18,17 L2,17 Z" fill="none" stroke="var(--ember)" strokeWidth="1.5" strokeLinejoin="round" />
                      <line x1="10" y1="9" x2="10" y2="13" stroke="var(--ember)" strokeWidth="1.5" strokeLinecap="round" />
                      <circle cx="10" cy="15.5" r="0.8" fill="var(--ember)" />
                    </svg>
                  </div>
                  <div className="recipe-detail__duplicate-banner-body">
                    <p className="recipe-detail__duplicate-banner-title">Possible duplicate detected</p>
                    <p className="recipe-detail__duplicate-banner-text">
                      This recipe may be a duplicate of <strong>"{selectedRecipe.duplicateOf.name}"</strong>.
                      Resolve this before publishing.
                    </p>
                    <div className="recipe-detail__duplicate-banner-actions">
                      <button
                        className="review-btn review-btn--secondary"
                        disabled={resolving}
                        onClick={() => handleResolveDuplicate('NOT_DUPLICATE')}
                      >
                        Not a duplicate
                      </button>
                      <button
                        className="review-btn review-btn--danger"
                        disabled={resolving}
                        onClick={() => handleResolveDuplicate('USE_EXISTING')}
                      >
                        Use existing recipe
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* Ingredients section */}
              <div className="recipe-detail__ingredients">
                <h2 className="recipe-detail__section-title">
                  <svg width="18" height="18" viewBox="0 0 18 18">
                    <path d="M9,2 C9,2 4,5 4,9 Q4,13 9,14 Q14,13 14,9 C14,5 9,2 9,2Z" fill="none" stroke="var(--sage-deep)" strokeWidth="1.5" />
                    <line x1="9" y1="14" x2="9" y2="16" stroke="var(--sage-deep)" strokeWidth="1.5" strokeLinecap="round" />
                    <line x1="7" y1="16" x2="11" y2="16" stroke="var(--sage-deep)" strokeWidth="1.5" strokeLinecap="round" />
                  </svg>
                  Ingredients
                  {pendingCount > 0 && (
                    <span className="recipe-detail__pending-count">{pendingCount} need review</span>
                  )}
                </h2>

                {selectedRecipe.ingredients.length === 0 ? (
                  <p className="recipe-detail__no-ingredients">No ingredients listed.</p>
                ) : (
                  <>
                  <ul className="recipe-detail__ingredient-list">
                    {selectedRecipe.ingredients.map(ri => {
                      const globalName = ri.ingredient?.name;
                      const isPending = ri.status === 'pending_review';

                      return (
                        <li key={ri.id} className={`recipe-detail__ingredient ${isPending ? 'recipe-detail__ingredient--pending' : ''}`}>
                          {/* Approved ingredient row */}
                          {!isPending && (
                            <div className="recipe-detail__ingredient-row">
                              <span className="recipe-detail__ingredient-status-icon">
                                <svg width="14" height="14" viewBox="0 0 14 14">
                                  <circle cx="7" cy="7" r="6" fill="none" stroke="var(--sage-deep)" strokeWidth="1.3" />
                                  <path d="M4,7 L6,9 L10,5" fill="none" stroke="var(--sage-deep)" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
                                </svg>
                              </span>
                              <span className="recipe-detail__ingredient-qty">
                                {ri.quantity % 1 === 0 ? ri.quantity : ri.quantity.toFixed(2)} {ri.unit}
                              </span>
                              <span className="recipe-detail__ingredient-name">{globalName ?? '—'}</span>
                              <span className="recipe-detail__ingredient-category">{ri.ingredient?.category ?? ''}</span>
                              <span className="recipe-detail__ingredient-source">
                                {globalName && ri.originalText && ri.originalText !== globalName ? `"${ri.originalText}"` : ''}
                              </span>
                              <span className="recipe-detail__ingredient-action">
                                <button className="review-edit-btn" onClick={() => openResolveModal(ri)}>Edit</button>
                              </span>
                            </div>
                          )}

                          {/* Pending ingredient — inline editable */}
                          {isPending && (() => {
                            const edit = getPendingEdit(ri);
                            const matchStillValid = isStillExistingMatch(ri, edit);
                            const isCreateMode = !matchStillValid;
                            const hasSuggestion = !!(ri.matchedIngredient || ri.suggestedIngredientName);

                            return (
                              <div className="recipe-detail__pending-card">
                                {/* Source text */}
                                {ri.originalText && (
                                  <div className="recipe-detail__pending-source-line">
                                    <svg width="14" height="14" viewBox="0 0 14 14" className="recipe-detail__pending-icon">
                                      <circle cx="7" cy="7" r="6" fill="none" stroke="var(--ember)" strokeWidth="1.3" />
                                      <line x1="7" y1="4.5" x2="7" y2="8" stroke="var(--ember)" strokeWidth="1.3" strokeLinecap="round" />
                                      <circle cx="7" cy="9.5" r="0.7" fill="var(--ember)" />
                                    </svg>
                                    <span className="recipe-detail__pending-source">&ldquo;{ri.originalText}&rdquo;</span>
                                  </div>
                                )}

                                {/* Editable fields row */}
                                <div className="recipe-detail__pending-fields">
                                  <input
                                    type="number"
                                    className="pending-field pending-field--qty"
                                    value={edit.qty}
                                    min={0}
                                    step="any"
                                    onChange={e => initPendingEdit(ri, { qty: parseFloat(e.target.value) || 0 })}
                                  />
                                  <div className="pending-field-group">
                                    <div className={`pending-field-group__fields ${isCreateMode && edit.name.trim() ? 'pending-field-group__fields--bordered' : ''}`}>
                                      <select
                                        className="pending-field pending-field--unit"
                                        value={edit.unit}
                                        onChange={e => initPendingEdit(ri, { unit: e.target.value })}
                                      >
                                        {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                                      </select>
                                      <input
                                        type="text"
                                        className="pending-field pending-field--name"
                                        value={edit.name}
                                        placeholder="Ingredient name"
                                        onChange={e => initPendingEdit(ri, { name: e.target.value })}
                                      />
                                      {isCreateMode && edit.name.trim() && (
                                        <select
                                          className="pending-field pending-field--category"
                                          value={edit.category}
                                          onChange={e => initPendingEdit(ri, { category: e.target.value })}
                                        >
                                          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                                        </select>
                                      )}
                                    </div>
                                    {isCreateMode && edit.name.trim() && (
                                      <span className="pending-status__label pending-status__label--create">create new ingredient</span>
                                    )}
                                  </div>
                                </div>

                                {/* Status line — existing match info */}
                                <div className="recipe-detail__pending-status">
                                  {matchStillValid ? (
                                    <>
                                      <span className="pending-status__label pending-status__label--match">using existing</span>
                                      <span className="pending-status__value">{ri.matchedIngredient!.name}</span>
                                      <span className="recipe-detail__ingredient-category">{ri.matchedIngredient!.category}</span>
                                    </>
                                  ) : !edit.name.trim() ? (
                                    <span className="pending-status__label">enter a name to resolve</span>
                                  ) : null}
                                </div>

                                {/* Actions */}
                                <div className="recipe-detail__pending-actions">
                                  {(hasSuggestion || edit.name.trim()) && (
                                    <button
                                      className="review-accept-btn"
                                      disabled={acceptingId === ri.id || !edit.name.trim()}
                                      onClick={() => handleAcceptPending(ri)}
                                    >
                                      {acceptingId === ri.id ? 'Saving…' : matchStillValid ? '✓ Accept' : '+ Create & Accept'}
                                    </button>
                                  )}
                                </div>
                              </div>
                            );
                          })()}
                        </li>
                      );
                    })}
                  </ul>

                  {pendingCount > 1 && selectedRecipe.ingredients.some(ri => ri.status === 'pending_review' && (ri.matchedIngredient || ri.suggestedIngredientName)) && (
                    <button
                      className="recipe-detail__accept-all-btn"
                      onClick={handleAcceptAllSuggestions}
                      disabled={!!acceptingId}
                    >
                      ✓ Accept All Suggestions
                    </button>
                  )}
                  </>
                )}

                <div className="recipe-detail__add-ingredient">
                    {!addIngredientOpen ? (
                      <button
                        className="recipe-detail__add-btn"
                        onClick={() => { setAddIngredientOpen(true); setAddIngredientSearch(''); setAddCreateMode(false); setAddError(''); }}
                      >
                        + Add Ingredient
                      </button>
                    ) : (
                      <div className="recipe-detail__add-picker">
                        <div className="recipe-detail__add-picker-header">
                          <input
                            type="text"
                            className="recipes-input"
                            placeholder="Search ingredients..."
                            value={addIngredientSearch}
                            onChange={e => setAddIngredientSearch(e.target.value)}
                            autoFocus
                          />
                          <button className="recipe-detail__add-close" onClick={() => setAddIngredientOpen(false)}>✕</button>
                        </div>
                        {addError && <p className="recipes-form-error" style={{ marginBottom: 'var(--space-sm)' }}>{addError}</p>}

                        {!addCreateMode ? (
                          <>
                            <div className="recipe-detail__add-categories">
                              {Object.entries(
                                ingredients
                                  .filter(ing => {
                                    const q = addIngredientSearch.toLowerCase();
                                    return !q || ing.name.toLowerCase().includes(q) || ing.category.toLowerCase().includes(q);
                                  })
                                  .reduce<Record<string, IngredientResponse[]>>((acc, ing) => {
                                    (acc[ing.category] = acc[ing.category] || []).push(ing);
                                    return acc;
                                  }, {})
                              )
                                .sort(([a], [b]) => a.localeCompare(b))
                                .map(([cat, ings]) => (
                                  <div key={cat} className="recipe-detail__add-category-group">
                                    <span className="recipe-detail__add-category-label">{cat}</span>
                                    <div className="recipe-detail__add-pills">
                                      {ings.sort((a, b) => a.name.localeCompare(b.name)).map(ing => (
                                        <button
                                          key={ing.id}
                                          className="ingredient-pill"
                                          onClick={() => handleAddIngredientToRecipe(ing)}
                                        >
                                          {ing.name}
                                        </button>
                                      ))}
                                    </div>
                                  </div>
                                ))}
                            </div>
                            <button
                              className="recipe-detail__add-create-btn"
                              onClick={() => { setAddCreateMode(true); setAddNewName(addIngredientSearch); }}
                            >
                              + Create new ingredient
                            </button>
                          </>
                        ) : (
                          <div className="recipe-detail__add-create-form">
                            <div className="recipe-detail__add-create-row">
                              <input
                                type="text"
                                className="recipes-input"
                                placeholder="Ingredient name"
                                value={addNewName}
                                onChange={e => setAddNewName(e.target.value)}
                                autoFocus
                              />
                            </div>
                            <div className="recipe-detail__add-create-row">
                              <select className="recipes-select" value={addNewCategory} onChange={e => setAddNewCategory(e.target.value)}>
                                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                              </select>
                              <input
                                type="number"
                                className="recipes-input recipes-input--qty"
                                placeholder="Qty"
                                value={addNewQty}
                                onChange={e => setAddNewQty(Number(e.target.value) || 0)}
                                min={0}
                                step="any"
                              />
                              <select className="recipes-select" value={addNewUnit} onChange={e => setAddNewUnit(e.target.value)}>
                                {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                              </select>
                            </div>
                            <div className="recipe-detail__add-create-actions">
                              <button className="recipe-detail__add-create-cancel" onClick={() => setAddCreateMode(false)}>Cancel</button>
                              <button
                                className="recipe-detail__add-create-save"
                                onClick={handleCreateAndAddIngredient}
                                disabled={addCreating || !addNewName.trim()}
                              >
                                {addCreating ? 'Creating...' : 'Create & Add'}
                              </button>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                </div>

                {resolveError && !resolveModalIngredient && (
                  <p className="recipes-form-error" style={{ marginTop: 'var(--space-sm)' }}>{resolveError}</p>
                )}

                {/* ── Ingredient Resolve Modal ── */}
                {resolveModalIngredient && (
                  <div className="modal-overlay" onClick={closeResolveModal}>
                    <div className="resolve-modal" onClick={e => e.stopPropagation()}>
                      <h2 className="resolve-modal__title">Link Ingredient</h2>

                      {/* Source text */}
                      {resolveModalIngredient.originalText && (
                        <div className="resolve-modal__source">
                          <span className="resolve-modal__source-label">From the recipe:</span>
                          <span className="resolve-modal__source-text">"{resolveModalIngredient.originalText}"</span>
                        </div>
                      )}

                      {/* Suggested matches */}
                      {!resolveCreateMode && (
                        <div className="resolve-modal__section">
                          <h3 className="resolve-modal__section-title">We think this might be one of these ingredients</h3>
                          {resolveModalSuggestions.length > 0 ? (
                            <div className="resolve-modal__suggestions">
                              {resolveModalSuggestions.map(ing => (
                                <button
                                  key={ing.id}
                                  className={`resolve-modal__suggestion ${resolveSelectedId === ing.id ? 'resolve-modal__suggestion--selected' : ''}`}
                                  onClick={() => { setResolveSelectedId(ing.id); setResolveCreateMode(false); }}
                                >
                                  <span className="resolve-modal__suggestion-name">{ing.name}</span>
                                  <span className="resolve-modal__suggestion-meta">{ing.category} &middot; {ing.defaultUnit}</span>
                                </button>
                              ))}
                            </div>
                          ) : (
                            <p className="resolve-modal__no-suggestions">No suggested ingredients — search below or create a new one.</p>
                          )}
                        </div>
                      )}

                      {/* Search existing */}
                      {!resolveCreateMode && (
                        <div className="resolve-modal__section">
                          <h3 className="resolve-modal__section-title">Search for other ingredients</h3>
                          <input
                            className="resolve-modal__search"
                            placeholder="Type to search..."
                            value={resolveSearchQuery}
                            onChange={e => setResolveSearchQuery(e.target.value)}
                          />
                          {resolveSearchResults.length > 0 && (
                            <div className="resolve-modal__search-results">
                              {resolveSearchResults.slice(0, 8).map(ing => (
                                <button
                                  key={ing.id}
                                  className={`resolve-modal__suggestion ${resolveSelectedId === ing.id ? 'resolve-modal__suggestion--selected' : ''}`}
                                  onClick={() => { setResolveSelectedId(ing.id); setResolveCreateMode(false); }}
                                >
                                  <span className="resolve-modal__suggestion-name">{ing.name}</span>
                                  <span className="resolve-modal__suggestion-meta">{ing.category} &middot; {ing.defaultUnit}</span>
                                </button>
                              ))}
                            </div>
                          )}
                        </div>
                      )}

                      {/* Create new ingredient */}
                      <div className="resolve-modal__section">
                        {!resolveCreateMode ? (
                          <button
                            className="resolve-modal__create-toggle"
                            onClick={() => { setResolveCreateMode(true); setResolveSelectedId(null); }}
                          >
                            + Create a new ingredient
                          </button>
                        ) : (
                          <div className="resolve-modal__create-form">
                            <h3 className="resolve-modal__section-title">Create new ingredient</h3>
                            <div className="resolve-modal__create-fields">
                              <div className="recipes-field">
                                <label className="recipes-label">Name</label>
                                <input
                                  className="recipes-input"
                                  value={newIngName}
                                  onChange={e => setNewIngName(e.target.value)}
                                  autoFocus
                                />
                              </div>
                              <div className="resolve-modal__create-row">
                                <div className="recipes-field">
                                  <label className="recipes-label">Category</label>
                                  <select className="recipes-input recipes-select" value={newIngCategory} onChange={e => setNewIngCategory(e.target.value)}>
                                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                                  </select>
                                </div>
                                <div className="recipes-field">
                                  <label className="recipes-label">Default unit</label>
                                  <select className="recipes-input recipes-select" value={newIngUnit} onChange={e => setNewIngUnit(e.target.value)}>
                                    {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                                  </select>
                                </div>
                              </div>
                            </div>
                            <button
                              className="resolve-modal__create-back"
                              onClick={() => setResolveCreateMode(false)}
                            >
                              ← Back to search
                            </button>
                          </div>
                        )}
                      </div>

                      {/* Quantity & Unit */}
                      <div className="resolve-modal__qty-row">
                        <label className="resolve-modal__qty-label">Quantity & Unit</label>
                        <div className="resolve-modal__qty-inputs">
                          <input
                            type="number"
                            className="resolve-modal__qty-input"
                            value={resolveQty}
                            onChange={e => setResolveQty(Number(e.target.value) || 0)}
                            min={0}
                            step="any"
                          />
                          <select
                            className="resolve-modal__unit-select"
                            value={resolveUnit}
                            onChange={e => setResolveUnit(e.target.value)}
                          >
                            {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                          </select>
                        </div>
                      </div>

                      {/* Preview summary */}
                      <div className="resolve-modal__preview">
                        {resolveCreateMode && newIngName.trim() ? (
                          <p>Will create <strong>{newIngName.trim()}</strong> ({newIngCategory}, {newIngUnit}) and link this ingredient to it.</p>
                        ) : resolvePreviewIngredient ? (
                          <p>Updating ingredient to <strong>{resolveQty % 1 === 0 ? resolveQty : resolveQty.toFixed(2)} {resolveUnit} {resolvePreviewIngredient.name}</strong></p>
                        ) : (
                          <p className="resolve-modal__preview--empty">Select an ingredient or create a new one</p>
                        )}
                      </div>

                      {resolveError && <p className="resolve-modal__error">{resolveError}</p>}

                      {/* Actions */}
                      <div className="resolve-modal__actions">
                        <button
                          className="modal-btn modal-btn--danger resolve-modal__remove-btn"
                          onClick={handleRemoveIngredient}
                          disabled={resolving}
                        >
                          Remove from recipe
                        </button>
                        <div className="resolve-modal__actions-right">
                          <button className="modal-btn modal-btn--secondary" onClick={closeResolveModal} disabled={resolving}>
                            Cancel
                          </button>
                          <button
                            className="modal-btn"
                            disabled={resolving || (!resolveSelectedId && !(resolveCreateMode && newIngName.trim()))}
                            onClick={handleResolveSave}
                          >
                            {resolving ? 'Saving...' : 'Save'}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Publish section */}
              {selectedRecipe.status === 'draft' && (
                <div className="recipe-detail__publish-section">
                  {canPublish ? (
                    <>
                      <p className="recipe-detail__publish-ready">
                        All ingredients resolved — ready to publish!
                      </p>
                      <button
                        className="recipe-detail__publish-btn"
                        disabled={publishing}
                        onClick={handlePublish}
                      >
                        {publishing ? 'Publishing...' : 'Publish to Cookbook'}
                      </button>
                    </>
                  ) : (
                    <p className="recipe-detail__publish-blocked">
                      {pendingCount > 0
                        ? `${pendingCount} ingredient${pendingCount !== 1 ? 's' : ''} still need${pendingCount === 1 ? 's' : ''} review before publishing.`
                        : selectedRecipe.duplicateOf
                          ? 'Resolve the duplicate warning before publishing.'
                          : ''}
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        )}

        {/* ── Import View ── */}
        {view === 'import' && (
          <div className="recipes-create-view">
            <div className="recipes-detail-header">
              <button className="recipes-back-btn" onClick={handleBackToList}>
                <svg width="18" height="18" viewBox="0 0 18 18">
                  <path d="M11,4 L6,9 L11,14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                Cancel
              </button>
            </div>

            <div className="recipes-form-container">
              <h1 className="recipes-form-title">Import from URL</h1>
              <p className="recipes-form-subtitle">Paste a recipe link and let the camp wizard do the rest</p>

              <form className="recipes-form" onSubmit={handleImport}>
                <div className="recipes-form-section">
                  <div className="recipes-field">
                    <label className="recipes-label">Recipe URL</label>
                    <input
                      className="recipes-input"
                      type="url"
                      placeholder="https://example.com/guacamole-recipe"
                      value={importUrl}
                      onChange={e => setImportUrl(e.target.value)}
                      autoFocus
                    />
                    <p className="recipes-field-hint">
                      The recipe will be scraped and parsed automatically. You'll review and approve each ingredient before publishing.
                    </p>
                  </div>

                  {importing && (
                    <div className="recipes-import-progress">
                      <div className="recipes-loading-flame" />
                      <p>The camp wizard is reading the scroll...</p>
                    </div>
                  )}
                </div>

                {importError && <p className="recipes-form-error">{importError}</p>}

                <div className="recipes-form-actions">
                  <button type="button" className="recipes-cancel-btn" onClick={handleBackToList}>
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="recipes-submit-btn"
                    disabled={importing || !importUrl.trim()}
                  >
                    {importing ? 'Importing...' : 'Import Recipe'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* ── Create View ── */}
        {view === 'create' && (
          <div className="recipes-create-view">
            <div className="recipes-detail-header">
              <button className="recipes-back-btn" onClick={handleBackToList}>
                <svg width="18" height="18" viewBox="0 0 18 18">
                  <path d="M11,4 L6,9 L11,14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                Cancel
              </button>
            </div>

            <div className="recipes-form-container">
              <h1 className="recipes-form-title">New Provision</h1>
              <p className="recipes-form-subtitle">Add a recipe to the camp cookbook</p>

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
                        {MEALS.map(m => <option key={m} value={m}>{m}</option>)}
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
                        {THEMES.map(t => <option key={t} value={t}>{t}</option>)}
                      </select>
                    </div>
                  </div>
                </div>

                <div className="recipes-form-section">
                  <h3 className="recipes-form-section-title">Ingredients</h3>

                  {/* Added ingredients with editable qty/unit */}
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
                            onChange={e => setDraftIngredients(prev => prev.map(x =>
                              x.ingredientId === d.ingredientId ? { ...x, quantity: Number(e.target.value) } : x
                            ))}
                          />
                          <select
                            className="recipes-select--inline"
                            value={d.unit}
                            onChange={e => setDraftIngredients(prev => prev.map(x =>
                              x.ingredientId === d.ingredientId ? { ...x, unit: e.target.value } : x
                            ))}
                          >
                            {UNITS.map(u => (
                              <option key={u} value={u}>{u}</option>
                            ))}
                          </select>
                          <span className="recipes-draft-ingredient__name">{d.ingredientName}</span>
                          <button
                            type="button"
                            className="recipes-draft-ingredient__remove"
                            onClick={() => handleRemoveDraftIngredient(d.ingredientId)}
                          >×</button>
                        </li>
                      ))}
                    </ul>
                  )}

                  {/* Ingredient pill picker */}
                  <div className="ingredient-picker">
                    <div className="ingredient-picker__search-wrap">
                      <svg width="14" height="14" viewBox="0 0 14 14" className="ingredient-picker__search-icon">
                        <circle cx="5.5" cy="5.5" r="4" fill="none" stroke="currentColor" strokeWidth="1.3" />
                        <line x1="8.5" y1="8.5" x2="12" y2="12" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                      </svg>
                      <input
                        className="ingredient-picker__search"
                        placeholder="Filter ingredients..."
                        value={categorySearch}
                        onChange={e => setCategorySearch(e.target.value)}
                      />
                      {categorySearch && (
                        <button type="button" className="ingredient-picker__clear" onClick={() => setCategorySearch('')}>×</button>
                      )}
                    </div>

                    <div className="ingredient-picker__categories">
                      {ingredientsByCategory.length === 0 ? (
                        <p className="ingredient-picker__empty">
                          {categorySearch ? 'No ingredients match your search.' : 'All ingredients have been added!'}
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
                        onClick={() => { setPickerCreateMode(true); setPickerNewName(categorySearch); }}
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
                          <select className="recipes-select" value={pickerNewCategory} onChange={e => setPickerNewCategory(e.target.value)}>
                            {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
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
                          <select className="recipes-select" value={pickerNewUnit} onChange={e => setPickerNewUnit(e.target.value)}>
                            {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                          </select>
                        </div>
                        <div className="recipe-detail__add-create-actions">
                          <button type="button" className="recipe-detail__add-create-cancel" onClick={() => setPickerCreateMode(false)}>Cancel</button>
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
                  <button type="button" className="recipes-cancel-btn" onClick={handleBackToList}>
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="recipes-submit-btn"
                    disabled={creating || !createName.trim()}
                  >
                    {creating ? 'Creating...' : 'Add to Cookbook'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* ── Edit View ── */}
        {view === 'edit' && selectedRecipe && (
          <div className="recipes-create-view">
            <div className="recipes-detail-header">
              <button className="recipes-back-btn" onClick={() => setView('detail')}>
                <svg width="18" height="18" viewBox="0 0 18 18">
                  <path d="M11,4 L6,9 L11,14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                Cancel
              </button>
            </div>

            <div className="recipes-form-container">
              <h1 className="recipes-form-title">Edit Provision</h1>

              <form className="recipes-form" onSubmit={handleSaveEdit}>
                <div className="recipes-form-section">
                  <div className="recipes-field">
                    <label className="recipes-label">Recipe name *</label>
                    <input
                      className="recipes-input"
                      value={editName}
                      onChange={e => setEditName(e.target.value)}
                      autoFocus
                    />
                  </div>
                  <div className="recipes-field">
                    <label className="recipes-label">Description</label>
                    <textarea
                      className="recipes-input recipes-textarea"
                      value={editDesc}
                      onChange={e => setEditDesc(e.target.value)}
                      rows={3}
                    />
                  </div>
                  <div className="recipes-field recipes-field--half">
                    <label className="recipes-label">Servings *</label>
                    <input
                      className="recipes-input"
                      type="number"
                      min={1}
                      value={editServings}
                      onChange={e => setEditServings(Number(e.target.value))}
                    />
                  </div>
                  <div className="recipes-row">
                    <div className="recipes-field recipes-field--half">
                      <label className="recipes-label">Meal</label>
                      <select
                        className="recipes-select"
                        value={editMeal}
                        onChange={e => setEditMeal(e.target.value)}
                      >
                        <option value="">—</option>
                        {MEALS.map(m => <option key={m} value={m}>{m}</option>)}
                      </select>
                    </div>
                    <div className="recipes-field recipes-field--half">
                      <label className="recipes-label">Theme</label>
                      <select
                        className="recipes-select"
                        value={editTheme}
                        onChange={e => setEditTheme(e.target.value)}
                      >
                        <option value="">—</option>
                        {THEMES.map(t => <option key={t} value={t}>{t}</option>)}
                      </select>
                    </div>
                  </div>
                </div>

                {editError && <p className="recipes-form-error">{editError}</p>}

                <div className="recipes-form-actions">
                  <button type="button" className="recipes-cancel-btn" onClick={() => setView('detail')}>
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="recipes-submit-btn"
                    disabled={saving || !editName.trim()}
                  >
                    {saving ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>

      {/* Delete confirmation modal */}
      {deletingRecipe && (
        <div className="modal-overlay" onClick={() => setDeletingRecipe(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-icon-large">
              <svg width="48" height="48" viewBox="0 0 48 48">
                <path d="M10,16 L38,16" stroke="var(--rose-deep)" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M18,16 L18,10 Q18,8 20,8 L28,8 Q30,8 30,10 L30,16" stroke="var(--rose-deep)" strokeWidth="2" fill="none" />
                <path d="M13,16 L15,40 Q15,42 17,42 L31,42 Q33,42 33,40 L35,16" stroke="var(--rose-deep)" strokeWidth="2" fill="none" />
              </svg>
            </div>
            <h2 className="modal-title">Remove from Cookbook?</h2>
            <p className="modal-flavor">"{deletingRecipe.name}" will be lost from the recipe chest.</p>
            <div className="modal-actions">
              <button className="modal-btn modal-btn--secondary" onClick={() => setDeletingRecipe(null)}>
                Keep It
              </button>
              <button className="modal-btn modal-btn--danger" onClick={handleDeleteConfirm}>
                Remove
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
