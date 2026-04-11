import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type LadderActivityInput } from '../api/client';
import { AppHeader } from '../components/AppHeader';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { FormField } from '../components/ui/FormField';
import './NewActivityLadderPage.css';

interface DraftActivity extends LadderActivityInput {
  _key: number;
}

let keyCounter = 0;
function newKey() {
  return ++keyCounter;
}

function emptyDraft(): DraftActivity {
  return { _key: newKey(), name: '', imageUrl: '', distanceMinutes: 0, costPerPerson: 0 };
}

export function NewActivityLadderPage() {
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [activities, setActivities] = useState<DraftActivity[]>([emptyDraft(), emptyDraft()]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const updateActivity = (key: number, field: keyof LadderActivityInput, value: string) => {
    setActivities((prev) =>
      prev.map((a) => {
        if (a._key !== key) return a;
        if (field === 'distanceMinutes') return { ...a, distanceMinutes: parseInt(value, 10) || 0 };
        if (field === 'costPerPerson') return { ...a, costPerPerson: parseFloat(value) || 0 };
        return { ...a, [field]: value };
      }),
    );
  };

  const addActivity = () => {
    setActivities((prev) => [...prev, emptyDraft()]);
  };

  const removeActivity = (key: number) => {
    setActivities((prev) => prev.filter((a) => a._key !== key));
  };

  const validActivities = activities.filter(
    (a) =>
      a.name.trim().length > 0 &&
      /^https?:\/\//.test(a.imageUrl.trim()) &&
      a.distanceMinutes >= 0 &&
      a.costPerPerson >= 0,
  );
  const canSubmit = title.trim().length > 0 && validActivities.length >= 2;

  const missingReasons = (): string[] => {
    const reasons: string[] = [];
    if (title.trim().length === 0) reasons.push('Add a ladder title');
    activities.forEach((a, idx) => {
      const issues: string[] = [];
      if (a.name.trim().length === 0) issues.push('name');
      if (a.imageUrl.trim().length === 0) issues.push('image URL');
      else if (!/^https?:\/\//.test(a.imageUrl.trim())) issues.push('image URL must start with http:// or https://');
      if (a.distanceMinutes < 0) issues.push('distance ≥ 0');
      if (a.costPerPerson < 0) issues.push('cost ≥ 0');
      if (issues.length > 0) reasons.push(`Activity ${idx + 1}: ${issues.join(', ')}`);
    });
    if (validActivities.length < 2 && reasons.length === 0) {
      reasons.push('Add at least 2 valid activities');
    }
    return reasons;
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError('');
    try {
      const payload = {
        title: title.trim(),
        activities: validActivities.map(({ name, imageUrl, distanceMinutes, costPerPerson }) => ({
          name: name.trim(),
          imageUrl: imageUrl.trim(),
          distanceMinutes,
          costPerPerson,
        })),
      };
      const res = await api.ladders.create(payload);
      navigate(`/activities/${res.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create ladder.');
      setSubmitting(false);
    }
  };

  return (
    <div className="new-ladder-page">
      <ParallaxBackground variant="dusk" />
      <div className="new-ladder-content">
        <AppHeader
          pageTitle="New Activity Ladder"
          pageIcon={
            <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
              <polygon points="8,1.5 10,6 15,6 11,9.5 12.5,15 8,12 3.5,15 5,9.5 1,6 6,6" fill="none" stroke="currentColor" strokeWidth="1.2" />
            </svg>
          }
        />

        <main className="new-ladder-main">
          <div className="new-ladder-form">
            <FormField label="Ladder Title">
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Weekend Adventure Vote"
                maxLength={200}
              />
            </FormField>

            <div className="new-ladder-activities">
              <div className="new-ladder-activities__header">
                <h2 className="new-ladder-activities__title">Activities</h2>
                <span className="new-ladder-activities__hint">Add at least 2 to start the ladder</span>
              </div>

              <div className="new-ladder-activities__list">
                {activities.map((activity, idx) => (
                  <div key={activity._key} className="new-ladder-activity-row">
                    <div className="new-ladder-activity-row__num">{idx + 1}</div>
                    <div className="new-ladder-activity-row__fields">
                      <FormField label="Name">
                        <Input
                          value={activity.name}
                          onChange={(e) => updateActivity(activity._key, 'name', e.target.value)}
                          placeholder="Activity name"
                          maxLength={200}
                        />
                      </FormField>
                      <FormField label="Image URL">
                        <Input
                          value={activity.imageUrl}
                          onChange={(e) => updateActivity(activity._key, 'imageUrl', e.target.value)}
                          placeholder="https://..."
                          type="url"
                        />
                      </FormField>
                      <div className="new-ladder-activity-row__row2">
                        <FormField label="Distance (min)">
                          <Input
                            value={activity.distanceMinutes === 0 ? '' : String(activity.distanceMinutes)}
                            onChange={(e) => updateActivity(activity._key, 'distanceMinutes', e.target.value)}
                            placeholder="0"
                            type="number"
                            min="0"
                          />
                        </FormField>
                        <FormField label="Cost per person ($)">
                          <Input
                            value={activity.costPerPerson === 0 ? '' : String(activity.costPerPerson)}
                            onChange={(e) => updateActivity(activity._key, 'costPerPerson', e.target.value)}
                            placeholder="0.00"
                            type="number"
                            min="0"
                            step="0.01"
                          />
                        </FormField>
                      </div>
                    </div>
                    {activities.length > 2 && (
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => removeActivity(activity._key)}
                        aria-label={`Remove activity ${idx + 1}`}
                        className="new-ladder-activity-row__remove"
                      >
                        ×
                      </Button>
                    )}
                  </div>
                ))}
              </div>

              <Button variant="ghost" size="sm" onClick={addActivity} className="new-ladder-add-btn">
                + Add another activity
              </Button>
            </div>

            {error && <p className="new-ladder-error">{error}</p>}

            {!canSubmit && !submitting && (
              <ul className="new-ladder-missing">
                {missingReasons().map((r) => (
                  <li key={r}>{r}</li>
                ))}
              </ul>
            )}

            <div className="new-ladder-actions">
              <Button variant="secondary" onClick={() => navigate('/activities')}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleSubmit}
                loading={submitting}
                disabled={!canSubmit}
              >
                Create Ladder
              </Button>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
