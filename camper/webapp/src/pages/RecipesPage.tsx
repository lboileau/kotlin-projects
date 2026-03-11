import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  api,
  type RecipeResponse,
  type RecipeDetailResponse,
  type IngredientResponse,
  type CreateRecipeIngredientRequest,
} from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import './RecipesPage.css';
import '../components/Modal.css';

const UNITS = ['g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'oz', 'lb', 'pieces', 'whole', 'bunch', 'can', 'clove', 'pinch', 'slice', 'sprig'] as const;

type View = 'list' | 'detail' | 'create' | 'edit';

interface DraftIngredient {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}

export function RecipesPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [view, setView] = useState<View>('list');
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeDetailResponse | null>(null);
  const [ingredients, setIngredients] = useState<IngredientResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  // Create form state
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createLink, setCreateLink] = useState('');
  const [createServings, setCreateServings] = useState(4);
  const [draftIngredients, setDraftIngredients] = useState<DraftIngredient[]>([]);
  const [ingredientSearch, setIngredientSearch] = useState('');
  const [ingredientSearchOpen, setIngredientSearchOpen] = useState(false);
  const [pendingIngredient, setPendingIngredient] = useState<IngredientResponse | null>(null);
  const [pendingQty, setPendingQty] = useState<number>(1);
  const [pendingUnit, setPendingUnit] = useState<string>('pieces');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  // Edit form state
  const [editName, setEditName] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editServings, setEditServings] = useState(4);
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState('');

  // Delete confirmation
  const [deletingRecipe, setDeletingRecipe] = useState<RecipeDetailResponse | null>(null);

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

  const handleAddDraftIngredient = () => {
    if (!pendingIngredient) return;
    if (draftIngredients.some(d => d.ingredientId === pendingIngredient.id)) {
      setCreateError('That ingredient is already added');
      return;
    }
    setDraftIngredients(prev => [...prev, {
      ingredientId: pendingIngredient.id,
      ingredientName: pendingIngredient.name,
      quantity: pendingQty,
      unit: pendingUnit,
    }]);
    setPendingIngredient(null);
    setIngredientSearch('');
    setPendingQty(1);
    setPendingUnit('pieces');
    setCreateError('');
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

  const handleOpenEdit = () => {
    if (!selectedRecipe) return;
    setEditName(selectedRecipe.name);
    setEditDesc(selectedRecipe.description || '');
    setEditServings(selectedRecipe.baseServings);
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

  const filteredIngredients = ingredients.filter(i =>
    i.name.toLowerCase().includes(ingredientSearch.toLowerCase())
  );

  const filteredRecipes = recipes.filter(r =>
    r.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="recipes-page">
      <ParallaxBackground variant="dusk" />

      <div className="recipes-content">
        <header className="recipes-header">
          <div className="recipes-header-left">
            <button className="recipes-back-home" onClick={() => navigate('/')}>
              <svg width="20" height="20" viewBox="0 0 20 20">
                <path d="M13,4 L7,10 L13,16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <svg width="24" height="24" viewBox="0 0 24 24" className="recipes-brand-icon">
                <polygon points="12,2 4,20 20,20" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
                <path d="M9,15 Q11,10 12,8 Q13,10 15,15" fill="var(--ember)" opacity="0.7" />
              </svg>
              <span className="recipes-brand-text">Camper</span>
            </button>
          </div>
          <div className="recipes-header-title">
            <svg width="22" height="22" viewBox="0 0 22 22" className="recipes-header-icon">
              <rect x="3" y="4" width="16" height="14" rx="2" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
              <line x1="7" y1="8" x2="15" y2="8" stroke="var(--ember)" strokeWidth="1.2" strokeLinecap="round" opacity="0.8" />
              <line x1="7" y1="11" x2="15" y2="11" stroke="var(--ember)" strokeWidth="1.2" strokeLinecap="round" opacity="0.6" />
              <line x1="7" y1="14" x2="12" y2="14" stroke="var(--ember)" strokeWidth="1.2" strokeLinecap="round" opacity="0.4" />
            </svg>
            <span className="recipes-header-label">Camp Provisions</span>
          </div>
          <div className="recipes-header-right">
            <button className="recipes-user-btn" onClick={() => navigate('/account')}>
              <svg width="26" height="26" viewBox="0 0 26 26">
                <defs><clipPath id="avatar-clip-recipes"><circle cx="13" cy="13" r="12" /></clipPath></defs>
                <circle cx="13" cy="13" r="12" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="1.5" />
                <g clipPath="url(#avatar-clip-recipes)">
                  <circle cx="13" cy="10" r="4.5" fill="var(--parchment)" />
                  <ellipse cx="13" cy="22" rx="7" ry="5.5" fill="var(--parchment)" />
                </g>
              </svg>
              <span className="recipes-user-name">{user?.username || user?.email}</span>
            </button>
            <button className="recipes-logout" onClick={logout}>Log Out</button>
          </div>
        </header>

        {/* List View */}
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

                {filteredRecipes.length === 0 ? (
                  <div className="recipes-empty">
                    <svg width="72" height="72" viewBox="0 0 72 72" className="recipes-empty-icon">
                      <rect x="12" y="16" width="48" height="40" rx="4" fill="none" stroke="var(--tan-deep)" strokeWidth="2" />
                      <line x1="22" y1="28" x2="50" y2="28" stroke="var(--tan-deep)" strokeWidth="1.5" strokeLinecap="round" opacity="0.6" />
                      <line x1="22" y1="35" x2="50" y2="35" stroke="var(--tan-deep)" strokeWidth="1.5" strokeLinecap="round" opacity="0.4" />
                      <line x1="22" y1="42" x2="38" y2="42" stroke="var(--tan-deep)" strokeWidth="1.5" strokeLinecap="round" opacity="0.3" />
                    </svg>
                    <p className="recipes-empty-text">
                      {search ? 'No provisions match your search.' : 'No recipes yet. Add the first one!'}
                    </p>
                  </div>
                ) : (
                  <div className="recipes-grid">
                    {filteredRecipes.map((recipe, i) => (
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
                            Serves {recipe.baseServings}
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

        {/* Detail View */}
        {view === 'detail' && selectedRecipe && (
          <div className="recipes-detail-view">
            <div className="recipes-detail-header">
              <button className="recipes-back-btn" onClick={handleBackToList}>
                <svg width="18" height="18" viewBox="0 0 18 18">
                  <path d="M11,4 L6,9 L11,14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                All Recipes
              </button>
              {selectedRecipe.createdBy === user?.id && (
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
              )}
            </div>

            <div className="recipe-detail">
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
                </div>
              </div>

              <div className="recipe-detail__ingredients">
                <h2 className="recipe-detail__section-title">
                  <svg width="18" height="18" viewBox="0 0 18 18">
                    <path d="M9,2 C9,2 4,5 4,9 Q4,13 9,14 Q14,13 14,9 C14,5 9,2 9,2Z" fill="none" stroke="var(--sage-deep)" strokeWidth="1.5" />
                    <line x1="9" y1="14" x2="9" y2="16" stroke="var(--sage-deep)" strokeWidth="1.5" strokeLinecap="round" />
                    <line x1="7" y1="16" x2="11" y2="16" stroke="var(--sage-deep)" strokeWidth="1.5" strokeLinecap="round" />
                  </svg>
                  Ingredients
                </h2>
                {selectedRecipe.ingredients.length === 0 ? (
                  <p className="recipe-detail__no-ingredients">No ingredients listed.</p>
                ) : (
                  <ul className="recipe-detail__ingredient-list">
                    {selectedRecipe.ingredients.map(ri => (
                      <li key={ri.id} className={`recipe-detail__ingredient ${ri.status === 'pending_review' ? 'recipe-detail__ingredient--pending' : ''}`}>
                        <span className="recipe-detail__ingredient-qty">
                          {ri.quantity % 1 === 0 ? ri.quantity : ri.quantity.toFixed(2)} {ri.unit}
                        </span>
                        <span className="recipe-detail__ingredient-name">
                          {ri.ingredient?.name ?? ri.suggestedIngredientName ?? ri.originalText ?? '—'}
                        </span>
                        {ri.status === 'pending_review' && (
                          <span className="recipe-detail__ingredient-flag">needs review</span>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Create View */}
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
                </div>

                <div className="recipes-form-section">
                  <h3 className="recipes-form-section-title">Ingredients</h3>

                  {draftIngredients.length > 0 && (
                    <ul className="recipes-draft-ingredients">
                      {draftIngredients.map(d => (
                        <li key={d.ingredientId} className="recipes-draft-ingredient">
                          <span className="recipes-draft-ingredient__qty">{d.quantity % 1 === 0 ? d.quantity : d.quantity.toFixed(2)} {d.unit}</span>
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

                  <div className="recipes-add-ingredient">
                    <div className="recipes-ingredient-search-wrap">
                      <input
                        className="recipes-input"
                        placeholder="Search ingredients..."
                        value={ingredientSearch}
                        onFocus={() => setIngredientSearchOpen(true)}
                        onBlur={() => setTimeout(() => setIngredientSearchOpen(false), 150)}
                        onChange={e => {
                          setIngredientSearch(e.target.value);
                          setPendingIngredient(null);
                        }}
                      />
                      {ingredientSearchOpen && ingredientSearch.length > 0 && filteredIngredients.length > 0 && !pendingIngredient && (
                        <ul className="recipes-ingredient-dropdown">
                          {filteredIngredients.slice(0, 8).map(ing => (
                            <li
                              key={ing.id}
                              className="recipes-ingredient-option"
                              onMouseDown={() => {
                                setPendingIngredient(ing);
                                setIngredientSearch(ing.name);
                                setPendingUnit(ing.defaultUnit);
                                setIngredientSearchOpen(false);
                              }}
                            >
                              <span className="recipes-ingredient-option__name">{ing.name}</span>
                              <span className="recipes-ingredient-option__cat">{ing.category}</span>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>

                    {pendingIngredient && (
                      <div className="recipes-ingredient-qty-row">
                        <input
                          className="recipes-input recipes-input--qty"
                          type="number"
                          min={0.01}
                          step={0.01}
                          value={pendingQty}
                          onChange={e => setPendingQty(Number(e.target.value))}
                          placeholder="Qty"
                        />
                        <select
                          className="recipes-input recipes-select"
                          value={pendingUnit}
                          onChange={e => setPendingUnit(e.target.value)}
                        >
                          {UNITS.map(u => (
                            <option key={u} value={u}>{u}</option>
                          ))}
                        </select>
                        <button
                          type="button"
                          className="recipes-add-ingredient-btn"
                          onClick={handleAddDraftIngredient}
                        >
                          Add
                        </button>
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

        {/* Edit View */}
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
