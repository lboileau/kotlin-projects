import { useState } from 'react';
import { api, type User, type AvatarResponse } from '../api/client';
import { DIETARY_OPTIONS, EXPERIENCE_OPTIONS } from '../lib/profileConstants';
import { SKIN_COLORS, HAIR_COLORS, SHIRT_COLORS, PANTS_COLORS } from '../lib/avatarConstants';
import { AvatarHair } from './AvatarHair';
import { AvatarHead } from './AvatarHead';
import { Input } from './ui/Input';
import { Select } from './ui/Select';
import { CheckboxGroup } from './ui/CheckboxGroup';
import { FormField } from './ui/FormField';
import { Button } from './ui/Button';
import './ProfileSetupModal.css';

function AvatarPreview({ avatar }: { avatar: AvatarResponse | null }) {
  if (!avatar) return <p className="setup-avatar-placeholder">No avatar generated yet.</p>;
  const skin = SKIN_COLORS[avatar.skinColor] || '#D4A574';
  const hood = HAIR_COLORS[avatar.hairColor] || '#4A3020';
  const body = SHIRT_COLORS[avatar.shirtColor] || '#3A5A8A';
  const accent = PANTS_COLORS[avatar.pantsColor] || '#2A3A5A';
  return (
    <svg width="80" height="106" viewBox="0 0 48 64" className="setup-avatar-preview-svg">
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

interface Props {
  isOpen: boolean;
  user: User;
  onComplete: (updatedUser: User) => void;
  onClose?: () => void;
}

export function ProfileSetupModal({ isOpen, user, onComplete, onClose }: Props) {
  const isEditMode = user.profileCompleted;
  const [username, setUsername] = useState(user.username || '');
  const [experienceLevel, setExperienceLevel] = useState(user.experienceLevel || '');
  const [dietaryRestrictions, setDietaryRestrictions] = useState<string[]>(user.dietaryRestrictions || []);
  const [avatar, setAvatar] = useState<AvatarResponse | null>(user.avatar);
  const [submitting, setSubmitting] = useState(false);
  const [randomizing, setRandomizing] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

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
        username: username.trim(),
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
    <div className="modal-overlay" onClick={isEditMode ? onClose : undefined}>
      <div className="modal-content setup-modal" onClick={e => e.stopPropagation()}>
        {isEditMode && onClose && (
          <button className="modal-close-btn" onClick={onClose} title="Close">
            <svg width="20" height="20" viewBox="0 0 20 20">
              <line x1="4" y1="4" x2="16" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              <line x1="16" y1="4" x2="4" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        )}
        <div className="setup-header">
          {isEditMode ? (
            <AvatarHead avatar={avatar} size={56} />
          ) : (
            <svg width="48" height="48" viewBox="0 0 48 48" className="setup-icon">
              <polygon points="24,4 8,40 40,40" fill="none" stroke="var(--ember)" strokeWidth="2" />
              <path d="M20,32 Q22,24 24,20 Q26,24 28,32" fill="var(--ember)" opacity="0.7" />
              <circle cx="16" cy="14" r="1.5" fill="var(--butter)" opacity="0.6" />
              <circle cx="34" cy="18" r="1" fill="var(--butter)" opacity="0.4" />
            </svg>
          )}
          <h2 className="modal-title">{isEditMode ? 'Camp Profile' : 'Welcome, Adventurer!'}</h2>
          <p className="modal-flavor">{isEditMode ? 'Update your camp profile.' : 'Set up your camp profile before heading out.'}</p>
        </div>

        <div className="modal-divider">
          <svg width="120" height="12" viewBox="0 0 120 12">
            <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
          </svg>
        </div>

        <div className="setup-form">
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
              options={EXPERIENCE_OPTIONS}
              placeholder="Select your level..."
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
            <AvatarPreview avatar={avatar} />
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleRandomize}
              disabled={randomizing}
            >
              {randomizing ? 'Randomizing...' : 'Randomize Avatar'}
            </Button>
          </FormField>

          {error && <p className="modal-error">{error}</p>}

          <Button
            size="lg"
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? 'Saving...' : isEditMode ? 'Save Profile' : 'Start Adventuring'}
          </Button>
        </div>
      </div>
    </div>
  );
}
