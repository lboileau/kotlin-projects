import { useState } from 'react';
import { api, type User, type AvatarResponse } from '../api/client';
import { DIETARY_OPTIONS, EXPERIENCE_OPTIONS } from '../lib/profileConstants';
import { AvatarPreview } from './AvatarPreview';
import { Input } from './ui/Input';
import { Select } from './ui/Select';
import { CheckboxGroup } from './ui/CheckboxGroup';
import { FormField } from './ui/FormField';
import { Button } from './ui/Button';
import './ProfileForm.css';

interface ProfileFormProps {
  user: User;
  onSave: (updatedUser: User) => void;
  submitLabel?: string;
  showEmail?: boolean;
  markProfileCompleted?: boolean;
}

export function ProfileForm({ user, onSave, submitLabel = 'Save Profile', showEmail, markProfileCompleted }: ProfileFormProps) {
  const [username, setUsername] = useState(user.username || '');
  const [experienceLevel, setExperienceLevel] = useState(user.experienceLevel || '');
  const [dietaryRestrictions, setDietaryRestrictions] = useState<string[]>(user.dietaryRestrictions || []);
  const [avatar, setAvatar] = useState<AvatarResponse | null>(user.avatar);
  const [avatarSeed, setAvatarSeed] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [randomizing, setRandomizing] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  const isDirty =
    avatarSeed !== null ||
    username.trim() !== (user.username || '') ||
    experienceLevel !== (user.experienceLevel || '') ||
    JSON.stringify([...dietaryRestrictions].sort()) !== JSON.stringify([...(user.dietaryRestrictions || [])].sort());

  const handleRandomize = async () => {
    setRandomizing(true);
    try {
      const preview = await api.randomizeAvatar(user.id);
      setAvatar(preview.avatar);
      setAvatarSeed(preview.seed);
    } catch {
      // silently fail
    } finally {
      setRandomizing(false);
    }
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!isDirty && !markProfileCompleted) return;
    setSubmitting(true);
    setError('');
    setSaved(false);
    try {
      const updated = await api.updateUser(user.id, {
        username: username.trim(),
        experienceLevel: experienceLevel || null,
        dietaryRestrictions,
        ...(markProfileCompleted ? { profileCompleted: true } : {}),
        ...(avatarSeed ? { avatarSeed } : {}),
      });
      onSave(updated);
      setAvatarSeed(null);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save profile');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="profile-form" onSubmit={handleSubmit}>
      {showEmail && (
        <FormField label="Email">
          <Input
            className="profile-form__input--readonly"
            value={user.email || ''}
            readOnly
          />
        </FormField>
      )}

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
          options={showEmail ? [{ value: '', label: 'Not set' }, ...EXPERIENCE_OPTIONS] : EXPERIENCE_OPTIONS}
          placeholder={showEmail ? undefined : 'Select your level...'}
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
          <div className="profile-form__avatar-details">
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
          </div>
        ) : (
          <p className="profile-form__avatar-placeholder">No avatar yet — one will be generated for you.</p>
        )}
      </FormField>

      {error && <p className="profile-form__error">{error}</p>}

      <Button
        type="submit"
        size={showEmail ? undefined : 'lg'}
        disabled={submitting || (!isDirty && !markProfileCompleted) || !username.trim()}
      >
        {submitting ? 'Saving...' : saved ? 'Saved!' : submitLabel}
      </Button>
    </form>
  );
}
