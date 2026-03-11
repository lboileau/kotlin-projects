import { useEffect, useState } from 'react';
import {
  api,
  type IngredientResponse,
  type CreateIngredientRequest,
  type UpdateIngredientRequest,
} from '../api/client';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AppHeader } from '../components/AppHeader';
import './IngredientsPage.css';
import '../components/Modal.css';

const UNITS = ['g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'oz', 'lb', 'pieces', 'whole', 'bunch', 'can', 'clove', 'pinch', 'slice', 'sprig'] as const;
const CATEGORIES = ['produce', 'dairy', 'meat', 'seafood', 'pantry', 'spice', 'condiment', 'frozen', 'bakery', 'other'] as const;

export function IngredientsPage() {
  const [ingredients, setIngredients] = useState<IngredientResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  // Create form
  const [showCreate, setShowCreate] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createCategory, setCreateCategory] = useState<string>('produce');
  const [createUnit, setCreateUnit] = useState<string>('pieces');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

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
    if (!createName.trim()) return;
    setCreating(true);
    setCreateError('');
    try {
      await api.createIngredient({ name: createName.trim(), category: createCategory, defaultUnit: createUnit });
      setCreateName('');
      setCreateCategory('produce');
      setCreateUnit('pieces');
      setShowCreate(false);
      await loadIngredients();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create ingredient');
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
    return !q || ing.name.toLowerCase().includes(q) || ing.category.toLowerCase().includes(q);
  });

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

        <div className="ingredients-toolbar">
          <input
            type="text"
            className="ingredients-search"
            placeholder="Search ingredients..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          <button className="ingredients-create-btn" onClick={() => setShowCreate(true)}>
            + New Ingredient
          </button>
        </div>

        {error && <p className="ingredients-error">{error}</p>}

        {/* Create form */}
        {showCreate && (
          <div className="ingredients-create-form">
            <h3 className="ingredients-create-title">New Ingredient</h3>
            <form onSubmit={handleCreate}>
              <div className="ingredients-form-row">
                <input
                  type="text"
                  className="ingredients-input"
                  placeholder="Name"
                  value={createName}
                  onChange={e => setCreateName(e.target.value)}
                  autoFocus
                />
                <select className="ingredients-select" value={createCategory} onChange={e => setCreateCategory(e.target.value)}>
                  {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
                <select className="ingredients-select" value={createUnit} onChange={e => setCreateUnit(e.target.value)}>
                  {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                </select>
              </div>
              {createError && <p className="ingredients-error">{createError}</p>}
              <div className="ingredients-form-actions">
                <button type="button" className="ingredients-btn-secondary" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="ingredients-btn-primary" disabled={creating || !createName.trim()}>
                  {creating ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
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
