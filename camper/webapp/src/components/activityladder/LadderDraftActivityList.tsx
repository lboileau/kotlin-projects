import { useState } from 'react';
import type { LadderActivity } from '../../api/client';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { FormField } from '../ui/FormField';
import { LadderActivityCard } from './LadderActivityCard';
import './LadderDraftActivityList.css';

interface LadderDraftActivityListProps {
  activities: LadderActivity[];
  readonly?: boolean;
  /** For inline add form (creator-only) */
  onAdd?: (data: { name: string; imageUrl: string; distanceMinutes: number; costPerPerson: number }) => Promise<void>;
  onRemove?: (activityId: string) => Promise<void>;
  addError?: string;
}

export function LadderDraftActivityList({
  activities,
  readonly = false,
  onAdd,
  onRemove,
  addError,
}: LadderDraftActivityListProps) {
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [distanceMinutes, setDistanceMinutes] = useState('');
  const [costPerPerson, setCostPerPerson] = useState('');
  const [saving, setSaving] = useState(false);
  const [removing, setRemoving] = useState<string | null>(null);

  const handleAdd = async () => {
    if (!onAdd) return;
    const dist = parseInt(distanceMinutes, 10);
    const cost = parseFloat(costPerPerson);
    if (!name.trim() || !imageUrl.trim() || isNaN(dist) || isNaN(cost)) return;

    setSaving(true);
    try {
      await onAdd({ name: name.trim(), imageUrl: imageUrl.trim(), distanceMinutes: dist, costPerPerson: cost });
      setName('');
      setImageUrl('');
      setDistanceMinutes('');
      setCostPerPerson('');
      setShowForm(false);
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async (id: string) => {
    if (!onRemove) return;
    setRemoving(id);
    try {
      await onRemove(id);
    } finally {
      setRemoving(null);
    }
  };

  return (
    <div className="ladder-draft-list">
      <div className="ladder-draft-list__header">
        <h2 className="ladder-draft-list__title">
          Activities
          <span className="ladder-draft-list__count">{activities.length}</span>
        </h2>
        {!readonly && !showForm && (
          <Button variant="secondary" size="sm" onClick={() => setShowForm(true)}>
            + Add Activity
          </Button>
        )}
      </div>

      {activities.length === 0 && !showForm && (
        <p className="ladder-draft-list__empty">
          {readonly ? 'No activities added yet.' : 'Add at least 2 activities to start.'}
        </p>
      )}

      <ul className="ladder-draft-list__items">
        {activities.map((activity) => (
          <li key={activity.id} className="ladder-draft-list__item">
            <LadderActivityCard activity={activity} variant="display" />
            {!readonly && (
              <Button
                variant="danger"
                size="sm"
                onClick={() => handleRemove(activity.id)}
                loading={removing === activity.id}
                aria-label={`Remove ${activity.name}`}
                className="ladder-draft-list__remove-btn"
              >
                Remove
              </Button>
            )}
          </li>
        ))}
      </ul>

      {!readonly && showForm && (
        <div className="ladder-draft-list__add-form">
          <h3 className="ladder-draft-list__form-title">New Activity</h3>
          {addError && <p className="ladder-draft-list__error">{addError}</p>}
          <FormField label="Name">
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Rock Climbing"
            />
          </FormField>
          <FormField label="Image URL">
            <Input
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="https://..."
              type="url"
            />
          </FormField>
          <div className="ladder-draft-list__form-row">
            <FormField label="Distance (minutes)">
              <Input
                value={distanceMinutes}
                onChange={(e) => setDistanceMinutes(e.target.value)}
                placeholder="0"
                type="number"
                min="0"
              />
            </FormField>
            <FormField label="Cost per person ($)">
              <Input
                value={costPerPerson}
                onChange={(e) => setCostPerPerson(e.target.value)}
                placeholder="0.00"
                type="number"
                min="0"
                step="0.01"
              />
            </FormField>
          </div>
          <div className="ladder-draft-list__form-actions">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setShowForm(false);
                setName('');
                setImageUrl('');
                setDistanceMinutes('');
                setCostPerPerson('');
              }}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleAdd}
              loading={saving}
              disabled={!name.trim() || !imageUrl.trim()}
            >
              Add Activity
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
