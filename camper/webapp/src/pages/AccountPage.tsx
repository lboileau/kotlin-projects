import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
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

function formatAvatarProp(value: string): string {
  return value.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
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
                    <div className="account-avatar-props">
                      <span className="account-avatar-prop">Hair: {formatAvatarProp(avatar.hairStyle)}, {formatAvatarProp(avatar.hairColor)}</span>
                      <span className="account-avatar-prop">Skin: {formatAvatarProp(avatar.skinColor)}</span>
                      <span className="account-avatar-prop">Style: {formatAvatarProp(avatar.clothingStyle)}</span>
                      <span className="account-avatar-prop">Shirt: {formatAvatarProp(avatar.shirtColor)}</span>
                      <span className="account-avatar-prop">Pants: {formatAvatarProp(avatar.pantsColor)}</span>
                    </div>
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
