import { useState } from 'react';
import { api, type User, type AvatarResponse } from '../api/client';
import './ProfileSetupModal.css';

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

function AvatarPreview({ avatar }: { avatar: AvatarResponse | null }) {
  if (!avatar) return <p className="setup-avatar-placeholder">No avatar generated yet.</p>;
  return (
    <div className="setup-avatar-props">
      <span className="setup-avatar-prop">Hair: {formatAvatarProp(avatar.hairStyle)}, {formatAvatarProp(avatar.hairColor)}</span>
      <span className="setup-avatar-prop">Skin: {formatAvatarProp(avatar.skinColor)}</span>
      <span className="setup-avatar-prop">Style: {formatAvatarProp(avatar.clothingStyle)}</span>
      <span className="setup-avatar-prop">Shirt: {formatAvatarProp(avatar.shirtColor)}</span>
      <span className="setup-avatar-prop">Pants: {formatAvatarProp(avatar.pantsColor)}</span>
    </div>
  );
}

interface Props {
  isOpen: boolean;
  user: User;
  onComplete: (updatedUser: User) => void;
}

export function ProfileSetupModal({ isOpen, user, onComplete }: Props) {
  const [experienceLevel, setExperienceLevel] = useState('');
  const [dietaryRestrictions, setDietaryRestrictions] = useState<string[]>([]);
  const [avatar, setAvatar] = useState<AvatarResponse | null>(user.avatar);
  const [submitting, setSubmitting] = useState(false);
  const [randomizing, setRandomizing] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const toggleDietary = (value: string) => {
    setDietaryRestrictions(prev =>
      prev.includes(value) ? prev.filter(v => v !== value) : [...prev, value]
    );
  };

  const handleRandomize = async () => {
    setRandomizing(true);
    try {
      const updated = await api.randomizeAvatar(user.id);
      setAvatar(updated.avatar);
    } catch {
      // silently fail
    } finally {
      setRandomizing(false);
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const updated = await api.updateUser(user.id, {
        username: user.username || '',
        experienceLevel: experienceLevel || null,
        dietaryRestrictions,
        profileCompleted: true,
      });
      onComplete(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save profile');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content setup-modal">
        <div className="setup-header">
          <svg width="48" height="48" viewBox="0 0 48 48" className="setup-icon">
            <polygon points="24,4 8,40 40,40" fill="none" stroke="var(--ember)" strokeWidth="2" />
            <path d="M20,32 Q22,24 24,20 Q26,24 28,32" fill="var(--ember)" opacity="0.7" />
            <circle cx="16" cy="14" r="1.5" fill="var(--butter)" opacity="0.6" />
            <circle cx="34" cy="18" r="1" fill="var(--butter)" opacity="0.4" />
          </svg>
          <h2 className="modal-title">Welcome, Adventurer!</h2>
          <p className="modal-flavor">Set up your camp profile before heading out.</p>
        </div>

        <div className="modal-divider">
          <svg width="120" height="12" viewBox="0 0 120 12">
            <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
          </svg>
        </div>

        <div className="setup-form">
          <div className="setup-field">
            <label className="setup-label">Experience Level</label>
            <select
              className="modal-input setup-select"
              value={experienceLevel}
              onChange={e => setExperienceLevel(e.target.value)}
            >
              <option value="">Select your level...</option>
              {EXPERIENCE_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div className="setup-field">
            <label className="setup-label">Dietary Restrictions</label>
            <div className="setup-checkbox-grid">
              {DIETARY_OPTIONS.map(opt => (
                <label key={opt.value} className="setup-checkbox-item">
                  <input
                    type="checkbox"
                    checked={dietaryRestrictions.includes(opt.value)}
                    onChange={() => toggleDietary(opt.value)}
                  />
                  <span>{opt.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="setup-field">
            <label className="setup-label">Your Avatar</label>
            <AvatarPreview avatar={avatar} />
            <button
              type="button"
              className="setup-randomize-btn"
              onClick={handleRandomize}
              disabled={randomizing}
            >
              {randomizing ? 'Randomizing...' : 'Randomize Avatar'}
            </button>
          </div>

          {error && <p className="modal-error">{error}</p>}

          <button
            className="modal-btn setup-submit"
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? 'Saving...' : 'Start Adventuring'}
          </button>
        </div>
      </div>
    </div>
  );
}
