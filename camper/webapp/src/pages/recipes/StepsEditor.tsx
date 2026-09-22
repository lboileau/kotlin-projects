import { useRef, type ClipboardEvent } from 'react';
import { Button, TextArea } from '@radix-ui/themes';
import { ChevronDownIcon, ChevronUpIcon, PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { RowActionButton } from '../../components/RowActionButton';
import { splitPastedSteps } from '../../lib/recipeSteps';
import './StepsEditor.css';

interface StepsEditorProps {
  steps: string[];
  onChange: (steps: string[]) => void;
}

/**
 * The method, one textarea per step, edited in place: add at the end, remove,
 * move up or down. Like the ingredient lines, nothing is sent until Save. A
 * whole method pasted into one empty step is split into steps (one per line
 * or paragraph) so a copied recipe doesn't land as a single block. Numbering
 * is shown, not typed — "1." at the start of a step is stripped on save.
 */
export function StepsEditor({ steps, onChange }: StepsEditorProps) {
  const areas = useRef<(HTMLTextAreaElement | null)[]>([]);

  function update(index: number, text: string) {
    onChange(steps.map((step, i) => (i === index ? text : step)));
  }

  function remove(index: number) {
    onChange(steps.filter((_, i) => i !== index));
  }

  function move(index: number, delta: -1 | 1) {
    const target = index + delta;
    if (target < 0 || target >= steps.length) return;
    const next = steps.slice();
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
    // Keep focus with the step that moved, so keyboard users can keep nudging it.
    requestAnimationFrame(() => areas.current[target]?.focus());
  }

  function add() {
    onChange([...steps, '']);
    requestAnimationFrame(() => areas.current[steps.length]?.focus());
  }

  function handlePaste(index: number, event: ClipboardEvent<HTMLTextAreaElement>) {
    if (steps[index].trim() !== '') return;
    const pasted = splitPastedSteps(event.clipboardData.getData('text'));
    if (pasted.length <= 1) return;
    event.preventDefault();
    onChange([...steps.slice(0, index), ...pasted, ...steps.slice(index + 1)]);
  }

  return (
    <div className="steps-editor">
      {steps.length > 0 && (
        <ol className="steps-editor__list">
          {steps.map((step, index) => (
            <li key={index} className="steps-editor__row">
              <span className="steps-editor__number" aria-hidden="true">
                {index + 1}
              </span>
              <TextArea
                ref={(element) => {
                  areas.current[index] = element;
                }}
                className="steps-editor__text"
                size="3"
                rows={2}
                value={step}
                placeholder={`Step ${index + 1}`}
                aria-label={`Step ${index + 1}`}
                autoCapitalize="sentences"
                enterKeyHint="next"
                onChange={(event) => update(index, event.target.value)}
                onPaste={(event) => handlePaste(index, event)}
              />
              <div className="steps-editor__actions">
                <RowActionButton quiet aria-label={`Move step ${index + 1} up`} disabled={index === 0} onClick={() => move(index, -1)}>
                  <ChevronUpIcon />
                </RowActionButton>
                <RowActionButton
                  quiet
                  aria-label={`Move step ${index + 1} down`}
                  disabled={index === steps.length - 1}
                  onClick={() => move(index, 1)}
                >
                  <ChevronDownIcon />
                </RowActionButton>
                <RowActionButton quiet color="red" aria-label={`Remove step ${index + 1}`} onClick={() => remove(index)}>
                  <TrashIcon />
                </RowActionButton>
              </div>
            </li>
          ))}
        </ol>
      )}
      <Button type="button" variant="soft" size="3" onClick={add} className="steps-editor__add">
        <PlusIcon /> {steps.length === 0 ? 'Add a step' : 'Add another step'}
      </Button>
    </div>
  );
}
