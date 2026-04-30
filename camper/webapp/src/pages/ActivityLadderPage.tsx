import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, type LadderDetail } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLadderUpdates } from '../hooks/useLadderUpdates';
import { AppHeader } from '../components/AppHeader';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { Button } from '../components/ui/Button';
import { ConfirmModal } from '../components/ui/ConfirmModal';
import { LadderPeoplePanel } from '../components/activityladder/LadderPeoplePanel';
import { LadderMatchupView } from '../components/activityladder/LadderMatchupView';
import { LadderOutcomeBanner } from '../components/activityladder/LadderOutcomeBanner';
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

  // Round-end celebration: while this is set, we highlight the winning card
  // with an animation. Crucially, during the celebration we DEFER any refetch
  // that would advance `ladder.currentRound` — so the displayed pair stays put
  // and the cards animate in place without swapping or flashing to the next
  // round. A pending refetch flag catches us up when the animation ends.
  const [celebratingWinnerId, setCelebratingWinnerId] = useState<string | null>(null);
  const celebratingRef = useRef(false);
  // Tie overlay: mirrors the winner celebration — pauses refetches and shows
  // a centered "Tie — reshuffling..." overlay on top of the matchup cards.
  const [tieActive, setTieActive] = useState(false);
  const tieActiveRef = useRef(false);
  const pendingRefetchRef = useRef(false);
  const celebrationTimerRef = useRef<number | null>(null);
  const tieTimerRef = useRef<number | null>(null);

  // Duration of the round-resolution overlays (winner + tie).
  const ROUND_RESOLUTION_MS = 2800;

  // Action states
  const [startLoading, setStartLoading] = useState(false);
  const [voteLoading, setVoteLoading] = useState(false);
  const [actionError, setActionError] = useState('');

  // Restart confirm modal
  const [showRestartConfirm, setShowRestartConfirm] = useState(false);

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

  // Clean up any pending overlay timers on unmount
  useEffect(() => {
    return () => {
      if (celebrationTimerRef.current !== null) {
        window.clearTimeout(celebrationTimerRef.current);
      }
      if (tieTimerRef.current !== null) {
        window.clearTimeout(tieTimerRef.current);
      }
    };
  }, []);

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
            loadLadder();
            break;
          case 'completed':
          case 'round-started':
            // Defer refetches that advance the round until any resolution
            // overlay (winner or tie) finishes. Otherwise ladder.currentRound
            // would jump to the next pair mid-animation.
            if (celebratingRef.current || tieActiveRef.current) {
              pendingRefetchRef.current = true;
            } else {
              loadLadder();
            }
            break;
          case 'vote-cast':
            // Increment the vote count locally — avoids a refetch race where
            // the final vote's loadLadder would land (with new-round data)
            // before round-resolved arrives.
            setLadder((prev) => {
              if (!prev || !prev.currentRound) return prev;
              return {
                ...prev,
                currentRound: {
                  ...prev.currentRound,
                  votesCast: prev.currentRound.votesCast + 1,
                },
              };
            });
            break;
          case 'round-resolved': {
            const outcome = msg.payload?.outcome as 'tie' | 'decided' | undefined;
            if (!outcome) break;
            const winnerId = (msg.payload?.winnerActivityId as string | null) ?? null;
            if (outcome === 'decided' && winnerId) {
              celebratingRef.current = true;
              setCelebratingWinnerId(winnerId);
              if (celebrationTimerRef.current !== null) {
                window.clearTimeout(celebrationTimerRef.current);
              }
              celebrationTimerRef.current = window.setTimeout(() => {
                celebratingRef.current = false;
                setCelebratingWinnerId(null);
                celebrationTimerRef.current = null;
                // Catch up on whatever round-started / completed event we
                // deferred during the animation.
                if (pendingRefetchRef.current) {
                  pendingRefetchRef.current = false;
                  loadLadder();
                }
              }, ROUND_RESOLUTION_MS);
            } else if (outcome === 'tie') {
              tieActiveRef.current = true;
              setTieActive(true);
              if (tieTimerRef.current !== null) {
                window.clearTimeout(tieTimerRef.current);
              }
              tieTimerRef.current = window.setTimeout(() => {
                tieActiveRef.current = false;
                setTieActive(false);
                tieTimerRef.current = null;
                if (pendingRefetchRef.current) {
                  pendingRefetchRef.current = false;
                  loadLadder();
                }
              }, ROUND_RESOLUTION_MS);
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

  // Current match activities. During a celebration, ladder.currentRound still
  // points to the just-resolved round (round-started/completed refetches are
  // deferred), so this lookup naturally keeps the pair frozen in place.
  const activityA = ladder.currentRound
    ? ladder.activities.find((a) => a.id === ladder.currentRound!.activityAId) ?? null
    : null;
  const activityB = ladder.currentRound
    ? ladder.activities.find((a) => a.id === ladder.currentRound!.activityBId) ?? null
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
    setActionError('');
    try {
      await api.ladders.restart(ladderId);
      await loadLadder();
      setCelebratingWinnerId(null);
      setTieActive(false);
      celebratingRef.current = false;
      tieActiveRef.current = false;
      pendingRefetchRef.current = false;
      if (celebrationTimerRef.current !== null) {
        window.clearTimeout(celebrationTimerRef.current);
        celebrationTimerRef.current = null;
      }
      if (tieTimerRef.current !== null) {
        window.clearTimeout(tieTimerRef.current);
        tieTimerRef.current = null;
      }
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Failed to restart ladder.');
    }
  };

  const handleVote = async (activityId: string) => {
    if (!ladderId) return;
    setVoteLoading(true);
    setActionError('');
    try {
      await api.ladders.vote(ladderId, { votedForActivityId: activityId });
      // Optimistically mark self as having voted. We intentionally don't
      // refetch here — that would race with round-resolved and swap the
      // displayed pair before the celebration animation runs. The vote-cast
      // WS event will bump votesCast, and round-started (deferred until after
      // the celebration) will refresh the rest.
      setLadder((prev) => {
        if (!prev || !prev.currentRound) return prev;
        if (prev.currentRound.votedUserIds.includes(currentUserId)) return prev;
        return {
          ...prev,
          currentRound: {
            ...prev.currentRound,
            votedUserIds: [...prev.currentRound.votedUserIds, currentUserId],
          },
        };
      });
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
                <LadderMatchupView
                  activityA={activityA}
                  activityB={activityB}
                  currentRound={ladder.currentRound}
                  canVote={!celebratingWinnerId && !tieActive && isVoter && !hasVotedThisRound && !voteLoading}
                  hasVoted={hasVotedThisRound}
                  isSpectator={isSpectator}
                  isFinalRound={ladder.isFinalRound}
                  isGrandFinalReset={ladder.isGrandFinalReset}
                  onVote={handleVote}
                  celebratingWinnerId={celebratingWinnerId}
                  tieOverlay={tieActive}
                />

                {isCreator && !celebratingWinnerId && !tieActive && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setShowRestartConfirm(true)}
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
                    onClick={() => setShowRestartConfirm(true)}
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

      {/* Restart confirmation — W29 will finalize copy */}
      <ConfirmModal
        isOpen={showRestartConfirm}
        title="Restart ladder?"
        message="All votes will be reset and a new tournament will begin."
        confirmLabel="Restart"
        tone="danger"
        onConfirm={async () => {
          await handleRestart();
          setShowRestartConfirm(false);
        }}
        onCancel={() => setShowRestartConfirm(false)}
      />
    </div>
  );
}
