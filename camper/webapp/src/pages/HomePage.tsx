import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type Plan } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AppHeader } from '../components/AppHeader';
import { Button } from '../components/ui/Button';
import './HomePage.css';
import '../components/Modal.css';

export function HomePage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState('');
  const [deletingPlan, setDeletingPlan] = useState<Plan | null>(null);
  const [leavingPlan, setLeavingPlan] = useState<Plan | null>(null);
  const [joiningPlanId, setJoiningPlanId] = useState<string | null>(null);
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    api.getPlans()
      .then(setPlans)
      .catch(() => setError('Failed to load trips'))
      .finally(() => setLoading(false));
  }, []);

  const handleDeleteClick = (e: React.MouseEvent, plan: Plan) => {
    e.stopPropagation();
    setDeletingPlan(plan);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingPlan) return;
    try {
      await api.deletePlan(deletingPlan.id);
      setPlans(prev => prev.filter(p => p.id !== deletingPlan.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete trip');
    } finally {
      setDeletingPlan(null);
    }
  };

  const handleLeaveClick = (e: React.MouseEvent, plan: Plan) => {
    e.stopPropagation();
    setLeavingPlan(plan);
  };

  const handleLeaveConfirm = async () => {
    if (!leavingPlan || !user) return;
    try {
      await api.removeMember(leavingPlan.id, user.id);
      setPlans(prev => prev.map(p => p.id === leavingPlan.id ? { ...p, isMember: false } : p));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to leave trip');
    } finally {
      setLeavingPlan(null);
    }
  };

  const handleJoinClick = async (e: React.MouseEvent, plan: Plan) => {
    e.stopPropagation();
    if (!user) return;
    setJoiningPlanId(plan.id);
    try {
      await api.addMember(plan.id, user.email);
      setPlans(prev => prev.map(p => p.id === plan.id ? { ...p, isMember: true } : p));
      navigate(`/plans/${plan.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to join trip');
    } finally {
      setJoiningPlanId(null);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    setCreating(true);
    try {
      const plan = await api.createPlan(newName.trim());
      setPlans(prev => [...prev, plan]);
      setNewName('');
      setShowCreate(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create trip');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="home-page">
      <ParallaxBackground variant="dusk" />

      <div className="home-content">
        <AppHeader
          pageTitle="Your Expeditions"
          pageIcon={
            <svg width="22" height="22" viewBox="0 0 22 22">
              <path d="M3,17 L3,9 L11,3 L19,9 L19,17 Z" fill="none" stroke="var(--ember)" strokeWidth="1.5" strokeLinejoin="round" />
            </svg>
          }
        />

        {/* Hero section */}
        <div className="home-hero">
          <p className="home-subtitle">Choose a trail or chart a new course</p>
        </div>

        {/* Trips grid */}
        <div className="home-trips">
          {loading ? (
            <div className="home-loading">
              <div className="home-loading-flame" />
              <p>Scouting the trails...</p>
            </div>
          ) : (
            <>
              {error && <p className="home-error">{error}</p>}

              <div className="home-grid">
                {plans.map((plan, i) => {
                  const isOwner = plan.ownerId === user?.id;
                  return (
                  <div key={plan.id} className="trip-card-wrapper">
                    {isOwner && (
                      <div className="trip-card__delete-zone">
                        <button
                          className="trip-card__delete"
                          onClick={(e) => handleDeleteClick(e, plan)}
                          title="Delete trip"
                        >
                          <svg width="18" height="18" viewBox="0 0 18 18">
                            <path d="M3,5 L15,5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                            <path d="M7,5 L7,3 Q7,2 8,2 L10,2 Q11,2 11,3 L11,5" stroke="currentColor" strokeWidth="1.2" fill="none" />
                            <path d="M4.5,5 L5.5,15 Q5.5,16 6.5,16 L11.5,16 Q12.5,16 12.5,15 L13.5,5" stroke="currentColor" strokeWidth="1.2" fill="none" />
                            <line x1="7.5" y1="8" x2="7.5" y2="13" stroke="currentColor" strokeWidth="1" strokeLinecap="round" />
                            <line x1="10.5" y1="8" x2="10.5" y2="13" stroke="currentColor" strokeWidth="1" strokeLinecap="round" />
                          </svg>
                        </button>
                      </div>
                    )}
                    {!isOwner && plan.isMember && (
                      <div className="trip-card__leave-zone">
                        <button
                          className="trip-card__leave"
                          onClick={(e) => handleLeaveClick(e, plan)}
                          title="Leave trip"
                        >
                          <svg width="18" height="18" viewBox="0 0 18 18">
                            <path d="M6,3 L3,3 Q2,3 2,4 L2,14 Q2,15 3,15 L6,15" stroke="currentColor" strokeWidth="1.3" fill="none" strokeLinecap="round" />
                            <path d="M7,9 L15,9" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                            <path d="M12,6 L15,9 L12,12" stroke="currentColor" strokeWidth="1.3" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                          </svg>
                        </button>
                      </div>
                    )}
                    <button
                      className="trip-card"
                      style={{ animationDelay: `${i * 0.08}s` }}
                      onClick={plan.isMember ? () => navigate(`/plans/${plan.id}`) : (e) => handleJoinClick(e, plan)}
                    >
                      <div className="trip-card__icon">
                        <svg width="44" height="48" viewBox="0 0 44 48">
                          {/* Crossed paddles behind tent */}
                          <line x1="4" y1="44" x2="36" y2="4" stroke="var(--tan-deep)" strokeWidth="2.2" strokeLinecap="round" />
                          <ellipse cx="35" cy="5" rx="6.5" ry="2" fill="var(--tan-deep)" stroke="var(--charcoal-light)" strokeWidth="0.8" transform="rotate(-48 35 5)" />
                          <line x1="40" y1="44" x2="8" y2="4" stroke="var(--tan-deep)" strokeWidth="2.2" strokeLinecap="round" />
                          <ellipse cx="9" cy="5" rx="6.5" ry="2" fill="var(--tan-deep)" stroke="var(--charcoal-light)" strokeWidth="0.8" transform="rotate(48 9 5)" />
                          {/* Tent body */}
                          <polygon points="22,10 6,40 38,40" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="1.5" strokeLinejoin="round" />
                          <polygon points="22,10 22,40 38,40" fill="var(--sage-deep)" opacity="0.35" />
                          <path d="M17,40 Q19,28 22,24 Q25,28 27,40" fill="var(--charcoal)" opacity="0.7" />
                          <line x1="22" y1="10" x2="22" y2="24" stroke="var(--sage-dark)" strokeWidth="1" opacity="0.5" />
                          <line x1="22" y1="10" x2="22" y2="5" stroke="var(--charcoal-light)" strokeWidth="1.2" />
                          <polygon points="22,5 28,7.5 22,10" fill="var(--rose-deep)" stroke="var(--rose-deep)" strokeWidth="0.5" />
                        </svg>
                      </div>
                      <div className="trip-card__info">
                        <h3 className="trip-card__name">{plan.name}</h3>
                        <p className="trip-card__date">
                          Created {new Date(plan.createdAt).toLocaleDateString('en-US', {
                            month: 'short', day: 'numeric', year: 'numeric'
                          })}
                        </p>
                      </div>
                      {plan.isMember ? (
                        <div className="trip-card__arrow">
                          <svg width="20" height="20" viewBox="0 0 20 20">
                            <path d="M7,4 L14,10 L7,16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                          </svg>
                        </div>
                      ) : (
                        <div className={`trip-card__join ${joiningPlanId === plan.id ? 'trip-card__join--loading' : ''}`}>
                          <svg width="20" height="20" viewBox="0 0 20 20">
                            <circle cx="10" cy="8" r="4" fill="none" stroke="currentColor" strokeWidth="1.5" />
                            <path d="M3,18 Q3,13 10,13 Q17,13 17,18" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                            <line x1="15" y1="4" x2="15" y2="10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                            <line x1="12" y1="7" x2="18" y2="7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                          </svg>
                          <span className="trip-card__join-text">Join</span>
                        </div>
                      )}
                    </button>
                  </div>
                  );
                })}

                {/* Create new trip card */}
                {!showCreate ? (
                  <button
                    className="trip-card trip-card--new"
                    style={{ animationDelay: `${plans.length * 0.08}s` }}
                    onClick={() => setShowCreate(true)}
                  >
                    <div className="trip-card__icon trip-card__icon--new">
                      <svg width="40" height="40" viewBox="0 0 40 40">
                        <rect x="6" y="8" width="28" height="24" rx="2" fill="none" stroke="var(--sage)" strokeWidth="1.5" strokeDasharray="3,3" />
                        <line x1="20" y1="14" x2="20" y2="26" stroke="var(--sage)" strokeWidth="1.5" strokeLinecap="round" opacity="0.6" />
                        <line x1="14" y1="20" x2="26" y2="20" stroke="var(--sage)" strokeWidth="1.5" strokeLinecap="round" opacity="0.6" />
                      </svg>
                    </div>
                    <div className="trip-card__info">
                      <h3 className="trip-card__name trip-card__name--new">Plan a New Trip</h3>
                      <p className="trip-card__date">Start a new adventure</p>
                    </div>
                    <div className="trip-card__plus">+</div>
                  </button>
                ) : (
                  <form
                    className="trip-card trip-card--form"
                    style={{ animationDelay: `${plans.length * 0.08}s` }}
                    onSubmit={handleCreate}
                  >
                    <input
                      className="trip-create-input"
                      placeholder="Name your expedition..."
                      value={newName}
                      onChange={e => setNewName(e.target.value)}
                      autoFocus
                    />
                    <div className="trip-create-actions">
                      <button
                        type="button"
                        className="trip-create-cancel"
                        onClick={() => { setShowCreate(false); setNewName(''); }}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        className="trip-create-submit"
                        disabled={creating || !newName.trim()}
                      >
                        {creating ? 'Creating...' : 'Set Camp'}
                      </button>
                    </div>
                  </form>
                )}
              </div>

              {plans.length === 0 && !showCreate && (
                <div className="home-empty">
                  <svg width="80" height="80" viewBox="0 0 80 80" className="home-empty-icon">
                    <polygon points="40,10 15,65 65,65" fill="none" stroke="var(--tan-deep)" strokeWidth="2" />
                    <path d="M33,52 Q37,38 40,34 Q43,38 47,52" fill="var(--ember)" opacity="0.4" />
                    <path d="M35,52 Q38,42 40,38 Q42,42 45,52" fill="var(--butter)" opacity="0.4" />
                  </svg>
                  <p className="home-empty-text">No expeditions yet. Light your first campfire!</p>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Delete confirmation modal */}
      {deletingPlan && (
        <div className="modal-overlay" onClick={() => setDeletingPlan(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-icon-large">
              <svg width="48" height="48" viewBox="0 0 48 48">
                <path d="M10,16 L38,16" stroke="var(--rose-deep)" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M18,16 L18,10 Q18,8 20,8 L28,8 Q30,8 30,10 L30,16" stroke="var(--rose-deep)" strokeWidth="2" fill="none" />
                <path d="M13,16 L15,40 Q15,42 17,42 L31,42 Q33,42 33,40 L35,16" stroke="var(--rose-deep)" strokeWidth="2" fill="none" />
                <line x1="20" y1="22" x2="20" y2="36" stroke="var(--rose-deep)" strokeWidth="1.5" strokeLinecap="round" />
                <line x1="24" y1="22" x2="24" y2="36" stroke="var(--rose-deep)" strokeWidth="1.5" strokeLinecap="round" />
                <line x1="28" y1="22" x2="28" y2="36" stroke="var(--rose-deep)" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
            </div>
            <h2 className="modal-title">Abandon Expedition?</h2>
            <p className="modal-flavor">"{deletingPlan.name}" will be lost to the wilderness forever.</p>
            <div className="modal-actions">
              <Button variant="secondary" onClick={() => setDeletingPlan(null)}>
                Keep Camp
              </Button>
              <Button variant="danger" onClick={handleDeleteConfirm}>
                Break Camp
              </Button>
            </div>
          </div>
        </div>
      )}
      {/* Leave confirmation modal */}
      {leavingPlan && (
        <div className="modal-overlay" onClick={() => setLeavingPlan(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-icon-large">
              <svg width="48" height="48" viewBox="0 0 48 48">
                <path d="M16,8 L10,8 Q6,8 6,12 L6,36 Q6,40 10,40 L16,40" stroke="var(--charcoal-light)" strokeWidth="2.5" fill="none" strokeLinecap="round" />
                <path d="M20,24 L40,24" stroke="var(--charcoal-light)" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M34,18 L40,24 L34,30" stroke="var(--charcoal-light)" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <h2 className="modal-title">Leave Expedition?</h2>
            <p className="modal-flavor">You'll pack up your gear and leave "{leavingPlan.name}" behind.</p>
            <div className="modal-actions">
              <Button variant="secondary" onClick={() => setLeavingPlan(null)}>
                Stay at Camp
              </Button>
              <Button variant="danger" onClick={handleLeaveConfirm}>
                Leave Camp
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
