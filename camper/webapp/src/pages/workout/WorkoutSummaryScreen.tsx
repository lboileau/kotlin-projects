import { Navigate, useNavigate } from 'react-router-dom';
import { Button, Table } from '@radix-ui/themes';
import { toast } from '../../lib/toastStore';
import { engine, fmtDur } from './engine/workoutEngine';
import { useEngine } from './useEngine';
import { WorkoutHeader } from './WorkoutChrome';

/** Per-exercise counts and times, the total and the duration. Nothing is stored: copy it to keep it. */
export function WorkoutSummaryScreen() {
  const { summary, summaryText } = useEngine();
  const navigate = useNavigate();
  if (!summary) return <Navigate to="/workout" replace />;

  const n = summary.exercises.length,
    done = summary.exercises.filter((e) => e.reason).length;
  const canShare = typeof navigator.share === 'function';

  async function copy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(summaryText);
      toast.info('Copied');
    } catch {
      window.prompt('Copy this:', summaryText);
    }
  }
  function finish(): void {
    engine.clearRun();
    navigate('/workout', { replace: true });
  }

  return (
    <section className="workout-screen">
      <WorkoutHeader title={done === n ? 'Workout complete' : `Workout ended · ${done} of ${n}`} />
      <div className="workout-scroll workout-summary">
        <div className="workout-totals">
          <div>
            <b>{summary.total}</b>reps
          </div>
          <div>
            <b>{fmtDur(summary.duration)}</b>duration
          </div>
          <div>
            <b>
              {done}/{n}
            </b>
            exercises
          </div>
        </div>
        <Table.Root size="2" variant="ghost">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Exercise</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell align="right">Reps</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell align="right">Time</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {summary.exercises.map((e, i) => (
              <Table.Row key={i}>
                <Table.RowHeaderCell>
                  {i + 1}. {e.name}
                </Table.RowHeaderCell>
                <Table.Cell align="right" className={e.reason && e.counted < e.target ? 'workout-summary__short' : undefined}>
                  {e.reason ? `${e.counted} / ${e.target}` : '—'}
                </Table.Cell>
                <Table.Cell align="right">{e.reason ? fmtDur(e.duration) : '—'}</Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
      </div>
      <footer className="workout-actions">
        <div className="workout-row workout-row--even">
          <Button variant="soft" size="3" onClick={() => void copy()}>
            Copy as text
          </Button>
          {canShare && (
            <Button variant="soft" size="3" onClick={() => navigator.share({ title: 'Workout', text: summaryText }).catch(() => {})}>
              Share
            </Button>
          )}
        </div>
        <Button size="4" className="workout-big" onClick={finish}>
          Done
        </Button>
      </footer>
    </section>
  );
}
