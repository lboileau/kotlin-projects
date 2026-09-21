import type { DictationChip } from '../../lib/dictationSelection';
import './DictationChips.css';

interface DictationChipsProps {
  chips: DictationChip[];
  onToggle: (key: string) => void;
}

/**
 * The parsed items of a dictation, each a toggle: tap to leave one out, tap
 * again to put it back. Toggling never edits the text the chips came from —
 * see webapp/CLAUDE.md's "never mutate a dictating field" rule.
 */
export function DictationChips({ chips, onToggle }: DictationChipsProps) {
  return (
    <div className="dictation-chips">
      {chips.map((chip) => (
        <button
          key={chip.key}
          type="button"
          className={`dictation-chips__chip${chip.excluded ? ' dictation-chips__chip--excluded' : ''}`}
          aria-label={chip.excluded ? `Add ${chip.text} back` : `Remove ${chip.text}`}
          aria-pressed={chip.excluded}
          // Keeps focus (and the keyboard, and a running dictation) in the text
          // field: preventDefault on pointerdown stops the blur, same as
          // IngredientPicker's options.
          onPointerDown={(event) => event.preventDefault()}
          onClick={() => onToggle(chip.key)}
        >
          {chip.text}
        </button>
      ))}
    </div>
  );
}
