import { useEffect, useState } from 'react';
import './LadderOutcomeBanner.css';

export type OutcomePayload = {
  outcome: 'tie' | 'decided';
  winnerActivityId?: string | null;
  voteTotals?: Record<string, number> | null;
};

interface LadderOutcomeBannerProps {
  outcome: OutcomePayload | null;
  /** Name lookup — pass activities by id */
  activityNames: Record<string, string>;
  winnerActivityId?: string | null;
  /** When true, renders the ladder-completed winner banner (persistent) */
  completed?: boolean;
}

export function LadderOutcomeBanner({
  outcome,
  activityNames,
  winnerActivityId,
  completed = false,
}: LadderOutcomeBannerProps) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (outcome) {
      setVisible(true);
    }
  }, [outcome]);

  // Fade out after 3 seconds when transient (not completed)
  useEffect(() => {
    if (!visible || completed) return;
    const timer = setTimeout(() => setVisible(false), 3000);
    return () => clearTimeout(timer);
  }, [visible, completed]);

  if (completed && winnerActivityId) {
    return (
      <div className="ladder-outcome-banner ladder-outcome-banner--winner" role="status" aria-live="polite">
        <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true">
          <polygon
            points="12,2 15,9 22,9 16.5,14 18.5,22 12,18 5.5,22 7.5,14 2,9 9,9"
            fill="var(--ember)"
            stroke="var(--ember-bright)"
            strokeWidth="0.8"
          />
        </svg>
        <span className="ladder-outcome-banner__text">
          {activityNames[winnerActivityId] ?? 'Winner'} wins the ladder!
        </span>
      </div>
    );
  }

  if (!visible || !outcome) return null;

  if (outcome.outcome === 'tie') {
    return (
      <div className={`ladder-outcome-banner ladder-outcome-banner--tie ${!visible ? 'ladder-outcome-banner--fade-out' : ''}`} role="status" aria-live="polite">
        <span className="ladder-outcome-banner__text">Tie — reshuffling...</span>
      </div>
    );
  }

  const winnerName = outcome.winnerActivityId ? activityNames[outcome.winnerActivityId] ?? 'Unknown' : 'Unknown';
  const totals = outcome.voteTotals ?? {};
  const totalEntries = Object.entries(totals);

  return (
    <div className={`ladder-outcome-banner ladder-outcome-banner--decided ${!visible ? 'ladder-outcome-banner--fade-out' : ''}`} role="status" aria-live="polite">
      <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
        <polygon
          points="9,1.5 11,7 17,7 12.5,10.5 14,16.5 9,13 4,16.5 5.5,10.5 1,7 7,7"
          fill="var(--ember)"
        />
      </svg>
      <span className="ladder-outcome-banner__text">
        {winnerName} wins
        {totalEntries.length === 2 && (
          <>
            {' '}({totalEntries.map(([, v]) => v).join('–')})
          </>
        )}
      </span>
    </div>
  );
}
