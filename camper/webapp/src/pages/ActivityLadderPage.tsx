import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, type LadderDetail } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLadderUpdates } from '../hooks/useLadderUpdates';
import { AppHeader } from '../components/AppHeader';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { Button } from '../components/ui/Button';
import { LadderPeoplePanel } from '../components/activityladder/LadderPeoplePanel';
import { LadderMatchupView } from '../components/activityladder/LadderMatchupView';
import { LadderOutcomeBanner, type OutcomePayload } from '../components/activityladder/LadderOutcomeBanner';
import { LadderDraftActivityList } from '../components/activityladder/LadderDraftActivityList';
import { LadderActivityCard } from '../components/activityladder/LadderActivityCard';
import './ActivityLadderPage.css';

export function ActivityLadderPage() {
  const { ladderId } = useParams<{ ladderId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [ladder, setLadder] = useState<LadderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Live presence state (sourced from presence-changed WS events)
  const [presentUserIds, setPresentUserIds] = useState<Set<string>>(new Set());

  // Last round outcome (for transient banner)
  const [lastResolvedOutcome, setLastResolvedOutcome] = useState<OutcomePayload | null>(null);

  // Action states
  const [startLoading, setStartLoading] = useState(false);
  const [restartLoading, setRestartLoading] = useState(false);
  const [voteLoading, setVoteLoading] = useState(false);
  const [actionError, setActionError] = useState('');

  const currentUserId = user?.id ?? '';

  const loadLadder = useCallback(async () => {
    if (!ladderId) return;
    try {
      const res = await api.ladders.get(ladderId);
      setLadder(res);
    } catch {
      setError('Failed to load ladder.');
    } finally {
      setLoading(false);
    }
  }, [ladderId]);

  useEffect(() => {
    loadLadder();
  }, [loadLadder]);

  // WebSocket subscription
  useLadderUpdates(
    ladderId,
    currentUserId || undefined,
    useCallback(
      (msg) => {
        switch (msg.action) {
          case 'presence-changed': {
            const ids = (msg.payload?.presentUserIds as string[] | undefined) ?? [];
            setPresentUserIds(new Set(ids));
            break;
          }
          case 'activity-added':
          case 'activity-removed':
          case 'started':
          case 'restarted':
          case 'completed':
            loadLadder();
            break;
          case 'round-started':
            loadLadder();
            setLastResolvedOutcome(null);
            break;
          case 'vote-cast':
            // Refetch to get updated vote counts
            loadLadder();
            break;
          case 'round-resolved': {
            const outcome = msg.payload?.outcome as 'tie' | 'decided' | undefined;
            if (outcome) {
              setLastResolvedOutcome({
                outcome,
                winnerActivityId: (msg.payload?.winnerActivityId as string | null) ?? null,
                voteTotals: (msg.payload?.voteTotals as Record<string, number> | null) ?? null,
              });
            }
            break;
          }
        }
      },
      [loadLadder],
    ),
  );

  if (loading) {
    return (
      <div className="ladder-page">
        <ParallaxBackground variant="dusk" />
        <div className="ladder-page-content">
          <AppHeader pageTitle="Activity Ladder" />
          <main className="ladder-page-main">
            <p className="ladder-page-status">Loading...</p>
          </main>
        </div>
      </div>
    );
  }

  if (error || !ladder) {
    return (
      <div className="ladder-page">
        <ParallaxBackground variant="dusk" />
        <div className="ladder-page-content">
          <AppHeader pageTitle="Activity Ladder" />
          <main className="ladder-page-main">
            <p className="ladder-page-status ladder-page-status--error">{error || 'Ladder not found.'}</p>
            <Button variant="secondary" onClick={() => navigate('/activities')}>
              Back to Activities
            </Button>
          </main>
        </div>
      </div>
    );
  }

  const isCreator = ladder.creatorId === currentUserId;
  const isVoter = ladder.participants.some((p) => p.userId === currentUserId);
  const isSpectator = ladder.status === 'ACTIVE' && !isVoter;
  const hasVotedThisRound =
    ladder.status === 'ACTIVE' && (ladder.currentRound?.votedUserIds.includes(currentUserId) ?? false);

  // Activity lookup map for banners
  const activityNames: Record<string, string> = {};
  ladder.activities.forEach((a) => {
    activityNames[a.id] = a.name;
  });

  // Current match activities
  const activityA = ladder.currentRound
    ? ladder.activities.find((a) => a.id === ladder.currentRound!.activityAId)
    : null;
  const activityB = ladder.currentRound
    ? ladder.activities.find((a) => a.id === ladder.currentRound!.activityBId)
    : null;

  const handleStart = async () => {
    if (!ladderId) return;
    setStartLoading(true);
    setActionError('');
    try {
      await api.ladders.start(ladderId);
      await loadLadder();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Failed to start ladder.');
    } finally {
      setStartLoading(false);
    }
  };

  const handleRestart = async () => {
    if (!ladderId) return;
    if (!confirm('Restart the ladder? This will reset all votes and participants.')) return;
    setRestartLoading(true);
    setActionError('');
    try {
      await api.ladders.restart(ladderId);
      await loadLadder();
      setLastResolvedOutcome(null);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Failed to restart ladder.');
    } finally {
      setRestartLoading(false);
    }
  };

  const handleVote = async (activityId: string) => {
    if (!ladderId) return;
    setVoteLoading(true);
    setActionError('');
    try {
      await api.ladders.vote(ladderId, { votedForActivityId: activityId });
      await loadLadder();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Failed to cast vote.');
    } finally {
      setVoteLoading(false);
    }
  };

  const handleAddActivity = async (data: {
    name: string;
    imageUrl: string;
    distanceMinutes: number;
    costPerPerson: number;
  }) => {
    if (!ladderId) return;
    await api.ladders.addActivity(ladderId, data);
    await loadLadder();
  };

  const handleRemoveActivity = async (activityId: string) => {
    if (!ladderId) return;
    await api.ladders.removeActivity(ladderId, activityId);
    await loadLadder();
  };

  const headerActions = (
    <Button variant="ghost" size="sm" onClick={() => navigate('/activities')}>
      ← All Ladders
    </Button>
  );

  return (
    <div className="ladder-page">
      <ParallaxBackground variant="dusk" />
      <div className="ladder-page-content">
        <AppHeader pageTitle={ladder.title} actions={headerActions} />

        <main className="ladder-page-main">
          {/* Status badge row */}
          <div className="ladder-page-status-row">
            <span className={`ladder-page-status-badge ladder-page-status-badge--${ladder.status.toLowerCase()}`}>
              {ladder.status}
            </span>
            {ladder.isFinalRound && !ladder.isGrandFinalReset && (
              <span className="ladder-page-final-label">Grand Final</span>
            )}
            {ladder.isGrandFinalReset && (
              <span className="ladder-page-final-label ladder-page-final-label--reset">Grand Final Reset</span>
            )}
          </div>

          {actionError && (
            <p className="ladder-page-action-error" role="alert">{actionError}</p>
          )}

          <div className="ladder-page-body">
            {/* ─── DRAFT ─── */}
            {ladder.status === 'DRAFT' && (
              <div className="ladder-page-draft">
                <LadderDraftActivityList
                  activities={ladder.activities}
                  readonly={!isCreator}
                  onAdd={isCreator ? handleAddActivity : undefined}
                  onRemove={isCreator ? handleRemoveActivity : undefined}
                />

                {isCreator && (
                  <div className="ladder-page-draft-actions">
                    {ladder.activities.length < 2 && (
                      <p className="ladder-page-draft-hint">Add at least 2 activities to start.</p>
                    )}
                    <Button
                      variant="primary"
                      size="lg"
                      onClick={handleStart}
                      loading={startLoading}
                      disabled={ladder.activities.length < 2}
                    >
                      Start Ladder
                    </Button>
                  </div>
                )}

                {!isCreator && (
                  <p className="ladder-page-waiting-msg">
                    Waiting for the creator to start the ladder...
                  </p>
                )}
              </div>
            )}

            {/* ─── ACTIVE ─── */}
            {ladder.status === 'ACTIVE' && activityA && activityB && ladder.currentRound && (
              <div className="ladder-page-active">
                {lastResolvedOutcome && (
                  <LadderOutcomeBanner
                    outcome={lastResolvedOutcome}
                    activityNames={activityNames}
                  />
                )}

                <LadderMatchupView
                  activityA={activityA}
                  activityB={activityB}
                  currentRound={ladder.currentRound}
                  canVote={isVoter && !hasVotedThisRound && !voteLoading}
                  hasVoted={hasVotedThisRound}
                  isSpectator={isSpectator}
                  isFinalRound={ladder.isFinalRound}
                  isGrandFinalReset={ladder.isGrandFinalReset}
                  onVote={handleVote}
                />

                {isCreator && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={handleRestart}
                    loading={restartLoading}
                    className="ladder-page-restart-btn"
                  >
                    Restart Ladder
                  </Button>
                )}
              </div>
            )}

            {/* ─── COMPLETED ─── */}
            {ladder.status === 'COMPLETED' && (
              <div className="ladder-page-completed">
                <LadderOutcomeBanner
                  outcome={null}
                  activityNames={activityNames}
                  winnerActivityId={ladder.winnerActivityId}
                  completed
                />

                <div className="ladder-page-completed-activities">
                  {ladder.activities
                    .slice()
                    .sort((a, b) => {
                      if (a.id === ladder.winnerActivityId) return -1;
                      if (b.id === ladder.winnerActivityId) return 1;
                      return a.losses - b.losses;
                    })
                    .map((activity) => (
                      <LadderActivityCard
                        key={activity.id}
                        activity={activity}
                        variant={
                          activity.id === ladder.winnerActivityId
                            ? 'winner'
                            : activity.bracket === 'ELIMINATED'
                            ? 'eliminated'
                            : 'loser'
                        }
                        isWinner={activity.id === ladder.winnerActivityId}
                      />
                    ))}
                </div>

                {isCreator && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={handleRestart}
                    loading={restartLoading}
                    className="ladder-page-restart-btn"
                  >
                    Restart Ladder
                  </Button>
                )}
              </div>
            )}
          </div>

          {/* People panel — always visible on the right */}
          <aside className="ladder-page-panel">
            <LadderPeoplePanel
              ladder={ladder}
              presentUserIds={presentUserIds}
              currentUserId={currentUserId}
            />
          </aside>
        </main>
      </div>
    </div>
  );
}
