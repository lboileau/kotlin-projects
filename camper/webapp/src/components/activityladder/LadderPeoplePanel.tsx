import { useEffect, useState } from 'react';
import type { LadderDetail, LadderParticipant, AvatarResponse } from '../../api/client';
import { api } from '../../api/client';
import { AvatarHead } from '../AvatarHead';
import './LadderPeoplePanel.css';

interface UserInfo {
  userId: string;
  username: string | null;
  avatar: AvatarResponse | null;
}

interface LadderPeoplePanelProps {
  ladder: LadderDetail;
  presentUserIds: Set<string>;
  currentUserId: string;
}

function useUserInfoCache(userIds: string[], participants: LadderParticipant[]) {
  const [cache, setCache] = useState<Record<string, UserInfo>>({});

  useEffect(() => {
    // Build initial cache from known participants (they already have username + avatarSeed)
    const initial: Record<string, UserInfo> = {};
    participants.forEach((p) => {
      if (!initial[p.userId]) {
        initial[p.userId] = { userId: p.userId, username: p.username, avatar: null };
      }
    });
    setCache((prev) => ({ ...initial, ...prev }));
  }, [participants]);

  useEffect(() => {
    // Fetch user info for unknown present users, and for participants whose avatar hasn't resolved yet
    const unknownIds = userIds.filter((id) => !cache[id] || cache[id].avatar === null);
    if (unknownIds.length === 0) return;

    // We fetch via the avatar endpoint to get avatar data
    Promise.allSettled(
      unknownIds.map(async (id) => {
        try {
          const avatar = await api.getAvatar(id);
          return { userId: id, avatar };
        } catch {
          return { userId: id, avatar: null };
        }
      }),
    ).then((results) => {
      const newEntries: Record<string, UserInfo> = {};
      results.forEach((r) => {
        if (r.status === 'fulfilled') {
          const { userId, avatar } = r.value;
          newEntries[userId] = {
            userId,
            username: null, // unknown for non-participants unless we fetch full user
            avatar,
          };
        }
      });
      setCache((prev) => ({ ...prev, ...newEntries }));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userIds.join(',')]);

  return cache;
}

interface PersonRowProps {
  userId: string;
  userInfo: UserInfo | undefined;
  isOnline: boolean;
  hasVoted?: boolean;
  isCurrentUser?: boolean;
  showVotedIndicator?: boolean;
}

function PersonRow({
  userId,
  userInfo,
  isOnline,
  hasVoted,
  isCurrentUser,
  showVotedIndicator = false,
}: PersonRowProps) {
  const displayName = userInfo?.username ?? (isCurrentUser ? 'You' : `User ${userId.slice(0, 6)}`);

  return (
    <div className={`ladder-people-panel__person ${!isOnline ? 'ladder-people-panel__person--offline' : ''}`}>
      <div className="ladder-people-panel__avatar">
        <AvatarHead avatar={userInfo?.avatar ?? null} size={28} />
        <span
          className={`ladder-people-panel__online-dot ${isOnline ? 'ladder-people-panel__online-dot--on' : 'ladder-people-panel__online-dot--off'}`}
          aria-label={isOnline ? 'Online' : 'Offline'}
        />
      </div>
      <span className="ladder-people-panel__name">
        {displayName}
        {isCurrentUser && <span className="ladder-people-panel__you-tag"> (you)</span>}
      </span>
      {showVotedIndicator && (
        <span
          className={`ladder-people-panel__vote-indicator ${hasVoted ? 'ladder-people-panel__vote-indicator--voted' : ''}`}
          aria-label={hasVoted ? 'Voted' : 'Not yet voted'}
        >
          {hasVoted ? (
            <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
              <polyline points="2,6 5,9 10,3" fill="none" stroke="var(--sage)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          ) : (
            <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
              <circle cx="6" cy="6" r="4" fill="none" stroke="rgba(250,245,235,0.25)" strokeWidth="1.2" />
            </svg>
          )}
        </span>
      )}
      {!isOnline && (
        <span className="ladder-people-panel__offline-label">offline</span>
      )}
    </div>
  );
}

export function LadderPeoplePanel({
  ladder,
  presentUserIds,
  currentUserId,
}: LadderPeoplePanelProps) {
  const presentArray = Array.from(presentUserIds);
  const participantUserIds = ladder.participants.map((p) => p.userId);

  // Users we need info for: all present users + all participants
  const allUserIds = Array.from(new Set([...presentArray, ...participantUserIds]));
  const userInfoCache = useUserInfoCache(allUserIds, ladder.participants);

  // Merge participant data into cache (participants come with username+avatarSeed from backend)
  const enrichedCache: Record<string, UserInfo> = { ...userInfoCache };
  ladder.participants.forEach((p) => {
    if (!enrichedCache[p.userId] || enrichedCache[p.userId].username === null) {
      enrichedCache[p.userId] = {
        userId: p.userId,
        username: p.username,
        avatar: null, // avatarSeed would need to be resolved — for now use AvatarHead fallback
      };
    }
  });

  const votedUserIds = ladder.currentRound?.votedUserIds ?? [];
  const isActiveLike = ladder.status === 'ACTIVE' || ladder.status === 'COMPLETED';

  if (!isActiveLike) {
    // DRAFT: show "In the room" — everyone who is currently present
    return (
      <aside className="ladder-people-panel" aria-label="People panel">
        <h3 className="ladder-people-panel__section-title">In the room</h3>
        {presentArray.length === 0 ? (
          <p className="ladder-people-panel__empty">No one else here yet</p>
        ) : (
          <ul className="ladder-people-panel__list">
            {presentArray.map((uid) => (
              <li key={uid}>
                <PersonRow
                  userId={uid}
                  userInfo={enrichedCache[uid]}
                  isOnline
                  isCurrentUser={uid === currentUserId}
                />
              </li>
            ))}
          </ul>
        )}
      </aside>
    );
  }

  // ACTIVE / COMPLETED: Voters + Watching sections
  const watchers = presentArray.filter((uid) => !participantUserIds.includes(uid));
  const showVoted = ladder.status === 'ACTIVE';

  return (
    <aside className="ladder-people-panel" aria-label="People panel">
      <div className="ladder-people-panel__section">
        <h3 className="ladder-people-panel__section-title">
          Voters
          <span className="ladder-people-panel__count">{ladder.participants.length}</span>
        </h3>
        {ladder.participants.length === 0 ? (
          <p className="ladder-people-panel__empty">No voters</p>
        ) : (
          <ul className="ladder-people-panel__list">
            {ladder.participants.map((p) => (
              <li key={p.userId}>
                <PersonRow
                  userId={p.userId}
                  userInfo={enrichedCache[p.userId]}
                  isOnline={presentUserIds.has(p.userId)}
                  hasVoted={votedUserIds.includes(p.userId)}
                  isCurrentUser={p.userId === currentUserId}
                  showVotedIndicator={showVoted}
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      {watchers.length > 0 && (
        <div className="ladder-people-panel__section ladder-people-panel__section--watching">
          <h3 className="ladder-people-panel__section-title">
            Watching
            <span className="ladder-people-panel__count">{watchers.length}</span>
          </h3>
          <ul className="ladder-people-panel__list">
            {watchers.map((uid) => (
              <li key={uid}>
                <PersonRow
                  userId={uid}
                  userInfo={enrichedCache[uid]}
                  isOnline
                  isCurrentUser={uid === currentUserId}
                />
              </li>
            ))}
          </ul>
        </div>
      )}
    </aside>
  );
}
