import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type LadderSummary } from '../api/client';
import { AppHeader } from '../components/AppHeader';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { Button } from '../components/ui/Button';
import './ActivitiesListPage.css';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Draft',
  ACTIVE: 'Active',
  COMPLETED: 'Completed',
};

export function ActivitiesListPage() {
  const navigate = useNavigate();
  const [ladders, setLadders] = useState<LadderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.ladders
      .list()
      .then((res) => {
        setLadders(res ?? []);
      })
      .catch(() => {
        setError('Failed to load activity ladders.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div className="activities-list-page">
      <ParallaxBackground variant="dusk" />
      <div className="activities-list-content">
        <AppHeader
          pageTitle="Activities"
          pageIcon={
            <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
              <polygon points="9,2 11.5,7 17,7 12.5,10.5 14,16 9,13 4,16 5.5,10.5 1,7 6.5,7" fill="none" stroke="currentColor" strokeWidth="1.3" />
            </svg>
          }
          actions={
            <Button variant="primary" size="sm" onClick={() => navigate('/activities/new')}>
              New Ladder
            </Button>
          }
        />

        <main className="activities-list-main">
          {loading && (
            <p className="activities-list-status">Loading...</p>
          )}
          {!loading && error && (
            <p className="activities-list-status activities-list-status--error">{error}</p>
          )}
          {!loading && !error && ladders.length === 0 && (
            <div className="activities-list-empty">
              <svg width="48" height="48" viewBox="0 0 48 48" aria-hidden="true">
                <polygon points="24,4 29,18 44,18 32,28 37,42 24,33 11,42 16,28 4,18 19,18" fill="none" stroke="var(--ember)" strokeWidth="1.5" opacity="0.5" />
              </svg>
              <p>No activity ladders yet.</p>
              <Button variant="primary" onClick={() => navigate('/activities/new')}>
                Create the first one
              </Button>
            </div>
          )}

          {!loading && !error && ladders.length > 0 && (
            <ul className="activities-list__items">
              {ladders.map((ladder) => (
                <li key={ladder.id}>
                  <button
                    className="activities-list__card"
                    onClick={() => navigate(`/activities/${ladder.id}`)}
                    aria-label={`Open ladder: ${ladder.title}`}
                  >
                    <div className="activities-list__card-header">
                      <span className="activities-list__card-title">{ladder.title}</span>
                      <span className={`activities-list__status-badge activities-list__status-badge--${ladder.status.toLowerCase()}`}>
                        {STATUS_LABELS[ladder.status] ?? ladder.status}
                      </span>
                    </div>
                    <div className="activities-list__card-meta">
                      <span className="activities-list__meta-item">
                        <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
                          <rect x="1" y="3" width="10" height="8" rx="1" fill="none" stroke="currentColor" strokeWidth="1" />
                          <line x1="4" y1="5.5" x2="8" y2="5.5" stroke="currentColor" strokeWidth="0.8" strokeLinecap="round" />
                          <line x1="4" y1="7.5" x2="7" y2="7.5" stroke="currentColor" strokeWidth="0.8" strokeLinecap="round" />
                        </svg>
                        {ladder.activityCount} {ladder.activityCount === 1 ? 'activity' : 'activities'}
                      </span>
                      {ladder.participantCount > 0 && (
                        <span className="activities-list__meta-item">
                          <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
                            <circle cx="5" cy="4" r="2.5" fill="none" stroke="currentColor" strokeWidth="1" />
                            <path d="M1,11 Q1,8 5,8 Q9,8 9,11" fill="none" stroke="currentColor" strokeWidth="1" />
                            <circle cx="9" cy="5" r="2" fill="none" stroke="currentColor" strokeWidth="0.8" />
                          </svg>
                          {ladder.participantCount} voter{ladder.participantCount === 1 ? '' : 's'}
                        </span>
                      )}
                    </div>
                    <div className="activities-list__card-arrow" aria-hidden="true">
                      <svg width="14" height="14" viewBox="0 0 14 14">
                        <path d="M5,3 L9,7 L5,11" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </main>
      </div>
    </div>
  );
}
