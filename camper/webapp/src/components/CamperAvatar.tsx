import './CamperAvatar.css';

function seededRandom(seed: number) {
  const x = Math.sin(seed * 9301 + 49297) * 49297;
  return x - Math.floor(x);
}

interface Props {
  name: string | null;
  email?: string | null;
  invitationStatus?: string | null;
  index: number;
  total: number;
  isAddButton?: boolean;
  timeOfDay?: 'day' | 'night';
  onClick?: () => void;
  onRemove?: () => void;
}

const AVATAR_COLORS = [
  { body: '#6B4E3D', accent: '#8B6B4A', hood: '#3A5A3A', skin: '#D4A574' },
  { body: '#4A5A6A', accent: '#5A6A7A', hood: '#2A3A4A', skin: '#C4946A' },
  { body: '#5A4A3A', accent: '#7A6A5A', hood: '#6A3A2A', skin: '#DCAC7C' },
  { body: '#4A6A5A', accent: '#5A7A6A', hood: '#3A4A3A', skin: '#C89A6A' },
  { body: '#6A5A4A', accent: '#8A7A6A', hood: '#4A3A2A', skin: '#D4A070' },
  { body: '#5A5A6A', accent: '#6A6A7A', hood: '#3A3A5A', skin: '#CCA478' },
  { body: '#5A4A4A', accent: '#7A5A4A', hood: '#4A5A4A', skin: '#D8AA74' },
  { body: '#4A5A4A', accent: '#6A7A5A', hood: '#3A4A3A', skin: '#C8986A' },
];

export function CamperAvatar({ name, email, invitationStatus, index, total, isAddButton, timeOfDay, onClick, onRemove }: Props) {
  // Semicircle above fire: 9 o'clock → 12 o'clock → 3 o'clock
  // Scale radius with member count so avatars don't overlap
  const startAngle = Math.PI;           // 9 o'clock (left)
  const arcSpan = Math.PI;              // 180 degrees
  const angleStep = total > 1 ? arcSpan / (total - 1) : 0;
  const angle = startAngle + angleStep * index;
  const baseRadiusX = 160;
  const baseRadiusY = 100;
  const scale = total > 4 ? 1 + (total - 4) * 0.15 : 1;
  const radiusX = baseRadiusX * scale;
  const radiusY = baseRadiusY * scale;
  const x = Math.cos(angle) * radiusX;
  const y = Math.sin(angle) * radiusY;

  const color = AVATAR_COLORS[index % AVATAR_COLORS.length];
  const isPending = !name;
  const isFailed = invitationStatus === 'failed' || invitationStatus === 'bounced' || invitationStatus === 'complained';
  const displayName = name || null;

  if (isAddButton) {
    return (
      <button
        className="camper-avatar camper-avatar--add"
        style={{
          transform: `translate(calc(${x}px - 50%), calc(${y}px - 50%))`,
          animationDelay: `${index * 0.1}s`,
        }}
        onClick={onClick}
        title="Invite an adventurer"
      >
        <div className="avatar-figure avatar-figure--ghost">
          <svg width="48" height="64" viewBox="0 0 48 64">
            {/* Ghost silhouette */}
            <ellipse cx="24" cy="16" rx="12" ry="12" fill="rgba(255,255,255,0.15)" />
            <rect x="14" y="24" width="20" height="28" rx="4" fill="rgba(255,255,255,0.1)" />
            {/* Plus sign */}
            <line x1="24" y1="30" x2="24" y2="46" stroke="rgba(255,248,231,0.5)" strokeWidth="2.5" strokeLinecap="round" />
            <line x1="16" y1="38" x2="32" y2="38" stroke="rgba(255,248,231,0.5)" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
        </div>
        <span className="avatar-name avatar-name--ghost">Invite someone</span>
      </button>
    );
  }

  return (
    <div
      className={`camper-avatar ${isPending ? 'camper-avatar--pending' : ''} ${isFailed ? 'camper-avatar--failed' : ''}`}
      title={isFailed && email ? `Failed to invite ${email}` : undefined}
      style={{
        transform: `translate(calc(${x}px - 50%), calc(${y}px - 50%))`,
        animationDelay: `${index * 0.1}s`,
      }}
    >
      {!displayName && (
        <span className={`avatar-name ${isFailed ? 'avatar-name--failed' : 'avatar-name--pending'}`}>
          {isFailed ? 'Failed' : 'Pending...'}
        </span>
      )}
      <div className="avatar-figure">
        <svg width="48" height="64" viewBox="0 0 48 64">
          {isPending ? <>
            {/* Ghost silhouette */}
            <circle cx="24" cy="14" r="11" fill="rgba(255,255,255,0.12)" />
            <ellipse cx="24" cy="10" rx="12" ry="8" fill="rgba(255,255,255,0.08)" />
            <circle cx="20" cy="14" r="1.5" fill="rgba(255,255,255,0.2)" />
            <circle cx="28" cy="14" r="1.5" fill="rgba(255,255,255,0.2)" />
            <text x="24" y="20" textAnchor="middle" fill="rgba(255,248,231,0.4)" fontSize="8" fontFamily="var(--font-body)" fontWeight="600">?</text>
            <rect x="13" y="26" width="22" height="24" rx="5" fill="rgba(255,255,255,0.08)" />
            <rect x="8" y="28" width="8" height="16" rx="4" fill="rgba(255,255,255,0.08)" />
            <rect x="32" y="28" width="8" height="16" rx="4" fill="rgba(255,255,255,0.08)" />
            <ellipse cx="19" cy="52" rx="6" ry="5" fill="rgba(255,255,255,0.06)" />
            <ellipse cx="29" cy="52" rx="6" ry="5" fill="rgba(255,255,255,0.06)" />
            <rect x="13" y="54" width="12" height="6" rx="3" fill="rgba(255,255,255,0.05)" />
            <rect x="23" y="54" width="12" height="6" rx="3" fill="rgba(255,255,255,0.05)" />
          </> : <>
            {/* Face */}
            <circle cx="24" cy="14" r="11" fill={color.skin} />
            {/* Hood / beanie */}
            <path d="M13,14 Q13,4 24,3 Q35,4 35,14" fill={color.hood} />
            <ellipse cx="24" cy="14" rx="12" ry="3" fill={color.hood} opacity="0.6" />
            {timeOfDay === 'night' ? <>
              {/* Brows — slightly furrowed, nervous */}
              <path d="M17,11 Q19.5,10 22,11.5" fill="none" stroke="#2A2A2A" strokeWidth="1.3" strokeLinecap="round" />
              <path d="M31,11 Q28.5,10 26,11.5" fill="none" stroke="#2A2A2A" strokeWidth="1.3" strokeLinecap="round" />
              {/* Eyes — a bit wider than normal, uneasy, shifting */}
              <ellipse cx="20" cy="13.5" rx="1.8" ry="1.8" fill="white" />
              <ellipse cx="28" cy="13.5" rx="1.8" ry="1.8" fill="white" />
              <g className="avatar-eyes-shift" style={{ animationDelay: `${(index * 2.3) % 6}s`, animationDuration: `${4 + seededRandom(index * 7 + 300) * 6}s` }}>
                <circle cx="20" cy="13.8" r="1.2" fill="#2A2A2A" />
                <circle cx="28" cy="13.8" r="1.2" fill="#2A2A2A" />
                <circle cx="20.3" cy="13.3" r="0.5" fill="white" />
                <circle cx="28.3" cy="13.3" r="0.5" fill="white" />
              </g>
              {/* Mouth — open grimace, inverted D */}
              <path d="M21,19.5 Q24,17 27,19.5" fill="#2A1A1A" />
              <path d="M21,19.5 L27,19.5" stroke="#2A1A1A" strokeWidth="1.2" strokeLinecap="round" fill="none" />
            </> : <>
              {/* Brows — stern, determined */}
              <path d="M17,11 L22,12.5" fill="none" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" />
              <path d="M31,11 L26,12.5" fill="none" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" />
              {/* Eyes — narrower, focused */}
              <ellipse cx="20" cy="14" rx="1.8" ry="1.4" fill="#2A2A2A" />
              <ellipse cx="28" cy="14" rx="1.8" ry="1.4" fill="#2A2A2A" />
              <circle cx="20.6" cy="13.6" r="0.6" fill="rgba(255,255,255,0.8)" />
              <circle cx="28.6" cy="13.6" r="0.6" fill="rgba(255,255,255,0.8)" />
              {/* Slight smirk — confident */}
              <path d="M21,18 Q24,19.5 27,18" fill="none" stroke="#3A2A2A" strokeWidth="1" strokeLinecap="round" />
            </>}
            {/* Stubble/chin detail */}
            <path d="M20,20 Q24,22 28,20" fill="none" stroke={color.skin} strokeWidth="0.5" opacity="0.4" />
            {/* Jacket / outdoor gear */}
            <rect x="13" y="26" width="22" height="24" rx="4" fill={color.body} />
            {/* Collar */}
            <path d="M15,26 L20,30 L24,28 L28,30 L33,26" fill={color.accent} />
            {/* Jacket center line */}
            <line x1="24" y1="30" x2="24" y2="50" stroke={color.hood} strokeWidth="1" opacity="0.4" />
            {/* Pocket */}
            <rect x="16" y="38" width="6" height="5" rx="1" fill="none" stroke={color.accent} strokeWidth="0.8" opacity="0.5" />
            {/* Arms — thicker, sturdier */}
            <rect x="7" y="28" width="9" height="16" rx="4.5" fill={color.body} />
            <rect x="32" y="28" width="9" height="16" rx="4.5" fill={color.body} />
            {/* Cuff detail */}
            <rect x="7" y="41" width="9" height="3" rx="1.5" fill={color.accent} opacity="0.6" />
            <rect x="32" y="41" width="9" height="3" rx="1.5" fill={color.accent} opacity="0.6" />
            {/* Legs (sitting) */}
            <ellipse cx="19" cy="52" rx="6" ry="5" fill={color.accent} />
            <ellipse cx="29" cy="52" rx="6" ry="5" fill={color.accent} />
            {/* Rugged boots */}
            <rect x="12" y="54" width="13" height="7" rx="3" fill="#3A2A1A" />
            <rect x="23" y="54" width="13" height="7" rx="3" fill="#3A2A1A" />
            {/* Boot sole detail */}
            <line x1="14" y1="59" x2="23" y2="59" stroke="#2A1A0A" strokeWidth="1" strokeLinecap="round" />
            <line x1="25" y1="59" x2="34" y2="59" stroke="#2A1A0A" strokeWidth="1" strokeLinecap="round" />
          </>}
        </svg>
      </div>
      {displayName && <span className="avatar-name">{displayName}</span>}
      {onRemove && (
        <button className="avatar-remove" onClick={onRemove} title="Remove from trip">
          <svg width="16" height="16" viewBox="0 0 16 16">
            <circle cx="8" cy="8" r="7" fill="rgba(232,93,58,0.8)" />
            <line x1="5" y1="5" x2="11" y2="11" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
            <line x1="11" y1="5" x2="5" y2="11" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </button>
      )}
      {!displayName && email && <span className="avatar-name avatar-name--email">{email}</span>}
      {/* Warm glow from fire on the character */}
      <div className="avatar-fire-glow" />
    </div>
  );
}
