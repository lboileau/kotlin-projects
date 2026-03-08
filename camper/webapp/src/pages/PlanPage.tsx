import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api, type Plan, type PlanMember } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { Campfire } from '../components/Campfire';
import { CamperAvatar } from '../components/CamperAvatar';
import { InteractableItem } from '../components/InteractableItem';
import { ComingSoonModal } from '../components/ComingSoonModal';
import { AddMemberModal } from '../components/AddMemberModal';
import { TentSVG, EquipmentPileSVG, KitchenSVG, MapTableSVG } from '../components/CampsiteItems';
import './PlanPage.css';

type ModalType = 'equipment' | 'kitchen' | 'itinerary' | 'tent' | 'addMember' | null;

const MODAL_CONFIG: Record<string, { title: string; icon: React.ReactNode }> = {
  equipment: {
    title: 'Equipment & Gear',
    icon: (
      <svg width="48" height="48" viewBox="0 0 48 48">
        <rect x="12" y="8" width="24" height="32" rx="6" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="2" />
        <rect x="16" y="12" width="16" height="10" rx="3" fill="var(--sage-deep)" />
        <path d="M15,14 Q12,28 15,36" fill="none" stroke="var(--sage-dark)" strokeWidth="2" strokeLinecap="round" />
        <path d="M33,14 Q36,28 33,36" fill="none" stroke="var(--sage-dark)" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
  },
  kitchen: {
    title: 'Camp Kitchen & Meals',
    icon: (
      <svg width="48" height="48" viewBox="0 0 48 48">
        <ellipse cx="24" cy="28" rx="16" ry="5" fill="var(--charcoal-light)" />
        <path d="M8,28 Q8,42 24,42 Q40,42 40,28" fill="var(--charcoal-light)" />
        <path d="M18,18 Q16,10 20,4" fill="none" stroke="rgba(255,255,255,0.4)" strokeWidth="2" strokeLinecap="round" />
        <path d="M24,16 Q22,8 26,2" fill="none" stroke="rgba(255,255,255,0.3)" strokeWidth="2" strokeLinecap="round" />
        <path d="M30,18 Q28,10 32,6" fill="none" stroke="rgba(255,255,255,0.25)" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
  },
  itinerary: {
    title: 'Trail Map & Itinerary',
    icon: (
      <svg width="48" height="48" viewBox="0 0 48 48">
        <rect x="6" y="10" width="36" height="28" rx="2" fill="var(--parchment)" stroke="var(--tan-deep)" strokeWidth="1.5" />
        <path d="M12,22 Q20,16 28,24 Q36,30 42,20" fill="none" stroke="var(--rose-deep)" strokeWidth="1.5" strokeDasharray="3,2" />
        <circle cx="16" cy="20" r="2" fill="var(--sage)" />
        <circle cx="36" cy="22" r="2" fill="var(--flame)" />
      </svg>
    ),
  },
  tent: {
    title: 'Tents & Canoe Pairings',
    icon: (
      <svg width="48" height="48" viewBox="0 0 48 48">
        <polygon points="24,6 6,40 42,40" fill="var(--rose)" stroke="var(--rose-deep)" strokeWidth="2" />
        <polygon points="24,6 24,40 42,40" fill="var(--rose-deep)" opacity="0.4" />
        <path d="M19,40 Q22,28 24,24 Q26,28 29,40" fill="var(--rose-deep)" />
      </svg>
    ),
  },
};

export function PlanPage() {
  const { planId } = useParams<{ planId: string }>();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [plan, setPlan] = useState<Plan | null>(null);
  const [members, setMembers] = useState<PlanMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeModal, setActiveModal] = useState<ModalType>(null);
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
      <header className="plan-header">
        <button className="plan-back" onClick={() => navigate('/')}>
          <svg width="20" height="20" viewBox="0 0 20 20">
            <path d="M13,4 L7,10 L13,16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          Back to Trails
        </button>
        <div className="plan-header-center">
          <svg width="24" height="24" viewBox="0 0 24 24" className="plan-header-icon">
            <polygon points="12,2 4,20 20,20" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
            <path d="M10,16 Q11,12 12,10 Q13,12 14,16" fill="var(--ember)" opacity="0.6" />
          </svg>
          <span className="plan-header-title">Campsite</span>
        </div>
        <div className="plan-header-user">
          <button className="plan-user-btn" onClick={() => navigate('/account')}>
            <svg width="28" height="28" viewBox="0 0 28 28" className="plan-user-avatar">
              <defs><clipPath id="avatar-clip-plan"><circle cx="14" cy="14" r="13" /></clipPath></defs>
              <circle cx="14" cy="14" r="13" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="1.5" />
              <g clipPath="url(#avatar-clip-plan)">
                <circle cx="14" cy="11" r="5" fill="var(--parchment)" />
                <ellipse cx="14" cy="24" rx="8" ry="6" fill="var(--parchment)" />
              </g>
            </svg>
            <span className="plan-user-name">{user?.username || user?.email}</span>
          </button>
          <button className="plan-logout" onClick={logout}>Log Out</button>
        </div>
      </header>

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
            <InteractableItem id="tent" label="Tents & Canoe Pairings" x={10} y={58} onClick={() => setActiveModal('tent')}>
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
                    index={i}
                    total={memberCount}
                    timeOfDay={timeOfDay}
                    onRemove={isOwner && member.userId !== user?.id ? () => handleRemoveMember(member.userId) : undefined}
                  />
                ))}
              </div>

              {/* Campfire */}
              <Campfire />

              {/* Invite shadow person — fixed below fire */}
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

      {activeModal && activeModal !== 'addMember' && MODAL_CONFIG[activeModal] && (
        <ComingSoonModal
          isOpen
          onClose={() => setActiveModal(null)}
          title={MODAL_CONFIG[activeModal].title}
          icon={MODAL_CONFIG[activeModal].icon}
        />
      )}
    </div>
  );
}
