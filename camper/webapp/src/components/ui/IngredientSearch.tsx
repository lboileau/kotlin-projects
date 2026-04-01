import { useState } from 'react';
import type { IngredientResponse } from '../../api/client';
import './IngredientSearch.css';

interface IngredientSearchProps {
  ingredients: IngredientResponse[];
  onSelect: (ingredient: IngredientResponse) => void;
  placeholder?: string;
  selectedIngredient?: IngredientResponse | null;
  onClear?: () => void;
}

export function IngredientSearch({
  ingredients,
  onSelect,
  placeholder = 'Search ingredients...',
  selectedIngredient = null,
  onClear,
}: IngredientSearchProps) {
  const [search, setSearch] = useState('');

  const filtered = search
    ? ingredients.filter(i => i.name.toLowerCase().includes(search.toLowerCase())).slice(0, 8)
    : [];

  const handleSelect = (ingredient: IngredientResponse) => {
    setSearch('');
    onSelect(ingredient);
  };

  const handleClear = () => {
    setSearch('');
    onClear?.();
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
        onChange={e => setSearch(e.target.value)}
        placeholder={placeholder}
        autoFocus
      />
      {search && filtered.length > 0 && (
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
      {search && filtered.length === 0 && (
        <div className="ingredient-search__dropdown">
          <span className="ingredient-search__dropdown-empty">No ingredients found</span>
        </div>
      )}
    </div>
  );
}
