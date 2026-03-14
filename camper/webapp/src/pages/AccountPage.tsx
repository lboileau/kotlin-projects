import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type AvatarResponse } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AvatarHair } from '../components/AvatarHair';
import './AccountPage.css';

const DIETARY_OPTIONS = [
  { value: 'gluten_free', label: 'Gluten Free' },
  { value: 'nut_allergy', label: 'Nut Allergy' },
  { value: 'vegetarian', label: 'Vegetarian' },
  { value: 'vegan', label: 'Vegan' },
  { value: 'lactose_intolerant', label: 'Lactose Intolerant' },
  { value: 'shellfish_allergy', label: 'Shellfish Allergy' },
  { value: 'halal', label: 'Halal' },
  { value: 'kosher', label: 'Kosher' },
];

const EXPERIENCE_OPTIONS = [
  { value: 'beginner', label: 'Beginner' },
  { value: 'intermediate', label: 'Intermediate' },
  { value: 'advanced', label: 'Advanced' },
  { value: 'expert', label: 'Expert' },
];

const SKIN_COLORS: Record<string, string> = {
  light: '#F5D6B8', fair: '#F0C8A0', medium: '#D4A574', olive: '#C4946A',
  tan: '#B8845A', brown: '#8B6B4A', dark: '#6A4A2A', deep: '#4A3020',
};
const HAIR_COLORS: Record<string, string> = {
  black: '#1A1A1A', brown: '#4A3020', blonde: '#D4B870', red: '#8B3A1A',
  gray: '#8A8A8A', white: '#E8E0D0', auburn: '#6A3A20', platinum: '#E8D8C0',
};
const SHIRT_COLORS: Record<string, string> = {
  red: '#B83A2A', blue: '#3A5A8A', green: '#3A6A3A', yellow: '#C4A030',
  orange: '#C06A20', purple: '#6A3A8A', white: '#E8E0D0', teal: '#2A6A6A',
};
const PANTS_COLORS: Record<string, string> = {
  black: '#2A2A2A', navy: '#2A3A5A', khaki: '#C4B090', olive: '#5A6A3A',
  brown: '#5A4030', gray: '#7A7A7A', denim: '#4A5A7A', charcoal: '#3A3A3A',
};

function AvatarPreviewSvg({ avatar }: { avatar: AvatarResponse }) {
  const skin = SKIN_COLORS[avatar.skinColor] || '#D4A574';
  const hood = HAIR_COLORS[avatar.hairColor] || '#4A3020';
  const body = SHIRT_COLORS[avatar.shirtColor] || '#3A5A8A';
  const accent = PANTS_COLORS[avatar.pantsColor] || '#2A3A5A';
  return (
    <svg width="80" height="106" viewBox="0 0 48 64" className="account-avatar-preview-svg">
      <circle cx="24" cy="14" r="11" fill={skin} />
      <AvatarHair style={avatar.hairStyle} color={hood} />
      <ellipse cx="20" cy="14" rx="1.8" ry="1.4" fill="#2A2A2A" />
      <ellipse cx="28" cy="14" rx="1.8" ry="1.4" fill="#2A2A2A" />
      <circle cx="20.6" cy="13.6" r="0.6" fill="rgba(255,255,255,0.8)" />
      <circle cx="28.6" cy="13.6" r="0.6" fill="rgba(255,255,255,0.8)" />
      <path d="M21,18 Q24,19.5 27,18" fill="none" stroke="#3A2A2A" strokeWidth="1" strokeLinecap="round" />
      <rect x="13" y="26" width="22" height="24" rx="4" fill={body} />
      <path d="M15,26 L20,30 L24,28 L28,30 L33,26" fill={accent} />
      <line x1="24" y1="30" x2="24" y2="50" stroke={hood} strokeWidth="1" opacity="0.4" />
      <rect x="7" y="28" width="9" height="16" rx="4.5" fill={body} />
      <rect x="32" y="28" width="9" height="16" rx="4.5" fill={body} />
      <ellipse cx="19" cy="52" rx="6" ry="5" fill={accent} />
      <ellipse cx="29" cy="52" rx="6" ry="5" fill={accent} />
      <rect x="12" y="54" width="13" height="7" rx="3" fill="#3A2A1A" />
      <rect x="23" y="54" width="13" height="7" rx="3" fill="#3A2A1A" />
    </svg>
  );
}

export function AccountPage() {
  const { user, login, logout } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState(user?.username || '');
  const [experienceLevel, setExperienceLevel] = useState(user?.experienceLevel || '');
  const [dietaryRestrictions, setDietaryRestrictions] = useState<string[]>(user?.dietaryRestrictions || []);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');
  const [randomizing, setRandomizing] = useState(false);

  const isDirty =
    username.trim() !== (user?.username || '') ||
    experienceLevel !== (user?.experienceLevel || '') ||
    JSON.stringify([...dietaryRestrictions].sort()) !== JSON.stringify([...(user?.dietaryRestrictions || [])].sort());

  const toggleDietary = (value: string) => {
    setDietaryRestrictions(prev =>
      prev.includes(value) ? prev.filter(v => v !== value) : [...prev, value]
    );
  };

  const handleRandomizeAvatar = async () => {
    if (!user) return;
    setRandomizing(true);
    try {
      const updated = await api.randomizeAvatar(user.id);
      login(updated);
    } catch {
      // silently fail
    } finally {
      setRandomizing(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !isDirty) return;
    setSaving(true);
    setError('');
    setSaved(false);
    try {
      const updated = await api.updateUser(user.id, {
        username: username.trim(),
        experienceLevel: experienceLevel || null,
        dietaryRestrictions,
      });
      login(updated);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const avatar = user?.avatar;

  return (
    <div className="account-page">
      <ParallaxBackground variant="dusk" />

      <div className="account-content">
        <header className="account-header">
          <button className="account-back" onClick={() => navigate(-1)}>
            <svg width="20" height="20" viewBox="0 0 20 20">
              <path d="M13,4 L7,10 L13,16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            Back
          </button>
          <div className="account-header-center">
            <svg width="24" height="24" viewBox="0 0 24 24" className="account-header-icon">
              <polygon points="12,2 4,20 20,20" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
              <path d="M10,16 Q11,12 12,10 Q13,12 14,16" fill="var(--ember)" opacity="0.6" />
            </svg>
            <span className="account-header-title">Camper</span>
          </div>
          <div className="account-header-user">
            <div className="account-user-btn">
              <svg width="28" height="28" viewBox="0 0 28 28" className="account-user-avatar">
                <defs><clipPath id="avatar-clip-account"><circle cx="14" cy="14" r="13" /></clipPath></defs>
                <circle cx="14" cy="14" r="13" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="1.5" />
                <g clipPath="url(#avatar-clip-account)">
                  <circle cx="14" cy="11" r="5" fill="var(--parchment)" />
                  <ellipse cx="14" cy="24" rx="8" ry="6" fill="var(--parchment)" />
                </g>
              </svg>
              <span className="account-user-name">{user?.username || user?.email}</span>
            </div>
            <button className="account-logout" onClick={logout}>Log Out</button>
          </div>
        </header>

        <div className="account-card-wrapper">
          <div className="account-card">
            <button className="account-close" onClick={() => navigate(-1)} title="Close">
              <svg width="20" height="20" viewBox="0 0 20 20">
                <line x1="4" y1="4" x2="16" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <line x1="16" y1="4" x2="4" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            <div className="account-avatar">
              <svg width="72" height="72" viewBox="0 0 72 72">
                <defs><clipPath id="avatar-clip-large"><circle cx="36" cy="36" r="34" /></clipPath></defs>
                <circle cx="36" cy="36" r="34" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="2" />
                <g clipPath="url(#avatar-clip-large)">
                  <circle cx="36" cy="28" r="12" fill="var(--parchment)" />
                  <ellipse cx="36" cy="56" rx="18" ry="14" fill="var(--parchment)" />
                </g>
              </svg>
            </div>
            <h1 className="account-title">Your Camp Profile</h1>

            <form className="account-form" onSubmit={handleSave}>
              <div className="account-field">
                <label className="account-label">Email</label>
                <input
                  className="account-input account-input--readonly"
                  value={user?.email || ''}
                  readOnly
                />
              </div>

              <div className="account-field">
                <label className="account-label">Trail Name</label>
                <input
                  className="account-input"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="Choose your trail name..."
                />
              </div>

              <div className="account-field">
                <label className="account-label">Experience Level</label>
                <select
                  className="account-input account-select"
                  value={experienceLevel}
                  onChange={e => setExperienceLevel(e.target.value)}
                >
                  <option value="">Not set</option>
                  {EXPERIENCE_OPTIONS.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>

              <div className="account-field">
                <label className="account-label">Dietary Restrictions</label>
                <div className="account-checkbox-grid">
                  {DIETARY_OPTIONS.map(opt => (
                    <label key={opt.value} className="account-checkbox-item">
                      <input
                        type="checkbox"
                        checked={dietaryRestrictions.includes(opt.value)}
                        onChange={() => toggleDietary(opt.value)}
                      />
                      <span className="account-checkbox-label">{opt.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div className="account-field">
                <label className="account-label">Your Avatar</label>
                {avatar ? (
                  <div className="account-avatar-details">
                    <AvatarPreviewSvg avatar={avatar} />
                    <button
                      type="button"
                      className="account-randomize-btn"
                      onClick={handleRandomizeAvatar}
                      disabled={randomizing}
                    >
                      {randomizing ? 'Randomizing...' : 'Randomize Avatar'}
                    </button>
                  </div>
                ) : (
                  <p className="account-avatar-placeholder">No avatar yet — one will be generated for you.</p>
                )}
              </div>

              {error && <p className="account-error">{error}</p>}

              <button
                type="submit"
                className="account-save"
                disabled={saving || !isDirty || !username.trim()}
              >
                {saving ? 'Saving...' : saved ? 'Saved!' : 'Update Profile'}
              </button>
            </form>

            <p className="account-meta">
              Adventurer since {user ? new Date(user.createdAt).toLocaleDateString('en-US', {
                month: 'long', day: 'numeric', year: 'numeric'
              }) : ''}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
