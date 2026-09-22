import { useRef, useState, type ClipboardEvent, type KeyboardEvent, type PointerEvent } from 'react';
import { Button, TextArea } from '@radix-ui/themes';
import { DragHandleDots2Icon, PlusIcon, TrashIcon } from '@radix-ui/react-icons';
import { RowActionButton } from '../../components/RowActionButton';
import { splitPastedSteps } from '../../lib/recipeSteps';
import { dropIndex, moveItem, shiftFor } from '../../lib/reorder';
import './StepsEditor.css';

interface StepsEditorProps {
  steps: string[];
  onChange: (steps: string[]) => void;
}

interface Drag {
  from: number;
  to: number;
  /** How far the pointer has moved since the grab, so the row follows it. */
  dy: number;
  height: number;
  /** Each row's vertical centre when the drag began — the drop target is read off these. */
  centres: number[];
}

/**
 * The method, one textarea per step, edited in place. Each row is
 * grab-handle (with the number) | text | delete. Reordering is a drag on the
 * handle — pointer events, so it works with a finger as well as a mouse
 * (HTML5 drag-and-drop doesn't on phones) — or ↑/↓ on the focused handle.
 * Like the ingredient lines, nothing is sent until Save. A whole method
 * pasted into one empty step is split into steps (one per line or
 * paragraph). Numbering is shown, not typed — "1." at the start of a step
 * is stripped on save.
 */
export function StepsEditor({ steps, onChange }: StepsEditorProps) {
  const areas = useRef<(HTMLTextAreaElement | null)[]>([]);
  const rows = useRef<(HTMLLIElement | null)[]>([]);
  const handles = useRef<(HTMLButtonElement | null)[]>([]);
  const [drag, setDragState] = useState<Drag | null>(null);
  // Mirrored in a ref so the drop handler reads the latest target without
  // doing its work inside a state updater (which StrictMode runs twice).
  const dragRef = useRef<Drag | null>(null);
  const grabY = useRef(0);
  function setDrag(next: Drag | null) {
    dragRef.current = next;
    setDragState(next);
  }

  function update(index: number, text: string) {
    onChange(steps.map((step, i) => (i === index ? text : step)));
  }

  function remove(index: number) {
    onChange(steps.filter((_, i) => i !== index));
  }

  function move(from: number, to: number) {
    if (from === to || to < 0 || to >= steps.length) return;
    onChange(moveItem(steps, from, to));
    // Keep focus with the step that moved, so a keyboard user can keep nudging it.
    requestAnimationFrame(() => handles.current[to]?.focus());
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

  function handleGrab(index: number, event: PointerEvent<HTMLButtonElement>) {
    if (event.button !== 0 && event.pointerType === 'mouse') return;
    event.currentTarget.setPointerCapture(event.pointerId);
    grabY.current = event.clientY;
    const rects = rows.current.map((row) => row?.getBoundingClientRect());
    setDrag({
      from: index,
      to: index,
      dy: 0,
      height: rects[index]?.height ?? 0,
      centres: rects.map((rect) => (rect ? rect.top + rect.height / 2 : 0)),
    });
  }

  function handleDragMove(event: PointerEvent<HTMLButtonElement>) {
    const current = dragRef.current;
    if (!current) return;
    const dy = event.clientY - grabY.current;
    setDrag({ ...current, dy, to: dropIndex(current.centres, current.from, current.centres[current.from] + dy) });
  }

  function handleDrop() {
    const current = dragRef.current;
    setDrag(null);
    if (current && current.to !== current.from) onChange(moveItem(steps, current.from, current.to));
  }

  function handleHandleKey(index: number, event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      move(index, index - 1);
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      move(index, index + 1);
    }
  }

  return (
    <div className="steps-editor">
      {steps.length > 0 && (
        <ol className={`steps-editor__list${drag ? ' steps-editor__list--dragging' : ''}`}>
          {steps.map((step, index) => {
            const dragging = drag?.from === index;
            const shift = drag ? shiftFor(index, drag.from, drag.to, drag.height) : 0;
            const transform = dragging ? `translateY(${drag!.dy}px)` : shift ? `translateY(${shift}px)` : undefined;
            return (
              <li
                key={index}
                ref={(element) => {
                  rows.current[index] = element;
                }}
                className={`steps-editor__row${dragging ? ' steps-editor__row--dragging' : ''}`}
                style={transform ? { transform } : undefined}
              >
                <button
                  ref={(element) => {
                    handles.current[index] = element;
                  }}
                  type="button"
                  className="steps-editor__handle"
                  aria-label={`Step ${index + 1}: drag to reorder, or use the arrow keys`}
                  onPointerDown={(event) => handleGrab(index, event)}
                  onPointerMove={drag ? handleDragMove : undefined}
                  onPointerUp={handleDrop}
                  onPointerCancel={handleDrop}
                  onKeyDown={(event) => handleHandleKey(index, event)}
                >
                  <span className="steps-editor__number" aria-hidden="true">
                    {index + 1}
                  </span>
                  <DragHandleDots2Icon aria-hidden="true" />
                </button>
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
                <RowActionButton quiet color="red" aria-label={`Remove step ${index + 1}`} onClick={() => remove(index)}>
                  <TrashIcon />
                </RowActionButton>
              </li>
            );
          })}
        </ol>
      )}
      <Button type="button" variant="soft" size="3" onClick={add} className="steps-editor__add">
        <PlusIcon /> {steps.length === 0 ? 'Add a step' : 'Add another step'}
      </Button>
    </div>
  );
}
