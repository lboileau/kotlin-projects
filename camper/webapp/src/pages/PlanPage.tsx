import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { api, type Plan, type PlanMember } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { usePlanUpdates } from '../hooks/usePlanUpdates';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { Campfire } from '../components/Campfire';
import { CamperAvatar } from '../components/CamperAvatar';
import { InteractableItem } from '../components/InteractableItem';
import { AddMemberModal } from '../components/AddMemberModal';
import { GearModal } from '../components/GearModal';
import { MealPlanModal } from '../components/MealPlanModal';
import { ItineraryModal } from '../components/ItineraryModal';
import { AssignmentsModal } from '../components/AssignmentsModal';
import { TentSVG, EquipmentPileSVG, KitchenSVG, MapTableSVG, LogBookSVG } from '../components/CampsiteItems';
import { LogBookModal } from '../components/LogBookModal';
import { ProfileSetupModal } from '../components/ProfileSetupModal';
import { AppHeader } from '../components/AppHeader';
import './PlanPage.css';

type ModalType = 'equipment' | 'kitchen' | 'itinerary' | 'assignments' | 'logbook' | 'addMember' | 'managePlan' | null;

export function PlanPage() {
  const { planId } = useParams<{ planId: string }>();
  const { user, login } = useAuth();
  const [plan, setPlan] = useState<Plan | null>(null);
  const [members, setMembers] = useState<PlanMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const [showProfileSetup, setShowProfileSetup] = useState(false);
  const [timeOfDay, setTimeOfDay] = useState<'day' | 'night'>(() => {
    const hour = new Date().getHours();
    return hour >= 6 && hour < 19 ? 'day' : 'night';
  });

  const isOwner = plan?.ownerId === user?.id;

  const loadData = useCallback(async () => {
    if (!planId) return;
    try {
      const [plans, memberData] = await Promise.all([
        api.getPlans(),
        api.getPlanMembers(planId),
      ]);
      setPlan(plans.find(p => p.id === planId) || null);
      setMembers(memberData);
    } catch {
      // handle error silently for now
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Show profile setup modal if user hasn't completed their profile
  useEffect(() => {
    if (!loading && user && user.profileCompleted === false) {
      setShowProfileSetup(true);
    }
  }, [loading, user]);

  // Live updates: resource-aware refetch via WebSocket
  const [assignmentsRefreshKey, setAssignmentsRefreshKey] = useState(0);
  const [itineraryRefreshKey, setItineraryRefreshKey] = useState(0);
  const [logbookRefreshKey, setLogbookRefreshKey] = useState(0);

  usePlanUpdates(planId, useCallback((message) => {
    const { resource } = message;
    if (resource === 'plan' || resource === 'members') {
      loadData();
    }
    if (resource === 'assignments') {
      setAssignmentsRefreshKey(k => k + 1);
    }
    if (resource === 'itinerary') {
      setItineraryRefreshKey(k => k + 1);
    }
    if (resource === 'log-book-faqs' || resource === 'log-book-journal') {
      setLogbookRefreshKey(k => k + 1);
    }
  }, [loadData]));

  const handleAddMember = async (email: string) => {
    if (!planId) return;
    await api.addMember(planId, email);
    await loadData();
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!planId) return;
    await api.removeMember(planId, memberId);
    await loadData();
  };

  const [updatingVisibility, setUpdatingVisibility] = useState(false);
  const [editPlanName, setEditPlanName] = useState('');
  const [savingName, setSavingName] = useState(false);
  const [updatingRole, setUpdatingRole] = useState<string | null>(null);

  const handleUpdateRole = async (userId: string, role: string) => {
    if (!planId) return;
    setUpdatingRole(userId);
    try {
      await api.updateMemberRole(planId, userId, role);
      await loadData();
    } finally {
      setUpdatingRole(null);
    }
  };

  const handleSavePlanName = async () => {
    if (!planId || !plan || !editPlanName.trim() || editPlanName.trim() === plan.name) return;
    setSavingName(true);
    try {
      const updated = await api.updatePlan(planId, { name: editPlanName.trim(), visibility: plan.visibility });
      setPlan(updated);
    } finally {
      setSavingName(false);
    }
  };

  const handleToggleVisibility = async () => {
    if (!planId || !plan) return;
    setUpdatingVisibility(true);
    try {
      const newVisibility = plan.visibility === 'public' ? 'private' : 'public';
      const updated = await api.updatePlan(planId, { name: plan.name, visibility: newVisibility });
      setPlan(updated);
    } finally {
      setUpdatingVisibility(false);
    }
  };

  const isMember = members.some(m => m.userId === user?.id);

  const [joining, setJoining] = useState(false);

  const handleJoinPlan = async () => {
    if (!planId || !user) return;
    setJoining(true);
    try {
      await api.addMember(planId, user.email);
      await loadData();
    } finally {
      setJoining(false);
    }
  };

  const memberCount = members.length;

  return (
    <div className="plan-page">
      <ParallaxBackground variant="campsite" timeOfDay={timeOfDay} />

      {/* Clickable celestial toggle — fixed overlay so it's above page content */}
      <button
        className="celestial-toggle"
        onClick={() => setTimeOfDay(t => t === 'day' ? 'night' : 'day')}
        aria-label="Toggle day/night"
      >
        {timeOfDay === 'day' ? (
          <svg width="44" height="44" viewBox="0 0 44 44">
            <circle cx="22" cy="22" r="14" fill="#F0D060" />
            {Array.from({ length: 8 }).map((_, i) => {
              const a = (i / 8) * Math.PI * 2;
              const x1 = 22 + Math.cos(a) * 17;
              const y1 = 22 + Math.sin(a) * 17;
              const x2 = 22 + Math.cos(a) * 21;
              const y2 = 22 + Math.sin(a) * 21;
              return <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke="#F0D060" strokeWidth="2" strokeLinecap="round" />;
            })}
          </svg>
        ) : (
          <svg width="40" height="40" viewBox="0 0 40 40">
            <circle cx="20" cy="20" r="14" fill="#C8D0D8" />
            <circle cx="26" cy="16" r="12" fill="#1A2030" />
          </svg>
        )}
      </button>

      {/* Top nav */}
      <AppHeader
        pageTitle={plan?.name || 'Campsite'}
        pageIcon={
          <svg width="24" height="24" viewBox="0 0 24 24">
            <polygon points="12,2 4,20 20,20" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
            <path d="M10,16 Q11,12 12,10 Q13,12 14,16" fill="var(--ember)" opacity="0.6" />
          </svg>
        }
        actions={isOwner ? (
          <button className="app-header__action-btn" onClick={() => { setEditPlanName(plan?.name || ''); setActiveModal('managePlan'); }}>
            <svg width="18" height="18" viewBox="0 0 18 18">
              <circle cx="9" cy="9" r="7" fill="none" stroke="currentColor" strokeWidth="1.5" />
              <path d="M9,5 L9,13 M5,9 L13,9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            Manage Plan
          </button>
        ) : undefined}
        onProfileClick={() => setShowProfileSetup(true)}
      />

      {/* Campsite scene */}
      <div className="campsite-scene">
        {loading ? (
          <div className="campsite-loading">
            <div className="campsite-loading-flame" />
            <p>Setting up camp...</p>
          </div>
        ) : (
          <>
            {/* Background items — on the ground below the 50% treeline */}
            <InteractableItem id="tent" label="Tents & Canoe Pairings" x={10} y={58} onClick={() => setActiveModal('assignments')}>
              <TentSVG />
            </InteractableItem>

            <InteractableItem id="equipment" label="Equipment & Gear" x={90} y={58} onClick={() => setActiveModal('equipment')}>
              <EquipmentPileSVG />
            </InteractableItem>

            <InteractableItem id="kitchen" label="Camp Kitchen & Meals" x={88} y={78} onClick={() => setActiveModal('kitchen')}>
              <KitchenSVG />
            </InteractableItem>

            <InteractableItem id="itinerary" label="Trail Map & Itinerary" x={12} y={78} onClick={() => setActiveModal('itinerary')}>
              <MapTableSVG />
            </InteractableItem>

            <InteractableItem id="logbook" label="Camp Log Book" x={28} y={58} onClick={() => setActiveModal('logbook')}>
              <LogBookSVG />
            </InteractableItem>

            {/* Central campfire area */}
            <div className="campsite-center">
              {/* Campers around fire */}
              <div className="campfire-circle">
                {[...members]
                  .sort((a, b) => {
                    // Named members first, pending last (right/bottom-most)
                    if (a.username && !b.username) return -1;
                    if (!a.username && b.username) return 1;
                    return 0;
                  })
                  .map((member, i) => (
                  <CamperAvatar
                    key={member.userId}
                    name={member.username || null}
                    email={member.email}
                    invitationStatus={member.invitationStatus}
                    role={plan?.ownerId === member.userId ? 'owner' : member.role === 'manager' ? 'manager' : 'member'}
                    index={i}
                    total={memberCount}
                    timeOfDay={timeOfDay}
                    avatar={member.avatar}
                    onRemove={
                      // Owner can remove anyone except themselves
                      (isOwner && member.userId !== user?.id)
                      // Members can remove themselves
                      || (member.userId === user?.id && !isOwner)
                      // Inviter can cancel their own pending invites (not yet registered)
                      || (member.invitedBy === user?.id && !member.username && member.userId !== user?.id)
                        ? () => handleRemoveMember(member.userId)
                        : undefined
                    }
                  />
                ))}
              </div>

              {/* Campfire */}
              <Campfire />

              {/* Below fire: Join button for non-members, invite ghost for members */}
              {isMember ? (
                <button
                  className="campsite-invite-ghost"
                  onClick={() => setActiveModal('addMember')}
                  title="Invite an adventurer"
                >
                  <div className="invite-ghost-figure">
                    <svg width="48" height="64" viewBox="0 0 48 64">
                      <ellipse cx="24" cy="16" rx="12" ry="12" fill="rgba(255,255,255,0.18)" />
                      <rect x="14" y="24" width="20" height="28" rx="4" fill="rgba(255,255,255,0.12)" />
                      <line x1="24" y1="32" x2="24" y2="44" stroke="rgba(255,248,231,0.5)" strokeWidth="2.5" strokeLinecap="round" />
                      <line x1="18" y1="38" x2="30" y2="38" stroke="rgba(255,248,231,0.5)" strokeWidth="2.5" strokeLinecap="round" />
                    </svg>
                  </div>
                </button>
              ) : (
                <button
                  className="campsite-join-btn"
                  onClick={handleJoinPlan}
                  disabled={joining}
                  title="Join this adventure"
                >
                  <div className="join-ghost-figure">
                    <svg width="48" height="64" viewBox="0 0 48 64">
                      <ellipse cx="24" cy="16" rx="12" ry="12" fill="rgba(139,195,128,0.3)" />
                      <rect x="14" y="24" width="20" height="28" rx="4" fill="rgba(139,195,128,0.2)" />
                      <circle cx="24" cy="16" r="6" fill="none" stroke="rgba(139,195,128,0.7)" strokeWidth="1.5" />
                      <path d="M22,16 L26,16 M24,14 L24,18" stroke="rgba(255,248,231,0.8)" strokeWidth="2" strokeLinecap="round" />
                    </svg>
                  </div>
                  <span className="join-label">{joining ? 'Joining...' : 'Join Camp'}</span>
                </button>
              )}
            </div>

          </>
        )}
      </div>

      {/* Modals */}
      {activeModal === 'addMember' && (
        <AddMemberModal
          isOpen
          onClose={() => setActiveModal(null)}
          onAdd={handleAddMember}
        />
      )}

      {activeModal === 'managePlan' && plan && (
        <div className="modal-overlay" onClick={() => setActiveModal(null)}>
          <div className="modal-content manage-plan-modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Manage Plan</h2>
            <div className="modal-divider">
              <svg width="120" height="12" viewBox="0 0 120 12">
                <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
              </svg>
            </div>
            <div className="manage-plan-setting">
              <div className="manage-plan-setting-info" style={{ flex: 1 }}>
                <span className="manage-plan-setting-label">Plan Name</span>
                <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                  <input
                    type="text"
                    className="manage-plan-name-input"
                    value={editPlanName}
                    onChange={e => setEditPlanName(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') handleSavePlanName(); }}
                    placeholder="Plan name"
                  />
                  <button
                    className="modal-btn manage-plan-save-btn"
                    onClick={handleSavePlanName}
                    disabled={savingName || !editPlanName.trim() || editPlanName.trim() === plan.name}
                  >
                    {savingName ? '...' : 'Save'}
                  </button>
                </div>
              </div>
            </div>

            <div className="manage-plan-setting">
              <div className="manage-plan-setting-info">
                <span className="manage-plan-setting-label">Plan Visibility</span>
                <span className="manage-plan-setting-desc">
                  {plan.visibility === 'public'
                    ? 'Anyone can discover and view this plan.'
                    : 'Only members can see this plan.'}
                </span>
              </div>
              <button
                className={`visibility-toggle ${plan.visibility}`}
                onClick={handleToggleVisibility}
                disabled={updatingVisibility}
              >
                <span className="visibility-toggle-track">
                  <span className="visibility-toggle-thumb" />
                </span>
                <span className="visibility-toggle-label">
                  {plan.visibility === 'public' ? 'Public' : 'Private'}
                </span>
              </button>
            </div>
            <div className="manage-plan-setting" style={{ flexDirection: 'column', alignItems: 'stretch' }}>
              <span className="manage-plan-setting-label">Members</span>
              <div className="manage-plan-members-list">
                {members.filter(m => m.username).map(member => {
                  const memberIsOwner = plan.ownerId === member.userId;
                  const effectiveRole = memberIsOwner ? 'owner' : member.role;
                  return (
                    <div key={member.userId} className="manage-plan-member-row">
                      <span className="manage-plan-member-name">
                        {member.username}{member.userId === user?.id ? ' (You)' : ''}
                      </span>
                      {memberIsOwner ? (
                        <span className="manage-plan-role-label">Owner</span>
                      ) : isOwner ? (
                        <select
                          className="manage-plan-role-select"
                          value={effectiveRole}
                          onChange={e => handleUpdateRole(member.userId, e.target.value)}
                          disabled={updatingRole === member.userId}
                        >
                          <option value="member">Member</option>
                          <option value="manager">Manager</option>
                        </select>
                      ) : (
                        <span className="manage-plan-role-label">
                          {effectiveRole === 'manager' ? 'Manager' : 'Member'}
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="modal-actions" style={{ marginTop: 'var(--space-lg)' }}>
              <button className="modal-btn" onClick={() => setActiveModal(null)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {activeModal === 'equipment' && planId && user && plan && (
        <GearModal
          isOpen
          onClose={() => setActiveModal(null)}
          planId={planId}
          planOwnerId={plan.ownerId}
          members={members}
          currentUserId={user.id}
        />
      )}

      {activeModal === 'kitchen' && planId && (
        <MealPlanModal
          isOpen
          onClose={() => setActiveModal(null)}
          planId={planId}
        />
      )}

      {activeModal === 'itinerary' && planId && (
        <ItineraryModal
          isOpen
          onClose={() => setActiveModal(null)}
          planId={planId}
          isOwner={isOwner}
          refreshKey={itineraryRefreshKey}
        />
      )}

      {activeModal === 'logbook' && planId && user && plan && (
        <LogBookModal
          isOpen
          onClose={() => setActiveModal(null)}
          planId={planId}
          userId={user.id}
          userRole={
            plan.ownerId === user.id
              ? 'OWNER'
              : members.find(m => m.userId === user.id)?.role === 'manager'
                ? 'MANAGER'
                : 'MEMBER'
          }
          members={members
            .filter(m => m.username)
            .map(m => ({ id: m.userId, displayName: m.username! }))}
          refreshKey={logbookRefreshKey}
        />
      )}

      {activeModal === 'assignments' && planId && user && plan && (
        <AssignmentsModal
          isOpen
          onClose={() => {
            api.syncGear(planId).catch(() => {});
            setActiveModal(null);
          }}
          planId={planId}
          planOwnerId={plan.ownerId}
          currentUserId={user.id}
          members={members}
          refreshKey={assignmentsRefreshKey}
        />
      )}

      {showProfileSetup && user && (
        <ProfileSetupModal
          isOpen
          user={user}
          onComplete={(updatedUser) => {
            login(updatedUser);
            setShowProfileSetup(false);
          }}
          onClose={() => setShowProfileSetup(false)}
        />
      )}
    </div>
  );
}
