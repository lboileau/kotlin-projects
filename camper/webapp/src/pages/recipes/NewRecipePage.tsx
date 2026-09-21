import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@radix-ui/themes';
import { PageHeader } from '../../components/PageHeader';
import { useCreateRecipe } from '../../queries/recipes';
import { enterMovesOn } from '../../lib/enterMovesOn';
import { parseQuantity } from '../../lib/parseQuantity';
import { toast } from '../../lib/toastStore';
import type { PendingLine } from './LinesEditor';
import { RecipeFormFields } from './RecipeFormFields';
import { validateRecipeForm, type RecipeFormValues } from './recipeForm';
import { EMPTY_DRAFT, clearNewRecipeDraft, readNewRecipeDraft, writeNewRecipeDraft } from './newRecipeDraft';
import './RecipeForm.css';

export function NewRecipePage() {
  const navigate = useNavigate();
  const createRecipe = useCreateRecipe();

  // Whatever was typed here before and never saved (see newRecipeDraft.ts).
  const [restored] = useState(readNewRecipeDraft);
  const [values, setValues] = useState<RecipeFormValues>(restored ?? EMPTY_DRAFT);
  const [error, setError] = useState<string | null>(null);
  // The add row's contents: a line typed but not yet added with +.
  const [pendingLine, setPendingLine] = useState<PendingLine | null>(null);
  const [linesEditorKey, setLinesEditorKey] = useState(0);
  // StrictMode runs mount effects twice in development; one toast is enough.
  const announcedRef = useRef(false);
  // Set once the recipe is created, so the unmount that follows doesn't re-save the draft.
  const savedRef = useRef(false);

  function resetForm() {
    setValues(EMPTY_DRAFT);
    setError(null);
    // The add row's ingredient and quantity live inside LinesEditor: remount
    // it, or a half-typed line survives Discard and is auto-saved straight back.
    setPendingLine(null);
    setLinesEditorKey((key) => key + 1);
  }

  useEffect(() => {
    if (restored && !announcedRef.current) {
      announcedRef.current = true;
      toast.info('Draft restored.', { label: 'Discard', onClick: resetForm });
    }
    // Once, on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // A complete line still sitting in the add row is kept as a line, so it comes back too.
  const completePendingLine = pendingLine && parseQuantity(pendingLine.quantity) !== null ? pendingLine : null;

  useEffect(() => {
    if (savedRef.current) return;
    const lines = completePendingLine
      ? [...values.lines, { clientId: crypto.randomUUID(), ...completePendingLine }]
      : values.lines;
    writeNewRecipeDraft({ ...values, lines });
  }, [values, completePendingLine]);

  function validateSource(): string | null {
    const webLink = values.webLink.trim();
    if (!webLink) return null;
    try {
      return new URL(webLink).protocol.startsWith('http') ? null : 'Enter a valid source URL.';
    } catch {
      return 'Enter a valid source URL.';
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const result = validateRecipeForm(values, pendingLine);
    const validationError = 'error' in result ? result.error : validateSource();
    if (validationError || 'error' in result) {
      setError(validationError);
      return;
    }
    setError(null);

    const created = await createRecipe.mutateAsync({
      name: values.name.trim(),
      description: values.description.trim() || undefined,
      webLink: values.webLink.trim() || undefined,
      baseServings: values.servings,
      meal: values.meal || undefined,
      theme: values.theme || undefined,
      // validateRecipeForm confirmed every line parses; the `?? 0` fallback never triggers.
      ingredients: result.lines.map((line) => ({
        ingredientId: line.ingredient.id,
        quantity: parseQuantity(line.quantity) ?? 0,
        unit: line.unit,
      })),
    });
    savedRef.current = true;
    clearNewRecipeDraft();
    navigate(`/recipes/${created.id}`, { replace: true });
  }

  return (
    <div className="recipe-form-page">
      <PageHeader
        title="New recipe"
        backTo="/recipes"
        task
        actions={
          <Button size="3" type="submit" form="new-recipe-form" loading={createRecipe.isPending}>
            Save
          </Button>
        }
      />
      <form id="new-recipe-form" className="recipe-form-page__body" onSubmit={handleSubmit} onKeyDown={enterMovesOn}>
        <RecipeFormFields
          values={values}
          onChange={(patch) => setValues((current) => ({ ...current, ...patch }))}
          onPendingLineChange={setPendingLine}
          linesEditorKey={linesEditorKey}
          sourceEditable
          autoFocusName
          error={error}
        />
      </form>
    </div>
  );
}
