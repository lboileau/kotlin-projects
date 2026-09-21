import { useId, useMemo, useRef, useState, type FormEvent } from 'react';
import { Badge, Button, Callout, Select, Text, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { CategoryChips } from '../../components/CategoryChips';
import { Sheet } from '../../components/Sheet';
import { SheetSelectContent } from '../../components/SheetSelectContent';
import { useSheet } from '../../components/useSheet';
import { findIngredientByName, useCreateIngredient, useIngredients } from '../../queries/ingredients';
import { ApiError } from '../../api/http';
import { DEFAULT_UNIT, UNITS, UNIT_FOR_CATEGORY, type Category } from '../../lib/ingredientConstants';
import { toast } from '../../lib/toastStore';
import './NewIngredientSheet.css';

export function NewIngredientSheet() {
  const sheet = useSheet('/ingredients');
  const { data: ingredients } = useIngredients();
  const createIngredient = useCreateIngredient();
  const nameRef = useRef<HTMLInputElement>(null);
  // Names being created right now. The duplicate check below reads the
  // cached list, which a create still in flight isn't in yet — and nothing
  // is disabled while one is, so "Butter" then "butter" could both get through.
  const inFlightRef = useRef(new Set<string>());

  // Category and unit stick between entries — rapid multi-add usually adds
  // several ingredients of the same kind (e.g. a handful of spices) in a row.
  // The category starts unchosen and is required (see CategoryChips).
  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [unit, setUnit] = useState<string>(DEFAULT_UNIT);
  const [error, setError] = useState<string | null>(null);
  const [categoryMissing, setCategoryMissing] = useState(false);
  const errorId = useId();
  const categoryLabelId = useId();

  function handleCategoryChange(next: string) {
    setCategory(next);
    setCategoryMissing(false);
    setError(null);
    // Picking a category suggests its usual unit; the user can still change it.
    setUnit(UNIT_FOR_CATEGORY[next as Category]);
  }

  const trimmed = name.trim();
  const suggestions = useMemo(() => {
    if (!trimmed) return [];
    const needle = trimmed.toLowerCase();
    return (ingredients ?? []).filter((i) => i.name.toLowerCase().includes(needle)).slice(0, 3);
  }, [ingredients, trimmed]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!trimmed) {
      setError('Enter a name.');
      return;
    }
    if (!category) {
      setCategoryMissing(true);
      setError('Choose a category.');
      return;
    }
    // Case-insensitive check against the cached list, same message as the
    // (case-sensitive) 409 the server would otherwise return — catches a
    // "Butter"/"butter" collision the server's own check wouldn't.
    const nameKey = trimmed.toLowerCase();
    if (findIngredientByName(ingredients ?? [], trimmed) || inFlightRef.current.has(nameKey)) {
      setError(`"${trimmed}" already exists.`);
      return;
    }
    setError(null);

    // Clear and refocus straight away and don't wait for the server (the
    // same shape as the shopping list's quick add): while this awaited the
    // request, the next name typed was wiped when it resolved and Enter was
    // swallowed by the disabled button, so fast entry lost ingredients.
    setName('');
    nameRef.current?.focus();
    inFlightRef.current.add(nameKey);
    createIngredient.mutate(
      { name: trimmed, category, defaultUnit: unit },
      {
        onSuccess: (created) => toast.info(`Added "${created.name}"`),
        onSettled: () => inFlightRef.current.delete(nameKey),
        onError: (err) => {
          // Only put the failed name back if the next one hasn't been started.
          setName((current) => current || trimmed);
          if (err instanceof ApiError && err.code === 'CONFLICT') {
            setError(`"${trimmed}" already exists.`);
          } else {
            setError(err instanceof ApiError ? err.message : `Could not add "${trimmed}".`);
          }
        },
      },
    );
  }

  return (
    <Sheet {...sheet.sheetProps} title="Add ingredients">
      <form className="new-ingredient-sheet__form" onSubmit={handleSubmit}>
        <Text as="label" size="2" weight="medium" className="new-ingredient-sheet__field">
          Name
          <TextField.Root
            ref={nameRef}
            autoFocus
            size="3"
            value={name}
            autoCapitalize="words"
            enterKeyHint="done"
            aria-invalid={Boolean(error)}
            aria-describedby={error ? errorId : undefined}
            onChange={(event) => {
              setName(event.target.value);
              setError(null);
            }}
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

        <div className="new-ingredient-sheet__field">
          <Text as="span" size="2" weight="medium" id={categoryLabelId}>
            Category
          </Text>
          <CategoryChips
            value={category}
            onChange={handleCategoryChange}
            labelledBy={categoryLabelId}
            invalid={categoryMissing}
          />
        </div>

        <Text as="label" size="2" weight="medium" className="new-ingredient-sheet__field">
          Unit
          <Select.Root value={unit} onValueChange={setUnit} size="3">
            <Select.Trigger />
            <SheetSelectContent>
              {UNITS.map((u) => (
                <Select.Item key={u} value={u}>
                  {u}
                </Select.Item>
              ))}
            </SheetSelectContent>
          </Select.Root>
        </Text>

        {error && (
          <Callout.Root id={errorId} color="red" variant="surface" size="1" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        )}

        {/* Never disabled or in a loading state: that would swallow the Enter for the next ingredient. */}
        <Button type="submit" size="3">
          Add
        </Button>
      </form>

      <Button variant="soft" size="3" className="new-ingredient-sheet__done" onClick={() => sheet.close()}>
        Done
      </Button>
    </Sheet>
  );
}
