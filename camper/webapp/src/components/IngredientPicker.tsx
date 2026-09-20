import { useId, useMemo, useRef, useState, type KeyboardEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Select, Text, TextField } from '@radix-ui/themes';
import { ApiError } from '../api/http';
import type { IngredientResponse } from '../api/ingredients';
import { createOrFindIngredient, useIngredients } from '../queries/ingredients';
import { CATEGORIES, UNITS, capitalize, normalizeCategory, normalizeUnit } from '../lib/ingredientConstants';
import { toast } from '../lib/toastStore';
import { SheetSelectContent } from './SheetSelectContent';
import './IngredientPicker.css';

const MAX_RESULTS = 30;

interface IngredientPickerProps {
  value: IngredientResponse | null;
  onSelect: (ingredient: IngredientResponse) => void;
  autoFocus?: boolean;
  initialQuery?: string;
  suggestedCategory?: string | null;
  suggestedUnit?: string | null;
  placeholder?: string;
  /** Visible label above the input. Omit to fall back to `placeholder` as the accessible name. */
  label?: string;
}

/** Prefix matches rank above substring matches; both groups are name-sorted, case-insensitive. */
function rankIngredients(list: IngredientResponse[], rawQuery: string): IngredientResponse[] {
  const query = rawQuery.trim().toLowerCase();
  const byName = (a: IngredientResponse, b: IngredientResponse) => a.name.localeCompare(b.name);

  if (!query) {
    return [...list].sort(byName).slice(0, MAX_RESULTS);
  }

  const prefix: IngredientResponse[] = [];
  const substring: IngredientResponse[] = [];
  for (const ingredient of list) {
    const name = ingredient.name.toLowerCase();
    if (name.startsWith(query)) {
      prefix.push(ingredient);
    } else if (name.includes(query)) {
      substring.push(ingredient);
    }
  }
  prefix.sort(byName);
  substring.sort(byName);
  return [...prefix, ...substring].slice(0, MAX_RESULTS);
}

/**
 * A mobile-friendly combobox over the cached ingredient list, shared by
 * every ingredient-picking flow in the app (writing a recipe, editing a
 * line, resolving an import). Results render below the input in normal
 * document flow — never a floating popover — so nothing gets clipped
 * when the on-screen keyboard opens.
 *
 * Consumers that need a fresh, empty picker after a selection (e.g. the
 * recipe lines editor, so entry can continue back to back) should
 * remount it with a changing `key` rather than rely on this component
 * to reset itself out from under an in-progress selection.
 */
export function IngredientPicker({
  value,
  onSelect,
  autoFocus = false,
  initialQuery,
  suggestedCategory,
  suggestedUnit,
  placeholder = 'Search ingredients',
  label,
}: IngredientPickerProps) {
  const { data: ingredients } = useIngredients();
  const queryClient = useQueryClient();
  const createIngredient = useMutation({
    mutationFn: (payload: { name: string; category: string; defaultUnit: string }) =>
      createOrFindIngredient(queryClient, payload),
    meta: { suppressErrorToast: true },
  });

  const [query, setQuery] = useState(() => value?.name ?? initialQuery ?? '');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const [creating, setCreating] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createCategory, setCreateCategory] = useState<string>(normalizeCategory(suggestedCategory));
  const [createUnit, setCreateUnit] = useState<string>(normalizeUnit(suggestedUnit));

  const inputId = useId();
  const listId = useId();
  const containerRef = useRef<HTMLDivElement>(null);

  const trimmedQuery = query.trim();
  const results = useMemo(() => rankIngredients(ingredients ?? [], query), [ingredients, query]);
  const hasExactMatch = useMemo(
    () => trimmedQuery.length > 0 && (ingredients ?? []).some((i) => i.name.toLowerCase() === trimmedQuery.toLowerCase()),
    [ingredients, trimmedQuery],
  );
  const showCreateRow = trimmedQuery.length > 0 && !hasExactMatch;
  const optionCount = results.length + (showCreateRow ? 1 : 0);

  function optionId(index: number): string {
    return `${listId}-option-${index}`;
  }

  function selectIngredient(ingredient: IngredientResponse) {
    onSelect(ingredient);
    setQuery(ingredient.name);
    setOpen(false);
    setActiveIndex(0);
  }

  function startCreate() {
    setCreateName(trimmedQuery);
    setCreateCategory(normalizeCategory(suggestedCategory));
    setCreateUnit(normalizeUnit(suggestedUnit));
    setCreating(true);
  }

  function cancelCreate() {
    setCreating(false);
    setOpen(true);
  }

  // The create sub-form can't be a nested <form> (callers already wrap this
  // in their own), so Enter in a plain text field here would otherwise
  // bubble to and submit the SURROUNDING form (dropping the ingredient being
  // created on /recipes/new, or tripping "Choose an ingredient" in a line
  // sheet). Only intercept it from an actual text input — a Select's
  // trigger is a <button>, and Enter there is how you open it.
  function handleCreateKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== 'Enter') return;
    if ((event.target as HTMLElement).tagName === 'INPUT') {
      event.preventDefault();
      event.stopPropagation();
      void confirmCreate();
    }
  }

  async function confirmCreate() {
    const name = createName.trim();
    if (!name) return;
    try {
      // Resolves to the existing ingredient on a 409 name collision rather than throwing.
      const result = await createIngredient.mutateAsync({
        name,
        category: createCategory,
        defaultUnit: createUnit,
      });
      setCreating(false);
      selectIngredient(result);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Could not create the ingredient.';
      toast.error(message);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (creating) return;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((i) => Math.min(i + 1, Math.max(optionCount - 1, 0)));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((i) => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      // Never let Enter in this input fall through to a surrounding <form>'s
      // submit — even while results are still loading (optionCount === 0)
      // or the list isn't open yet.
      event.preventDefault();
      if (!open || optionCount === 0) return;
      if (activeIndex < results.length) {
        selectIngredient(results[activeIndex]);
      } else if (showCreateRow) {
        startCreate();
      }
    } else if (event.key === 'Escape') {
      if (open) {
        event.preventDefault();
        setOpen(false);
      }
    }
  }

  // The listbox itself is only rendered under this same condition (see below) —
  // gating aria-expanded/activedescendant on it too keeps them from ever
  // referencing an <li> that isn't actually in the DOM (e.g. mid-`creating`).
  const listboxVisible = open && !creating && optionCount > 0;
  const activeId = listboxVisible ? optionId(activeIndex) : undefined;

  return (
    <div className="ingredient-picker" ref={containerRef}>
      {label && (
        <Text as="label" htmlFor={inputId} size="2" weight="medium" className="ingredient-picker__label">
          {label}
        </Text>
      )}
      <TextField.Root
        id={inputId}
        role="combobox"
        aria-expanded={listboxVisible}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={activeId}
        aria-label={label ? undefined : placeholder}
        autoComplete="off"
        autoCapitalize="off"
        autoCorrect="off"
        spellCheck={false}
        enterKeyHint="search"
        placeholder={placeholder}
        value={query}
        size="3"
        autoFocus={autoFocus}
        disabled={creating}
        onFocus={() => {
          setOpen(true);
          // Inside a scrolling sheet body, the on-screen keyboard can push the
          // results (and especially the Create row) out of view — nudge the
          // picker back into view once the keyboard's animation has settled.
          window.setTimeout(() => {
            containerRef.current?.scrollIntoView({ block: 'start', behavior: 'smooth' });
          }, 300);
        }}
        onChange={(event) => {
          setQuery(event.target.value);
          setOpen(true);
          setActiveIndex(0);
        }}
        onKeyDown={handleKeyDown}
        onBlur={() => {
          // Let a tap on a row (onPointerDown, which fires first) register before we close.
          window.setTimeout(() => setOpen(false), 100);
        }}
      />

      {open && !creating && (
        <ul id={listId} role="listbox" className="ingredient-picker__list" aria-label={placeholder}>
          {results.map((ingredient, index) => (
            <li
              key={ingredient.id}
              id={optionId(index)}
              role="option"
              aria-selected={index === activeIndex}
              className={`ingredient-picker__row${index === activeIndex ? ' ingredient-picker__row--active' : ''}`}
              // onPointerDown (not onMouseDown/onClick) fires before the input's blur on both
              // touch and mouse, and preventDefault here stops that blur from ever happening —
              // without it, the input would close the list (via onBlur) before the tap registers.
              onPointerDown={(event) => {
                event.preventDefault();
                selectIngredient(ingredient);
              }}
            >
              <span className="ingredient-picker__row-name">{ingredient.name}</span>
              <span className="ingredient-picker__row-category">{capitalize(ingredient.category)}</span>
            </li>
          ))}
          {showCreateRow && (
            <li
              id={optionId(results.length)}
              role="option"
              aria-selected={activeIndex === results.length}
              className={`ingredient-picker__row ingredient-picker__row--create${
                activeIndex === results.length ? ' ingredient-picker__row--active' : ''
              }`}
              onPointerDown={(event) => {
                event.preventDefault();
                startCreate();
              }}
            >
              Create &ldquo;{trimmedQuery}&rdquo;
            </li>
          )}
          {results.length === 0 && !showCreateRow && (
            <li className="ingredient-picker__row ingredient-picker__row--empty" aria-disabled="true">
              No ingredients found
            </li>
          )}
        </ul>
      )}

      {creating && (
        <div className="ingredient-picker__create" onKeyDown={handleCreateKeyDown}>
          <Text as="label" size="2" weight="medium" className="ingredient-picker__create-field">
            Name
            <TextField.Root
              value={createName}
              onChange={(event) => setCreateName(event.target.value)}
              autoFocus
              size="3"
              autoCapitalize="words"
              enterKeyHint="done"
            />
          </Text>
          <div className="ingredient-picker__create-row">
            <Text as="label" size="2" weight="medium" className="ingredient-picker__create-field">
              Category
              <Select.Root value={createCategory} onValueChange={setCreateCategory} size="3">
                <Select.Trigger />
                <SheetSelectContent>
                  {CATEGORIES.map((category) => (
                    <Select.Item key={category} value={category}>
                      {capitalize(category)}
                    </Select.Item>
                  ))}
                </SheetSelectContent>
              </Select.Root>
            </Text>
            <Text as="label" size="2" weight="medium" className="ingredient-picker__create-field">
              Unit
              <Select.Root value={createUnit} onValueChange={setCreateUnit} size="3">
                <Select.Trigger />
                <SheetSelectContent>
                  {UNITS.map((unit) => (
                    <Select.Item key={unit} value={unit}>
                      {unit}
                    </Select.Item>
                  ))}
                </SheetSelectContent>
              </Select.Root>
            </Text>
          </div>
          <div className="ingredient-picker__create-actions">
            <Button size="3" type="button" variant="soft" onClick={cancelCreate} disabled={createIngredient.isPending}>
              Cancel
            </Button>
            <Button
              size="3"
              type="button"
              onClick={confirmCreate}
              loading={createIngredient.isPending}
              disabled={!createName.trim()}
            >
              Create
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
