import type { ReactNode } from 'react';
import { Callout, IconButton, Select, Tabs, Text, TextArea, TextField } from '@radix-ui/themes';
import { RecipeTabsList } from '../../components/RecipeTabs';
import type { RecipeTab } from '../../lib/recipeSteps';
import { ExclamationTriangleIcon, ExternalLinkIcon, MinusIcon, PlusIcon } from '@radix-ui/react-icons';
import { MEALS, THEMES, capitalize } from '../../lib/ingredientConstants';
import { StepsEditor } from './StepsEditor';
import { LinesEditor, type PendingLine } from './LinesEditor';
import type { RecipeFormValues } from './recipeForm';

interface RecipeFormFieldsProps {
  values: RecipeFormValues;
  onChange: (patch: Partial<RecipeFormValues>) => void;
  onPendingLineChange: (pending: PendingLine | null) => void;
  /** Bump to empty the ingredient add row (the New form's Discard). */
  linesEditorKey?: number;
  /** The source link can only be set when the recipe is created; afterwards it is shown, not edited. */
  sourceEditable: boolean;
  autoFocusName?: boolean;
  error: string | null;
  /** Which of the three tabs is showing; the page keeps it in the URL. */
  tab: RecipeTab;
  onTabChange: (tab: RecipeTab) => void;
  /** The Photos tab's content — the photo grid on Edit, a note on New (nothing to attach to yet). */
  photos: ReactNode;
  /** Extra content under the ingredients (the Edit form's note about lines still in review). */
  children?: ReactNode;
}

/**
 * The recipe form's fields — the same for a new recipe and for editing one,
 * so both are one UX: every field and every ingredient line is edited in
 * place (quantity, unit, delete, and an add row at the bottom), nothing is
 * sent until Save, and Save sends it all. What differs is only what Save
 * does (`NewRecipePage` creates; `EditRecipePage` works out what changed).
 */
export function RecipeFormFields({
  values,
  onChange,
  onPendingLineChange,
  linesEditorKey,
  sourceEditable,
  autoFocusName = false,
  error,
  tab,
  onTabChange,
  photos,
  children,
}: RecipeFormFieldsProps) {
  const { name, description, servings, webLink, meal, theme, lines } = values;

  return (
    <>
      <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
        Name
        <TextField.Root
          size="3"
          value={name}
          onChange={(event) => onChange({ name: event.target.value })}
          autoFocus={autoFocusName}
          autoCapitalize="words"
          enterKeyHint="next"
        />
      </Text>

      <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
        Description
        <TextArea
          size="3"
          value={description}
          onChange={(event) => onChange({ description: event.target.value })}
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
            onClick={() => onChange({ servings: Math.max(1, servings - 1) })}
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
            onClick={() => onChange({ servings: servings + 1 })}
          >
            <PlusIcon />
          </IconButton>
        </div>
      </div>

      {sourceEditable ? (
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
            onChange={(event) => onChange({ webLink: event.target.value })}
          />
        </Text>
      ) : (
        webLink && (
          <div className="recipe-form-page__field">
            <Text as="span" size="2" weight="medium">
              Source
            </Text>
            <a href={webLink} target="_blank" rel="noopener noreferrer" className="recipe-form-page__readonly-link">
              {webLink} <ExternalLinkIcon />
            </a>
          </div>
        )
      )}

      <div className="recipe-form-page__row">
        <Text as="label" size="2" weight="medium" className="recipe-form-page__field">
          Meal
          <Select.Root value={meal} onValueChange={(value) => onChange({ meal: value })} size="3">
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
          <Select.Root value={theme} onValueChange={(value) => onChange({ theme: value })} size="3">
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

      {/* The same three tabs as the recipe page. Inactive panels stay mounted
          (hidden by CSS) so a half-typed ingredient row or step survives a
          tab switch — Radix would otherwise unmount them. */}
      <Tabs.Root value={tab} onValueChange={(next) => onTabChange(next as RecipeTab)} className="recipe-form-page__tabs">
        <RecipeTabsList counts={{ ingredients: lines.length, instructions: values.steps.filter((s) => s.trim()).length }} />

        <Tabs.Content value="ingredients" forceMount className="recipe-form-page__tab">
          <LinesEditor
            key={linesEditorKey}
            lines={lines}
            onChange={(next) => onChange({ lines: next })}
            onPendingChange={onPendingLineChange}
          />
          {children}
        </Tabs.Content>

        <Tabs.Content value="instructions" forceMount className="recipe-form-page__tab">
          <StepsEditor steps={values.steps} onChange={(next) => onChange({ steps: next })} />
        </Tabs.Content>

        <Tabs.Content value="photos" forceMount className="recipe-form-page__tab">
          {photos}
        </Tabs.Content>
      </Tabs.Root>

      {error && (
        <Callout.Root color="red" variant="surface" size="1" role="alert">
          <Callout.Icon>
            <ExclamationTriangleIcon />
          </Callout.Icon>
          <Callout.Text>{error}</Callout.Text>
        </Callout.Root>
      )}
    </>
  );
}
