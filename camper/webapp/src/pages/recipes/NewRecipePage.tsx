import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Callout, IconButton, Select, Text, TextArea, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon, MinusIcon, PlusIcon } from '@radix-ui/react-icons';
import { PageHeader } from '../../components/PageHeader';
import { useCreateRecipe } from '../../queries/recipes';
import { MEALS, THEMES, capitalize } from '../../lib/ingredientConstants';
import { parseQuantity } from '../../lib/parseQuantity';
import { LinesEditor, type DraftLine } from './LinesEditor';
import './RecipeForm.css';

export function NewRecipePage() {
  const navigate = useNavigate();
  const createRecipe = useCreateRecipe();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [servings, setServings] = useState(2);
  const [webLink, setWebLink] = useState('');
  const [meal, setMeal] = useState('');
  const [theme, setTheme] = useState('');
  const [lines, setLines] = useState<DraftLine[]>([]);
  const [error, setError] = useState<string | null>(null);

  function validate(): string | null {
    if (!name.trim()) return 'Name is required.';
    if (!Number.isFinite(servings) || servings < 1) return 'Servings must be at least 1.';
    if (webLink.trim()) {
      try {
        const parsed = new URL(webLink.trim());
        if (!parsed.protocol.startsWith('http')) return 'Enter a valid source URL.';
      } catch {
        return 'Enter a valid source URL.';
      }
    }
    if (lines.some((line) => parseQuantity(line.quantity) === null)) {
      return 'Every ingredient needs a valid quantity (e.g. "1", "1.5", or "1 1/2").';
    }
    return null;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);

    const created = await createRecipe.mutateAsync({
      name: name.trim(),
      description: description.trim() || undefined,
      webLink: webLink.trim() || undefined,
      baseServings: servings,
      meal: meal || undefined,
      theme: theme || undefined,
      // validate() above already confirmed every line parses; the `?? 0` fallback never triggers.
      ingredients: lines.map((line) => ({
        ingredientId: line.ingredient.id,
        quantity: parseQuantity(line.quantity) ?? 0,
        unit: line.unit,
      })),
    });
    navigate(`/recipes/${created.id}`, { replace: true });
  }

  return (
    <div className="recipe-form-page">
      <PageHeader
        title="New recipe"
        backTo="/recipes"
        actions={
          <Button size="3" type="submit" form="new-recipe-form" loading={createRecipe.isPending}>
            Save
          </Button>
        }
      />
      <form id="new-recipe-form" className="recipe-form-page__body" onSubmit={handleSubmit}>
        <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
          Name
          <TextField.Root
            size="3"
            value={name}
            onChange={(event) => setName(event.target.value)}
            autoFocus
            autoCapitalize="words"
            enterKeyHint="next"
          />
        </Text>

        <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
          Description
          <TextArea
            size="3"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={3}
            autoCapitalize="sentences"
          />
        </Text>

        <div className="recipe-form-page__field">
          <Text as="span" size="2" weight="medium">
            Servings
          </Text>
          <div className="recipe-form-page__stepper">
            <IconButton
              size="3"
              type="button"
              variant="soft"
              aria-label="Decrease servings"
              className="recipe-form-page__icon-button"
              onClick={() => setServings((s) => Math.max(1, s - 1))}
            >
              <MinusIcon />
            </IconButton>
            <Text as="span" size="4" weight="medium" className="recipe-form-page__stepper-value">
              {servings}
            </Text>
            <IconButton
              size="3"
              type="button"
              variant="soft"
              aria-label="Increase servings"
              className="recipe-form-page__icon-button"
              onClick={() => setServings((s) => s + 1)}
            >
              <PlusIcon />
            </IconButton>
          </div>
        </div>

        <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
          Source URL
          <TextField.Root
            type="url"
            inputMode="url"
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            size="3"
            placeholder="https://…"
            value={webLink}
            onChange={(event) => setWebLink(event.target.value)}
          />
        </Text>

        <div className="recipe-form-page__row">
          <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
            Meal
            <Select.Root value={meal} onValueChange={setMeal} size="3">
              <Select.Trigger placeholder="None" />
              <Select.Content>
                {MEALS.map((m) => (
                  <Select.Item key={m} value={m}>
                    {capitalize(m)}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Text>

          <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
            Theme
            <Select.Root value={theme} onValueChange={setTheme} size="3">
              <Select.Trigger placeholder="None" />
              <Select.Content>
                {THEMES.map((t) => (
                  <Select.Item key={t} value={t}>
                    {capitalize(t)}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Text>
        </div>

        <div className="recipe-form-page__field">
          <Text as="span" size="2" weight="medium">
            Ingredients
          </Text>
          <LinesEditor lines={lines} onChange={setLines} />
        </div>

        {error && (
          <Callout.Root color="red" variant="surface" size="1" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        )}
      </form>
    </div>
  );
}
