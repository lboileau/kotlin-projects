import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  api,
  type IngredientResponse,
} from '../api/client';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AppHeader } from '../components/AppHeader';
import './IngredientsPage.css';
import './RecipesPage.css';
import '../components/Modal.css';
import { UNITS } from '../lib/constants';
const CATEGORIES = ['produce', 'dairy', 'meat', 'seafood', 'pantry', 'spice', 'condiment', 'frozen', 'bakery', 'other'] as const;

export function IngredientsPage() {
  const navigate = useNavigate();
  const [ingredients, setIngredients] = useState<IngredientResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [categoryTab, setCategoryTab] = useState<string | null>(null);

  // Create form
  interface CreateRow { name: string; category: string; unit: string; }
  const emptyRow = (): CreateRow => ({ name: '', category: 'produce', unit: 'pieces' });
  const [showCreate, setShowCreate] = useState(false);
  const [createRows, setCreateRows] = useState<CreateRow[]>([emptyRow()]);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  const updateCreateRow = (index: number, field: keyof CreateRow, value: string) => {
    setCreateRows(rows => rows.map((r, i) => i === index ? { ...r, [field]: value } : r));
  };

  const removeCreateRow = (index: number) => {
    setCreateRows(rows => rows.length === 1 ? rows : rows.filter((_, i) => i !== index));
  };

  // Edit state
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editCategory, setEditCategory] = useState<string>('produce');
  const [editUnit, setEditUnit] = useState<string>('pieces');
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState('');

  // Delete confirmation
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    loadIngredients();
  }, []);

  const loadIngredients = async () => {
    try {
      const data = await api.getIngredients();
      setIngredients(data);
    } catch {
      setError('Failed to load ingredients');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    const validRows = createRows.filter(r => r.name.trim());
    if (validRows.length === 0) return;
    setCreating(true);
    setCreateError('');
    try {
      for (const row of validRows) {
        await api.createIngredient({ name: row.name.trim(), category: row.category, defaultUnit: row.unit });
      }
      setCreateRows([emptyRow()]);
      setShowCreate(false);
      await loadIngredients();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create ingredient');
      await loadIngredients();
    } finally {
      setCreating(false);
    }
  };

  const startEdit = (ing: IngredientResponse) => {
    setEditingId(ing.id);
    setEditName(ing.name);
    setEditCategory(ing.category);
    setEditUnit(ing.defaultUnit);
    setEditError('');
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditError('');
  };

  const handleSaveEdit = async () => {
    if (!editingId || !editName.trim()) return;
    setSaving(true);
    setEditError('');
    try {
      await api.updateIngredient(editingId, { name: editName.trim(), category: editCategory, defaultUnit: editUnit });
      setEditingId(null);
      await loadIngredients();
    } catch (err) {
      setEditError(err instanceof Error ? err.message : 'Failed to update ingredient');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deletingId) return;
    setDeleting(true);
    try {
      await api.deleteIngredient(deletingId);
      setDeletingId(null);
      await loadIngredients();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete ingredient');
      setDeletingId(null);
    } finally {
      setDeleting(false);
    }
  };

  const filtered = ingredients.filter(ing => {
    const q = search.toLowerCase();
    const matchesSearch = !q || ing.name.toLowerCase().includes(q) || ing.category.toLowerCase().includes(q);
    const matchesCategory = categoryTab === null || ing.category === categoryTab;
    return matchesSearch && matchesCategory;
  });

  const availableCategories = [...new Set(ingredients.map(i => i.category))].sort();

  const grouped = filtered.reduce<Record<string, IngredientResponse[]>>((acc, ing) => {
    (acc[ing.category] = acc[ing.category] || []).push(ing);
    return acc;
  }, {});

  const deletingIngredient = ingredients.find(i => i.id === deletingId);

  return (
    <div className="ingredients-page">
      <ParallaxBackground variant="dusk" />

      <div className="ingredients-content">
        <AppHeader
          pageTitle="Ingredients"
          pageIcon={
            <svg width="22" height="22" viewBox="0 0 22 22">
              {/* Carrot body */}
              <path d="M11,20 Q9,14 8,10 Q9,6 11,4 Q13,6 14,10 Q13,14 11,20 Z" fill="var(--ember)" opacity="0.85" />
              <path d="M11,20 Q12,14 14,10 Q13,6 11,4" fill="var(--ember)" opacity="0.6" />
              {/* Lines on carrot */}
              <line x1="9.5" y1="10" x2="12.5" y2="10" stroke="var(--parchment)" strokeWidth="0.6" opacity="0.4" strokeLinecap="round" />
              <line x1="9.8" y1="13" x2="12.2" y2="13" stroke="var(--parchment)" strokeWidth="0.6" opacity="0.3" strokeLinecap="round" />
              {/* Leafy top */}
              <path d="M11,4 Q8,1 6,2" stroke="var(--sage)" strokeWidth="1.3" fill="none" strokeLinecap="round" />
              <path d="M11,4 Q11,0 11,1" stroke="var(--sage)" strokeWidth="1.3" fill="none" strokeLinecap="round" />
              <path d="M11,4 Q14,1 16,2" stroke="var(--sage)" strokeWidth="1.3" fill="none" strokeLinecap="round" />
            </svg>
          }
        />

        <div className="recipes-section-nav">
          <button className="recipes-section-nav__tab" onClick={() => navigate('/recipes')}>Recipes</button>
          <button className="recipes-section-nav__tab recipes-section-nav__tab--active">Ingredients</button>
        </div>

        <div className="recipes-hero">
          <h1 className="recipes-title">Ingredient Pantry</h1>
          <p className="recipes-subtitle">All known ingredients across your recipes</p>
        </div>

        <div className="recipes-toolbar">
          <div className="recipes-search-wrap">
            <svg width="16" height="16" viewBox="0 0 16 16" className="recipes-search-icon">
              <circle cx="6.5" cy="6.5" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
              <line x1="10" y1="10" x2="14" y2="14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            <input
              className="recipes-search"
              placeholder="Search ingredients..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <button className="recipes-create-btn" onClick={() => setShowCreate(true)}>
            <svg width="16" height="16" viewBox="0 0 16 16">
              <line x1="8" y1="2" x2="8" y2="14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              <line x1="2" y1="8" x2="14" y2="8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
            New Ingredients
          </button>
        </div>

        {availableCategories.length > 1 && (
          <div className="recipes-meal-tabs">
            <button
              className={`recipes-meal-tab ${categoryTab === null ? 'recipes-meal-tab--active' : ''}`}
              onClick={() => setCategoryTab(null)}
            >All</button>
            {availableCategories.map(c => (
              <button
                key={c}
                className={`recipes-meal-tab ${categoryTab === c ? 'recipes-meal-tab--active' : ''}`}
                onClick={() => setCategoryTab(categoryTab === c ? null : c)}
              >{c}</button>
            ))}
          </div>
        )}

        {error && <p className="ingredients-error">{error}</p>}

        {/* Create form */}
        {showCreate && (
          <div className="ingredients-create-wrap">
          <div className="ingredients-create-form">
            <h3 className="ingredients-create-title">New Ingredients</h3>
            <form onSubmit={handleCreate}>
              {createRows.map((row, i) => (
                <div key={i} className="ingredients-form-row">
                  {i === 0 ? (
                    <>
                      <div className="ingredients-form-field ingredients-form-field--grow">
                        <label className="ingredients-form-label">name</label>
                        <input
                          type="text"
                          className="ingredients-input"
                          placeholder="e.g. garlic"
                          value={row.name}
                          onChange={e => updateCreateRow(i, 'name', e.target.value)}
                          autoFocus
                        />
                      </div>
                      <div className="ingredients-form-field">
                        <label className="ingredients-form-label">category</label>
                        <select className="ingredients-select" value={row.category} onChange={e => updateCreateRow(i, 'category', e.target.value)}>
                          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                        </select>
                      </div>
                      <div className="ingredients-form-field">
                        <label className="ingredients-form-label">default unit</label>
                        <select className="ingredients-select" value={row.unit} onChange={e => updateCreateRow(i, 'unit', e.target.value)}>
                          {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                        </select>
                      </div>
                    </>
                  ) : (
                    <>
                      <div className="ingredients-form-field ingredients-form-field--grow">
                        <input
                          type="text"
                          className="ingredients-input"
                          placeholder="e.g. garlic"
                          value={row.name}
                          onChange={e => updateCreateRow(i, 'name', e.target.value)}
                          autoFocus
                        />
                      </div>
                      <div className="ingredients-form-field">
                        <select className="ingredients-select" value={row.category} onChange={e => updateCreateRow(i, 'category', e.target.value)}>
                          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                        </select>
                      </div>
                      <div className="ingredients-form-field">
                        <select className="ingredients-select" value={row.unit} onChange={e => updateCreateRow(i, 'unit', e.target.value)}>
                          {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                        </select>
                      </div>
                      <button type="button" className="ingredients-btn-remove-row" onClick={() => removeCreateRow(i)}>
                        <svg width="14" height="14" viewBox="0 0 14 14">
                          <line x1="3" y1="3" x2="11" y2="11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                          <line x1="11" y1="3" x2="3" y2="11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                        </svg>
                      </button>
                    </>
                  )}
                </div>
              ))}
              <button
                type="button"
                className="ingredients-add-row-btn"
                onClick={() => setCreateRows(rows => [...rows, emptyRow()])}
              >+ Add another</button>
              {createError && <p className="ingredients-error">{createError}</p>}
              <div className="ingredients-form-actions">
                <button type="button" className="ingredients-btn-secondary" onClick={() => { setShowCreate(false); setCreateRows([emptyRow()]); }}>Cancel</button>
                <button type="submit" className="ingredients-btn-primary" disabled={creating || !createRows.some(r => r.name.trim())}>
                  {creating ? 'Creating...' : `Create ${createRows.filter(r => r.name.trim()).length || ''}`}
                </button>
              </div>
            </form>
          </div>
          </div>
        )}

        {loading ? (
          <p className="ingredients-loading">Loading ingredients...</p>
        ) : (
          <div className="ingredients-list">
            {Object.entries(grouped)
              .sort(([a], [b]) => a.localeCompare(b))
              .map(([category, ings]) => (
                <div key={category} className="ingredients-category-group">
                  <h3 className="ingredients-category-title">{category}</h3>
                  <div className="ingredients-category-items">
                    {ings.sort((a, b) => a.name.localeCompare(b.name)).map(ing => (
                      <div key={ing.id} className="ingredients-item">
                        {editingId === ing.id ? (
                          <div className="ingredients-item-edit">
                            <input
                              type="text"
                              className="ingredients-input ingredients-input--sm"
                              value={editName}
                              onChange={e => setEditName(e.target.value)}
                              autoFocus
                            />
                            <select className="ingredients-select ingredients-select--sm" value={editCategory} onChange={e => setEditCategory(e.target.value)}>
                              {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                            </select>
                            <select className="ingredients-select ingredients-select--sm" value={editUnit} onChange={e => setEditUnit(e.target.value)}>
                              {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                            </select>
                            <button className="ingredients-btn-save" onClick={handleSaveEdit} disabled={saving}>
                              {saving ? '...' : 'Save'}
                            </button>
                            <button className="ingredients-btn-cancel" onClick={cancelEdit}>Cancel</button>
                            {editError && <span className="ingredients-inline-error">{editError}</span>}
                          </div>
                        ) : (
                          <div className="ingredients-item-display">
                            <span className="ingredients-item-name">{ing.name}</span>
                            <span className="ingredients-item-unit">{ing.defaultUnit}</span>
                            <div className="ingredients-item-actions">
                              <button className="ingredients-btn-edit" onClick={() => startEdit(ing)}>Edit</button>
                              <button className="ingredients-btn-delete" onClick={() => setDeletingId(ing.id)}>Delete</button>
                            </div>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            {filtered.length === 0 && (
              <p className="ingredients-empty">No ingredients found.</p>
            )}
          </div>
        )}

        {/* Delete confirmation modal */}
        {deletingIngredient && (
          <div className="modal-overlay" onClick={() => !deleting && setDeletingId(null)}>
            <div className="ingredients-delete-modal" onClick={e => e.stopPropagation()}>
              <h2 className="ingredients-delete-title">Delete Ingredient</h2>
              <p className="ingredients-delete-text">
                Are you sure you want to delete <strong>{deletingIngredient.name}</strong>?
              </p>
              <p className="ingredients-delete-warning">
                Any recipes using this ingredient will be set back to draft and the ingredient will need to be re-resolved.
              </p>
              <div className="ingredients-delete-actions">
                <button className="ingredients-btn-secondary" onClick={() => setDeletingId(null)} disabled={deleting}>Cancel</button>
                <button className="ingredients-btn-danger" onClick={handleDelete} disabled={deleting}>
                  {deleting ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
