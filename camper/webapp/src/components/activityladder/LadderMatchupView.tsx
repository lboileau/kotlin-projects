import type { LadderActivity, CurrentRoundView } from '../../api/client';
import { LadderActivityCard } from './LadderActivityCard';
import { LadderVoteProgress } from './LadderVoteProgress';
import './LadderMatchupView.css';

interface LadderMatchupViewProps {
  activityA: LadderActivity;
  activityB: LadderActivity;
  currentRound: CurrentRoundView;
  canVote: boolean;
  hasVoted: boolean;
  isSpectator: boolean;
  isFinalRound: boolean;
  isGrandFinalReset: boolean;
  onVote: (activityId: string) => void;
  /** When set, highlight that card as the round winner and dim the other. */
  celebratingWinnerId?: string | null;
  /** When true, show the centered "Tie — reshuffling..." overlay. */
  tieOverlay?: boolean;
}

export function LadderMatchupView({
  activityA,
  activityB,
  currentRound,
  canVote,
  hasVoted,
  isSpectator,
  isFinalRound,
  isGrandFinalReset,
  onVote,
  celebratingWinnerId,
  tieOverlay = false,
}: LadderMatchupViewProps) {
  const voteDisabled = !canVote || hasVoted || isSpectator;
  const isCelebrating = !!celebratingWinnerId;
  const resolutionA: 'winner' | 'loser' | null = isCelebrating
    ? celebratingWinnerId === activityA.id
      ? 'winner'
      : 'loser'
    : null;
  const resolutionB: 'winner' | 'loser' | null = isCelebrating
    ? celebratingWinnerId === activityB.id
      ? 'winner'
      : 'loser'
    : null;

  let roundLabel = `Round ${currentRound.roundNumber}`;
  if (isGrandFinalReset) roundLabel = 'Grand Final Reset';
  else if (isFinalRound) roundLabel = 'Grand Final';

  return (
    <div className="ladder-matchup-view">
      {(isFinalRound || isGrandFinalReset) && (
        <div className={`ladder-matchup-view__final-banner ${isGrandFinalReset ? 'ladder-matchup-view__final-banner--reset' : ''}`}>
          {isGrandFinalReset ? (
            <>
              <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M8,2 L10,6 L14.5,6 L11,9 L12.5,14 L8,11 L3.5,14 L5,9 L1.5,6 L6,6 Z" fill="var(--ember)" />
              </svg>
              Grand Final Reset
            </>
          ) : (
            <>
              <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
                <polygon points="8,1 10,6 15,6 11,9.5 12.5,15 8,12 3.5,15 5,9.5 1,6 6,6" fill="var(--ember)" />
              </svg>
              Grand Final
            </>
          )}
        </div>
      )}

      <div className="ladder-matchup-view__round-label">{roundLabel}</div>

      {isSpectator && (
        <div className="ladder-matchup-view__spectator-notice" role="note">
          You are watching — voting was locked at start
        </div>
      )}

      <div className={`ladder-matchup-view__cards ${isCelebrating ? 'ladder-matchup-view__cards--celebrating' : ''}`}>
        <LadderActivityCard
          activity={activityA}
          variant="matchup"
          onVote={() => onVote(activityA.id)}
          voteDisabled={voteDisabled || isCelebrating || tieOverlay}
          voteLabel={hasVoted ? 'Voted' : 'Vote'}
          matchupResolution={resolutionA}
        />

        <div className="ladder-matchup-view__vs" aria-hidden="true">VS</div>

        <LadderActivityCard
          activity={activityB}
          variant="matchup"
          onVote={() => onVote(activityB.id)}
          voteDisabled={voteDisabled || isCelebrating || tieOverlay}
          voteLabel={hasVoted ? 'Voted' : 'Vote'}
          matchupResolution={resolutionB}
        />

        {tieOverlay && (
          <div
            className="ladder-matchup-view__tie-overlay"
            role="status"
            aria-live="polite"
          >
            <svg
              className="ladder-matchup-view__tie-icon"
              width="40"
              height="40"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" fill="none" stroke="var(--lavender-light)" strokeWidth="1.6" />
              <line x1="6" y1="12" x2="18" y2="12" stroke="var(--lavender-light)" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
            <div className="ladder-matchup-view__tie-text">Tie — reshuffling...</div>
          </div>
        )}
      </div>

      <div className="ladder-matchup-view__progress">
        <LadderVoteProgress
          votesCast={currentRound.votesCast}
          totalVoters={currentRound.totalVoters}
        />
      </div>
    </div>
  );
}
