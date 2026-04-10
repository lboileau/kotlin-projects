import './LadderVoteProgress.css';

interface LadderVoteProgressProps {
  votesCast: number;
  totalVoters: number;
}

export function LadderVoteProgress({ votesCast, totalVoters }: LadderVoteProgressProps) {
  const pct = totalVoters === 0 ? 0 : Math.round((votesCast / totalVoters) * 100);

  return (
    <div className="ladder-vote-progress" aria-label={`${votesCast} of ${totalVoters} voted`}>
      <div className="ladder-vote-progress__label">
        <span className="ladder-vote-progress__count">
          {votesCast} of {totalVoters}
        </span>{' '}
        voted
      </div>
      <div className="ladder-vote-progress__bar-track" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div
          className="ladder-vote-progress__bar-fill"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
