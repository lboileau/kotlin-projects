import { useState } from 'react';
import { api, type User, type AvatarResponse } from '../api/client';
import { DIETARY_OPTIONS, EXPERIENCE_OPTIONS } from '../lib/profileConstants';
import { AvatarPreview } from './AvatarPreview';
import { AvatarHead } from './AvatarHead';
import { Input } from './ui/Input';
import { Select } from './ui/Select';
import { CheckboxGroup } from './ui/CheckboxGroup';
import { FormField } from './ui/FormField';
import { Button } from './ui/Button';
import { Modal } from './ui/Modal';
import './ProfileSetupModal.css';

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
    <Modal isOpen={isOpen} onClose={onClose || (() => {})} closable={isEditMode && !!onClose} className="setup-modal">
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
    </Modal>
  );
}
