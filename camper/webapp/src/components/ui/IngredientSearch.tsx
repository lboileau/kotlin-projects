import { useState } from 'react';
import type { IngredientResponse, CreateIngredientRequest } from '../../api/client';
import { INGREDIENT_CATEGORIES, UNITS } from '../../lib/constants';
import './IngredientSearch.css';

interface IngredientSearchProps {
  ingredients: IngredientResponse[];
  onSelect: (ingredient: IngredientResponse) => void;
  placeholder?: string;
  selectedIngredient?: IngredientResponse | null;
  onClear?: () => void;
  onCreateIngredient?: (data: CreateIngredientRequest) => Promise<IngredientResponse>;
}

export function IngredientSearch({
  ingredients,
  onSelect,
  placeholder = 'Search ingredients...',
  selectedIngredient = null,
  onClear,
  onCreateIngredient,
}: IngredientSearchProps) {
  const [search, setSearch] = useState('');
  const [createMode, setCreateMode] = useState(false);
  const [newName, setNewName] = useState('');
  const [newCategory, setNewCategory] = useState<string>('produce');
  const [newUnit, setNewUnit] = useState<string>('pieces');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  const filtered = search
    ? ingredients.filter(i => i.name.toLowerCase().includes(search.toLowerCase())).slice(0, 8)
    : [];

  const handleSelect = (ingredient: IngredientResponse) => {
    setSearch('');
    setCreateMode(false);
    onSelect(ingredient);
  };

  const handleClear = () => {
    setSearch('');
    setCreateMode(false);
    onClear?.();
  };

  const handleStartCreate = () => {
    setNewName(search);
    setNewCategory('produce');
    setNewUnit('pieces');
    setCreateError('');
    setCreateMode(true);
  };

  const handleCreate = async () => {
    if (!onCreateIngredient || !newName.trim()) return;
    setCreating(true);
    setCreateError('');
    try {
      const created = await onCreateIngredient({
        name: newName.trim(),
        category: newCategory,
        defaultUnit: newUnit,
      });
      setSearch('');
      setCreateMode(false);
      setNewName('');
      setNewCategory('produce');
      setNewUnit('pieces');
      onSelect(created);
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create ingredient');
    } finally {
      setCreating(false);
    }
  };

  if (selectedIngredient) {
    return (
      <div className="ingredient-search__selected">
        <span className="ingredient-search__selected-name">{selectedIngredient.name}</span>
        <button className="ingredient-search__selected-clear" onClick={handleClear}>
          <svg width="10" height="10" viewBox="0 0 10 10"><path d="M2,2 L8,8 M8,2 L2,8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" /></svg>
        </button>
      </div>
    );
  }

  return (
    <div className="ingredient-search__wrap">
      <input
        className="ingredient-search__input"
        value={search}
        onChange={e => { setSearch(e.target.value); setCreateMode(false); }}
        placeholder={placeholder}
        autoFocus
      />
      {search && !createMode && filtered.length > 0 && (
        <div className="ingredient-search__dropdown">
          {filtered.map(ing => (
            <button
              key={ing.id}
              className="ingredient-search__dropdown-item"
              onClick={() => handleSelect(ing)}
            >
              <span>{ing.name}</span>
              <span className="ingredient-search__dropdown-cat">{ing.category}</span>
            </button>
          ))}
        </div>
      )}
      {search && !createMode && filtered.length === 0 && (
        <div className="ingredient-search__dropdown">
          <span className="ingredient-search__dropdown-empty">No ingredients found</span>
          {onCreateIngredient && (
            <button
              className="ingredient-search__dropdown-create"
              onClick={handleStartCreate}
            >
              + Create new ingredient
            </button>
          )}
        </div>
      )}
      {createMode && (
        <div className="ingredient-search__create-form">
          <input
            type="text"
            className="ingredient-search__input"
            placeholder="Ingredient name"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            autoFocus
          />
          <div className="ingredient-search__create-row">
            <select
              className="ingredient-search__create-select"
              value={newCategory}
              onChange={e => setNewCategory(e.target.value)}
            >
              {INGREDIENT_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
            <select
              className="ingredient-search__create-select"
              value={newUnit}
              onChange={e => setNewUnit(e.target.value)}
            >
              {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
            </select>
          </div>
          {createError && <span className="ingredient-search__create-error">{createError}</span>}
          <div className="ingredient-search__create-actions">
            <button
              className="ingredient-search__create-cancel"
              onClick={() => setCreateMode(false)}
            >
              Cancel
            </button>
            <button
              className="ingredient-search__create-save"
              onClick={handleCreate}
              disabled={creating || !newName.trim()}
            >
              {creating ? 'Creating...' : 'Create & Select'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
