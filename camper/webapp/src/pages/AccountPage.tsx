import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { DIETARY_OPTIONS, EXPERIENCE_OPTIONS } from '../lib/profileConstants';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AvatarPreview } from '../components/AvatarPreview';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { CheckboxGroup } from '../components/ui/CheckboxGroup';
import { FormField } from '../components/ui/FormField';
import { Button } from '../components/ui/Button';
import './AccountPage.css';

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
              <FormField label="Email">
                <Input
                  className="account-input--readonly"
                  value={user?.email || ''}
                  readOnly
                />
              </FormField>

              <FormField label="Trail Name">
                <Input
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="Choose your trail name..."
                />
              </FormField>

              <FormField label="Experience Level">
                <Select
                  value={experienceLevel}
                  onChange={e => setExperienceLevel(e.target.value)}
                  options={[{ value: '', label: 'Not set' }, ...EXPERIENCE_OPTIONS]}
                />
              </FormField>

              <FormField label="Dietary Restrictions">
                <CheckboxGroup
                  options={DIETARY_OPTIONS}
                  selected={dietaryRestrictions}
                  onChange={setDietaryRestrictions}
                />
              </FormField>

              <FormField label="Your Avatar">
                {avatar ? (
                  <div className="account-avatar-details">
                    <AvatarPreview avatar={avatar} />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={handleRandomizeAvatar}
                      disabled={randomizing}
                    >
                      {randomizing ? 'Randomizing...' : 'Randomize Avatar'}
                    </Button>
                  </div>
                ) : (
                  <p className="account-avatar-placeholder">No avatar yet — one will be generated for you.</p>
                )}
              </FormField>

              {error && <p className="account-error">{error}</p>}

              <Button
                type="submit"
                disabled={saving || !isDirty || !username.trim()}
              >
                {saving ? 'Saving...' : saved ? 'Saved!' : 'Update Profile'}
              </Button>
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
