import type { LadderActivity } from '../../api/client';
import { Button } from '../ui/Button';
import './LadderActivityCard.css';

type CardVariant = 'display' | 'matchup' | 'loser' | 'eliminated' | 'winner';

interface LadderActivityCardProps {
  activity: LadderActivity;
  variant?: CardVariant;
  onVote?: () => void;
  voteDisabled?: boolean;
  voteLabel?: string;
  isWinner?: boolean;
  /**
   * Transient round-end state used to celebrate the matchup outcome:
   * 'winner' animates the card with a pulse/glow and shows a WINNER overlay;
   * 'loser' dims the card. Only applies to the `matchup` variant.
   */
  matchupResolution?: 'winner' | 'loser' | null;
}

export function LadderActivityCard({
  activity,
  variant = 'display',
  onVote,
  voteDisabled = false,
  voteLabel = 'Vote',
  isWinner = false,
  matchupResolution = null,
}: LadderActivityCardProps) {
  const cardClass = [
    'ladder-activity-card',
    `ladder-activity-card--${variant}`,
    isWinner ? 'ladder-activity-card--winner-highlight' : '',
    matchupResolution === 'winner' ? 'ladder-activity-card--celebrate-winner' : '',
    matchupResolution === 'loser' ? 'ladder-activity-card--celebrate-loser' : '',
  ]
    .filter(Boolean)
    .join(' ');

  const costDisplay =
    activity.costPerPerson === 0
      ? 'Free'
      : `$${Number(activity.costPerPerson).toFixed(2)}/person`;

  const distDisplay =
    activity.distanceMinutes === 0
      ? 'On-site'
      : activity.distanceMinutes < 60
      ? `${activity.distanceMinutes} min away`
      : `${Math.round(activity.distanceMinutes / 60 * 10) / 10} hr away`;

  return (
    <div className={cardClass}>
      {matchupResolution === 'winner' && (
        <div className="ladder-activity-card__celebrate-overlay" aria-hidden="true">
          <svg className="ladder-activity-card__celebrate-star" width="48" height="48" viewBox="0 0 24 24">
            <polygon
              points="12,2 15,9 22,9 16.5,14 18.5,22 12,18 5.5,22 7.5,14 2,9 9,9"
              fill="var(--ember)"
              stroke="var(--ember-bright)"
              strokeWidth="0.8"
            />
          </svg>
          <div className="ladder-activity-card__celebrate-text">Winner!</div>
        </div>
      )}
      {variant === 'winner' && (
        <div className="ladder-activity-card__badge ladder-activity-card__badge--winner">
          <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
            <polygon
              points="8,1 10,6 15,6 11,9.5 12.5,15 8,12 3.5,15 5,9.5 1,6 6,6"
              fill="var(--ember)"
              stroke="var(--ember-bright)"
              strokeWidth="0.5"
            />
          </svg>
          Winner
        </div>
      )}
      {variant === 'eliminated' && (
        <div className="ladder-activity-card__badge ladder-activity-card__badge--eliminated">
          Eliminated
        </div>
      )}

      <div className="ladder-activity-card__image-wrap">
        <img
          src={activity.imageUrl}
          alt={activity.name}
          className="ladder-activity-card__image"
          onError={(e) => {
            (e.target as HTMLImageElement).style.display = 'none';
          }}
        />
      </div>

      <div className="ladder-activity-card__body">
        <h3 className="ladder-activity-card__name">{activity.name}</h3>
        <div className="ladder-activity-card__meta">
          <span className="ladder-activity-card__meta-item">
            <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
              <circle cx="6" cy="6" r="5" fill="none" stroke="currentColor" strokeWidth="1.2" />
              <line x1="6" y1="3" x2="6" y2="6" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
              <line x1="6" y1="6" x2="8.5" y2="7.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
            </svg>
            {distDisplay}
          </span>
          <span className="ladder-activity-card__meta-item">
            <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
              <circle cx="6" cy="6" r="5" fill="none" stroke="currentColor" strokeWidth="1.2" />
              <text x="6" y="9" textAnchor="middle" fontSize="7" fill="currentColor" fontFamily="sans-serif">$</text>
            </svg>
            {costDisplay}
          </span>
        </div>

        {activity.losses > 0 && variant !== 'winner' && (
          <div className="ladder-activity-card__losses">
            {Array.from({ length: 2 }).map((_, i) => (
              <span
                key={i}
                className={`ladder-activity-card__loss-pip ${i < activity.losses ? 'ladder-activity-card__loss-pip--filled' : ''}`}
                aria-hidden="true"
              />
            ))}
            <span className="ladder-activity-card__losses-label">
              {activity.losses === 1 ? '1 loss' : '2 losses'}
            </span>
          </div>
        )}

        {variant === 'matchup' && onVote && (
          <Button
            variant="primary"
            size="md"
            onClick={onVote}
            disabled={voteDisabled}
            className="ladder-activity-card__vote-btn"
            aria-label={`Vote for ${activity.name}`}
          >
            {voteLabel}
          </Button>
        )}
      </div>
    </div>
  );
}
