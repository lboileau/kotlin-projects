import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertDialog, Button, Flex, IconButton, Select, Switch, Text, TextField } from '@radix-ui/themes';
import { Cross2Icon, DragHandleDots2Icon, PlusIcon, ResetIcon, SpeakerLoudIcon, SpeakerOffIcon } from '@radix-ui/react-icons';
import { Stepper } from '../../components/Stepper';
import { dropIndex, moveItem, shiftFor } from '../../lib/reorder';
import { engine, type VoiceMode } from './engine/workoutEngine';
import { useEngine } from './useEngine';
import { WorkoutHeader } from './WorkoutChrome';

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
 * Build the workout: exercises in order with a rep target each (drag the handle to reorder, the
 * same pointer-event drag as the recipe steps editor, or ↑/↓ on the focused handle), rest and
 * rounds, and the sound options behind the speaker button in the header. Remembered between visits.
 */
export function WorkoutSetupScreen() {
  const { settings } = useEngine();
  const navigate = useNavigate();
  const plan = settings.current;
  const exercises = plan.exercises;
  const listRef = useRef<HTMLOListElement>(null);
  const rows = useRef<(HTMLLIElement | null)[]>([]);
  const handles = useRef<(HTMLButtonElement | null)[]>([]);
  const focusIndex = useRef<number | null>(null);
  const [soundOpen, setSoundOpen] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);
  const [drag, setDragState] = useState<Drag | null>(null);
  // Mirrored in a ref so the drop handler reads the latest target without doing its work inside
  // a state updater (which StrictMode runs twice).
  const dragRef = useRef<Drag | null>(null);
  const grabY = useRef(0);
  function setDrag(next: Drag | null) {
    dragRef.current = next;
    setDragState(next);
  }

  useEffect(() => {
    if (focusIndex.current == null) return;
    const input = listRef.current?.querySelector<HTMLInputElement>(`input[data-name="${focusIndex.current}"]`);
    focusIndex.current = null;
    input?.focus();
  });

  function reorder(from: number, to: number) {
    if (from === to || to < 0 || to >= exercises.length) return;
    engine.setExercises(moveItem(exercises, from, to));
    requestAnimationFrame(() => handles.current[to]?.focus()); // keep focus with the row that moved
  }
  function handleGrab(index: number, event: PointerEvent<HTMLButtonElement>) {
    if (event.button !== 0 && event.pointerType === 'mouse') return;
    event.currentTarget.setPointerCapture(event.pointerId);
    grabY.current = event.clientY;
    const rects = rows.current.map((row) => row?.getBoundingClientRect());
    setDrag({ from: index, to: index, dy: 0, height: rects[index]?.height ?? 0, centres: rects.map((r) => (r ? r.top + r.height / 2 : 0)) });
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
    if (current && current.to !== current.from) engine.setExercises(moveItem(exercises, current.from, current.to));
  }
  function handleHandleKey(index: number, event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      reorder(index, index - 1);
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      reorder(index, index + 1);
    }
  }

  const silent = settings.voiceMode === 'off' && !settings.beep;

  return (
    <section className="workout-screen">
      <WorkoutHeader
        title="Workout"
        trailing={
          <IconButton variant="soft" size="3" aria-label="Sound options" aria-expanded={soundOpen} onClick={() => setSoundOpen((open) => !open)}>
            {silent ? <SpeakerOffIcon /> : <SpeakerLoudIcon />}
          </IconButton>
        }
      />
      <div className="workout-scroll">
        {soundOpen && (
          <div className="workout-card workout-sound">
            <div className="workout-field">
              <Text size="3">Voice</Text>
              <Select.Root value={settings.voiceMode} onValueChange={(v) => engine.setVoiceMode(v as VoiceMode)} size="3">
                <Select.Trigger className="workout-field__select" aria-label="Voice" />
                <Select.Content position="popper">
                  <Select.Item value="checkpoints">Count checkpoints</Select.Item>
                  <Select.Item value="announce">Announcements only</Select.Item>
                  <Select.Item value="off">Off</Select.Item>
                </Select.Content>
              </Select.Root>
            </div>
            <label className="workout-field workout-field--start">
              <Switch size="3" checked={settings.beep} onCheckedChange={(v) => engine.setBeep(v)} />
              <Text size="3">Beep on each rep</Text>
            </label>
          </div>
        )}
        <div className="workout-h2-row">
          <h2 className="workout-h2">Exercises, in order</h2>
          <IconButton variant="soft" size="3" className="workout-reset" aria-label="Reset to the default workout" onClick={() => setConfirmReset(true)}>
            <ResetIcon />
          </IconButton>
        </div>
        <ol ref={listRef} className={`workout-exlist${drag ? ' workout-exlist--dragging' : ''}`}>
          {exercises.map((e, i) => {
            const dragging = drag?.from === i;
            const shift = drag ? shiftFor(i, drag.from, drag.to, drag.height) : 0;
            const transform = dragging ? `translateY(${drag!.dy}px)` : shift ? `translateY(${shift}px)` : undefined;
            return (
              <li
                key={i}
                ref={(element) => {
                  rows.current[i] = element;
                }}
                className={`workout-ex${dragging ? ' workout-ex--dragging' : ''}`}
                style={transform ? { transform } : undefined}
              >
                <button
                  ref={(element) => {
                    handles.current[i] = element;
                  }}
                  type="button"
                  className="workout-ex__handle"
                  aria-label={`Exercise ${i + 1}: drag to reorder, or use the arrow keys`}
                  onPointerDown={(event) => handleGrab(i, event)}
                  onPointerMove={drag ? handleDragMove : undefined}
                  onPointerUp={handleDrop}
                  onPointerCancel={handleDrop}
                  onKeyDown={(event) => handleHandleKey(i, event)}
                >
                  <span className="workout-ex__num" aria-hidden="true">
                    {i + 1}
                  </span>
                  <DragHandleDots2Icon aria-hidden="true" />
                </button>
                <TextField.Root
                  className="workout-ex__name"
                  size="3"
                  value={e.name}
                  placeholder={`Exercise ${i + 1}`}
                  data-name={i}
                  autoCapitalize="words"
                  autoComplete="off"
                  autoCorrect="off"
                  spellCheck={false}
                  enterKeyHint="next"
                  aria-label={`Exercise ${i + 1} name`}
                  onChange={(event) => engine.updateExercise(i, { name: event.target.value })}
                />
                <TextField.Root
                  className="workout-ex__reps"
                  size="3"
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={999}
                  value={String(e.reps)}
                  enterKeyHint="done"
                  aria-label={`Exercise ${i + 1} reps`}
                  onChange={(event) => engine.updateExercise(i, { reps: event.target.value === '' ? 0 : +event.target.value })}
                >
                  <TextField.Slot side="right" className="workout-ex__unit">
                    reps
                  </TextField.Slot>
                </TextField.Root>
                <IconButton variant="ghost" color="gray" size="3" className="workout-ex__remove" aria-label={`Remove exercise ${i + 1}`} disabled={exercises.length === 1} onClick={() => engine.removeExercise(i)}>
                  <Cross2Icon />
                </IconButton>
              </li>
            );
          })}
        </ol>
        <Button
          variant="soft"
          size="3"
          className="workout-wide"
          onClick={() => {
            focusIndex.current = engine.addExercise();
          }}
        >
          <PlusIcon /> Add exercise
        </Button>

        <div className="workout-card">
          <label className="workout-field">
            <Text size="3">Rest between exercises</Text>
            <TextField.Root
              className="workout-field__num"
              size="3"
              type="number"
              inputMode="numeric"
              min={0}
              max={600}
              value={String(plan.rest)}
              enterKeyHint="done"
              aria-label="Rest between exercises, seconds"
              onChange={(event) => engine.setRest(event.target.value === '' ? 0 : +event.target.value)}
            >
              <TextField.Slot side="right" className="workout-ex__unit">
                sec
              </TextField.Slot>
            </TextField.Root>
          </label>
          <div className="workout-field">
            <Text size="3">Rounds</Text>
            <Stepper value={plan.rounds} onChange={(v) => engine.setRounds(v)} min={1} max={20} ariaLabel="Rounds" />
          </div>
        </div>
      </div>
      <footer className="workout-actions">
        <Button size="4" className="workout-big" onClick={() => navigate('/workout/camera')}>
          Start workout
        </Button>
      </footer>

      <AlertDialog.Root open={confirmReset} onOpenChange={setConfirmReset}>
        <AlertDialog.Content maxWidth="360px">
          <AlertDialog.Title>Reset the workout?</AlertDialog.Title>
          <AlertDialog.Description size="2">Back to the default exercises, rest and rounds. Sound settings stay.</AlertDialog.Description>
          <Flex gap="3" mt="4" justify="end">
            <AlertDialog.Cancel>
              <Button variant="soft" color="gray">
                Keep mine
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action>
              <Button onClick={() => engine.resetPlan()}>Reset</Button>
            </AlertDialog.Action>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Root>
    </section>
  );
}
