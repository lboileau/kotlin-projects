import { useMemo, useRef, useState, type FormEvent } from 'react';
import { Badge, Button, Callout, Select, Text, TextField } from '@radix-ui/themes';
import { CheckIcon, ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useCloseSheet } from '../../components/useCloseSheet';
import { findIngredientByName, useCreateIngredient, useIngredients } from '../../queries/ingredients';
import { ApiError } from '../../api/http';
import { CATEGORIES, DEFAULT_CATEGORY, DEFAULT_UNIT, UNITS, capitalize } from '../../lib/ingredientConstants';
import type { IngredientResponse } from '../../api/ingredients';
import './NewIngredientSheet.css';

export function NewIngredientSheet() {
  const closeSheet = useCloseSheet('/ingredients');
  const { data: ingredients } = useIngredients();
  const createIngredient = useCreateIngredient();
  const nameRef = useRef<HTMLInputElement>(null);

  // Category and unit stick between entries — rapid multi-add usually adds
  // several ingredients of the same kind (e.g. a handful of spices) in a row.
  const [name, setName] = useState('');
  const [category, setCategory] = useState<string>(DEFAULT_CATEGORY);
  const [unit, setUnit] = useState<string>(DEFAULT_UNIT);
  const [error, setError] = useState<string | null>(null);
  const [added, setAdded] = useState<IngredientResponse[]>([]);

  const trimmed = name.trim();
  const suggestions = useMemo(() => {
    if (!trimmed) return [];
    const needle = trimmed.toLowerCase();
    return (ingredients ?? []).filter((i) => i.name.toLowerCase().includes(needle)).slice(0, 3);
  }, [ingredients, trimmed]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!trimmed) {
      setError('Enter a name.');
      return;
    }
    // Case-insensitive check against the cached list, same message as the
    // (case-sensitive) 409 the server would otherwise return — catches a
    // "Butter"/"butter" collision the server's own check wouldn't.
    if (findIngredientByName(ingredients ?? [], trimmed)) {
      setError(`"${trimmed}" already exists.`);
      return;
    }
    setError(null);
    try {
      const created = await createIngredient.mutateAsync({ name: trimmed, category, defaultUnit: unit });
      setAdded((prev) => [created, ...prev]);
      setName('');
      nameRef.current?.focus();
    } catch (err) {
      if (err instanceof ApiError && err.code === 'CONFLICT') {
        setError(`"${trimmed}" already exists.`);
      } else {
        setError(err instanceof ApiError ? err.message : 'Could not add this ingredient.');
      }
    }
  }

  return (
    <Sheet title="Add ingredients" onClose={closeSheet}>
      <form className="new-ingredient-sheet__form" onSubmit={handleSubmit}>
        <Text as="label" size="2" weight="medium" className="new-ingredient-sheet__field">
          Name
          <TextField.Root
            ref={nameRef}
            autoFocus
            size="3"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </Text>

        {suggestions.length > 0 && (
          <div className="new-ingredient-sheet__suggestions">
            <Text as="p" size="1" color="gray">
              Already in the list:
            </Text>
            <div className="new-ingredient-sheet__suggestion-list">
              {suggestions.map((s) => (
                <Badge key={s.id} variant="soft" color="gray">
                  {s.name}
                </Badge>
              ))}
            </div>
          </div>
        )}

        <div className="new-ingredient-sheet__row">
          <Text as="label" size="2" weight="medium" className="new-ingredient-sheet__field">
            Category
            <Select.Root value={category} onValueChange={setCategory} size="3">
              <Select.Trigger />
              <Select.Content>
                {CATEGORIES.map((c) => (
                  <Select.Item key={c} value={c}>
                    {capitalize(c)}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Text>
          <Text as="label" size="2" weight="medium" className="new-ingredient-sheet__field">
            Unit
            <Select.Root value={unit} onValueChange={setUnit} size="3">
              <Select.Trigger />
              <Select.Content>
                {UNITS.map((u) => (
                  <Select.Item key={u} value={u}>
                    {u}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Text>
        </div>

        {error && (
          <Callout.Root color="red" variant="surface" size="1">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        )}

        <Button type="submit" size="3" loading={createIngredient.isPending}>
          Add
        </Button>
      </form>

      {added.length > 0 && (
        <div className="new-ingredient-sheet__added">
          <Text as="p" size="1" color="gray">
            Added this session
          </Text>
          <ul className="new-ingredient-sheet__added-list">
            {added.map((ingredient) => (
              <li key={ingredient.id}>
                <CheckIcon /> {ingredient.name}
              </li>
            ))}
          </ul>
        </div>
      )}

      <Button variant="soft" size="3" className="new-ingredient-sheet__done" onClick={closeSheet}>
        Done
      </Button>
    </Sheet>
  );
}
