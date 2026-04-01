import { useState, useEffect, useCallback } from 'react';
import { api, type ItineraryEvent, type Itinerary } from '../api/client';
import { Button } from './ui/Button';
import { Input } from './ui/Input';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';
import './ItineraryModal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  isOwner: boolean;
  refreshKey?: number;
}

interface LinkFormData {
  url: string;
  label: string;
}

interface EventFormData {
  title: string;
  description: string;
  details: string;
  eventAt: string;
  category: string;
  estimatedCost: string;
  location: string;
  eventEndAt: string;
  links: LinkFormData[];
}

const EMPTY_FORM: EventFormData = {
  title: '',
  description: '',
  details: '',
  eventAt: '',
  category: 'other',
  estimatedCost: '',
  location: '',
  eventEndAt: '',
  links: [],
};

const CATEGORIES = [
  { value: 'travel', icon: '\u{1F697}', label: 'Travel' },
  { value: 'accommodation', icon: '\u{1F3E0}', label: 'Lodging' },
  { value: 'activity', icon: '\u{1F97E}', label: 'Activity' },
  { value: 'meal', icon: '\u{1F37D}\u{FE0F}', label: 'Meal' },
  { value: 'other', icon: '\u{1F4CC}', label: 'Other' },
] as const;

const CATEGORY_ICON_MAP: Record<string, string> = {
  travel: '\u{1F697}',
  accommodation: '\u{1F3E0}',
  activity: '\u{1F97E}',
  meal: '\u{1F37D}\u{FE0F}',
  other: '\u{1F4CC}',
};

const CATEGORY_LABEL_MAP: Record<string, string> = {
  travel: 'Travel',
  accommodation: 'Lodging',
  activity: 'Activity',
  meal: 'Meal',
  other: 'Other',
};

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  return d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
}

function toLocalDatetime(dateStr: string): string {
  const d = new Date(dateStr);
  const offset = d.getTimezoneOffset();
  const local = new Date(d.getTime() - offset * 60000);
  return local.toISOString().slice(0, 16);
}

function formatCost(cost: number | null): string {
  if (cost === null || cost === undefined) return '\u2014';
  return `$${cost.toFixed(2)}`;
}

function groupEventsByDate(events: ItineraryEvent[]): Map<string, ItineraryEvent[]> {
  const groups = new Map<string, ItineraryEvent[]>();
  for (const event of events) {
    const dateKey = formatDate(event.eventAt);
    const existing = groups.get(dateKey) || [];
    existing.push(event);
    groups.set(dateKey, existing);
  }
  return groups;
}

function getDayCost(events: ItineraryEvent[]): number | null {
  const costs = events.map(e => e.estimatedCost).filter((c): c is number => c !== null);
  return costs.length > 0 ? costs.reduce((a, b) => a + b, 0) : null;
}

function getLatestEventDate(events: ItineraryEvent[]): string {
  if (events.length === 0) return '';
  const sorted = [...events].sort((a, b) => new Date(b.eventAt).getTime() - new Date(a.eventAt).getTime());
  const latest = new Date(sorted[0].eventAt);
  latest.setHours(12, 0, 0, 0);
  return toLocalDatetime(latest.toISOString());
}

function getDayNumber(date: string, allDates: string[]): number {
  return allDates.indexOf(date) + 1;
}

export function ItineraryModal({ isOpen, onClose, planId, isOwner, refreshKey }: Props) {
  const [itinerary, setItinerary] = useState<Itinerary | null>(null);
  const [events, setEvents] = useState<ItineraryEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingEvent, setEditingEvent] = useState<ItineraryEvent | null>(null);
  const [form, setForm] = useState<EventFormData>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const loadEvents = useCallback(async () => {
    try {
      const result = await api.getItinerary(planId);
      setItinerary(result);
      setEvents(result.events);
    } catch {
      setItinerary(null);
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    if (isOpen) loadEvents();
  }, [isOpen, loadEvents]);

  // Live updates: refetch when refreshKey changes while modal is open
  useEffect(() => {
    if (isOpen && refreshKey !== undefined && refreshKey > 0) {
      loadEvents();
    }
  }, [refreshKey]); // eslint-disable-line react-hooks/exhaustive-deps

  const resetForm = () => {
    setForm(EMPTY_FORM);
    setShowForm(false);
    setEditingEvent(null);
    setError('');
  };

  const openAddForm = () => {
    const autoDate = getLatestEventDate(events);
    setForm({ ...EMPTY_FORM, eventAt: autoDate });
    setEditingEvent(null);
    setShowForm(true);
    setError('');
  };

  const handleEdit = (event: ItineraryEvent) => {
    setEditingEvent(event);
    setForm({
      title: event.title,
      description: event.description || '',
      details: event.details || '',
      eventAt: toLocalDatetime(event.eventAt),
      category: event.category,
      estimatedCost: event.estimatedCost !== null ? String(event.estimatedCost) : '',
      location: event.location || '',
      eventEndAt: event.eventEndAt ? toLocalDatetime(event.eventEndAt) : '',
      links: event.links.map(l => ({ url: l.url, label: l.label || '' })),
    });
    setShowForm(true);
    setError('');
  };

  const handleAddLink = () => {
    if (form.links.length >= 10) return;
    setForm(f => ({ ...f, links: [...f.links, { url: '', label: '' }] }));
  };

  const handleRemoveLink = (index: number) => {
    setForm(f => ({ ...f, links: f.links.filter((_, i) => i !== index) }));
  };

  const handleLinkChange = (index: number, field: 'url' | 'label', value: string) => {
    setForm(f => ({
      ...f,
      links: f.links.map((l, i) => i === index ? { ...l, [field]: value } : l),
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) { setError('Title is required'); return; }
    if (!form.eventAt) { setError('Date & time is required'); return; }

    const cost = form.estimatedCost.trim() ? parseFloat(form.estimatedCost) : null;
    if (cost !== null && (isNaN(cost) || cost < 0)) { setError('Cost must be a positive number'); return; }

    if (form.eventEndAt && new Date(form.eventEndAt) <= new Date(form.eventAt)) {
      setError('End time must be after start time');
      return;
    }

    for (const link of form.links) {
      if (!link.url.trim()) { setError('Link URL must not be empty'); return; }
    }

    setSaving(true);
    setError('');
    try {
      const data = {
        title: form.title.trim(),
        description: form.description.trim() || null,
        details: form.details.trim() || null,
        eventAt: new Date(form.eventAt).toISOString(),
        category: form.category,
        estimatedCost: cost,
        location: form.location.trim() || null,
        eventEndAt: form.eventEndAt ? new Date(form.eventEndAt).toISOString() : null,
        links: form.links.length > 0
          ? form.links.map(l => ({ url: l.url.trim(), label: l.label.trim() || null }))
          : null,
      };
      if (editingEvent) {
        await api.updateEvent(planId, editingEvent.id, data);
      } else {
        await api.addEvent(planId, data);
      }
      resetForm();
      await loadEvents();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (eventId: string) => {
    setDeletingId(eventId);
    try {
      await api.deleteEvent(planId, eventId);
      await loadEvents();
    } finally {
      setDeletingId(null);
    }
  };

  const grouped = groupEventsByDate(events);
  const allDates = Array.from(grouped.keys());

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg" className="itinerary-modal">
        {/* Header */}
        <div className="itinerary-header">
          <div className="itinerary-header-icon">
            <svg width="36" height="36" viewBox="0 0 36 36">
              <rect x="4" y="6" width="28" height="24" rx="2" fill="var(--parchment)" stroke="var(--tan-deep)" strokeWidth="1.5" />
              <path d="M8,14 Q14,10 20,16 Q26,22 32,14" fill="none" stroke="var(--rose-deep)" strokeWidth="1.5" strokeDasharray="3,2" />
              <circle cx="12" cy="12" r="1.5" fill="var(--sage)" />
              <circle cx="28" cy="16" r="1.5" fill="var(--flame)" />
              <line x1="8" y1="22" x2="20" y2="22" stroke="var(--tan-deep)" strokeWidth="0.8" opacity="0.5" />
              <line x1="8" y1="25" x2="16" y2="25" stroke="var(--tan-deep)" strokeWidth="0.8" opacity="0.4" />
            </svg>
          </div>
          <h2 className="modal-title">Trail Map & Itinerary</h2>
          <div className="modal-divider">
            <svg width="120" height="12" viewBox="0 0 120 12">
              <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
            </svg>
          </div>
        </div>

        {/* Content */}
        <div className="itinerary-body">
          {loading ? (
            <div className="itinerary-loading">
              <div className="itinerary-loading-dot" />
              <div className="itinerary-loading-dot" />
              <div className="itinerary-loading-dot" />
            </div>
          ) : showForm ? (
            /* Event form */
            <form className="itinerary-form" onSubmit={handleSubmit}>
              <h3 className="itinerary-form-title">
                {editingEvent ? 'Edit Event' : 'New Event'}
              </h3>

              {/* Category selector */}
              <div className="itinerary-category-selector">
                {CATEGORIES.map(cat => (
                  <button
                    key={cat.value}
                    type="button"
                    className={`itinerary-category-btn ${form.category === cat.value ? 'active' : ''}`}
                    onClick={() => setForm(f => ({ ...f, category: cat.value }))}
                    title={cat.label}
                  >
                    <span className="itinerary-category-icon">{cat.icon}</span>
                    <span className="itinerary-category-label">{cat.label}</span>
                  </button>
                ))}
              </div>

              <Input
                placeholder="Event title"
                value={form.title}
                onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                autoFocus
              />

              <div className="itinerary-form-grid">
                <FormField label="Location">
                  <Input
                    placeholder="Location (optional)"
                    value={form.location}
                    onChange={e => setForm(f => ({ ...f, location: e.target.value }))}
                  />
                </FormField>
                <FormField label="Estimated cost">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    value={form.estimatedCost}
                    onChange={e => setForm(f => ({ ...f, estimatedCost: e.target.value }))}
                  />
                </FormField>
              </div>

              <div className="itinerary-time-row">
                <FormField label="Start time">
                  <Input
                    type="datetime-local"
                    value={form.eventAt}
                    onChange={e => setForm(f => ({ ...f, eventAt: e.target.value }))}
                  />
                </FormField>
                <FormField label="End time">
                  <Input
                    type="datetime-local"
                    value={form.eventEndAt}
                    onChange={e => setForm(f => ({ ...f, eventEndAt: e.target.value }))}
                  />
                </FormField>
              </div>

              <textarea
                className="modal-input itinerary-textarea"
                placeholder="Description (optional)"
                rows={2}
                value={form.description}
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              />
              <textarea
                className="modal-input itinerary-textarea"
                placeholder="Details & notes (optional)"
                rows={2}
                value={form.details}
                onChange={e => setForm(f => ({ ...f, details: e.target.value }))}
              />

              {/* Links */}
              <div className="itinerary-links-section">
                <div className="itinerary-links-header">
                  <span className="itinerary-links-title">Links</span>
                  {form.links.length < 10 && (
                    <button type="button" className="itinerary-add-link-btn" onClick={handleAddLink}>
                      + Add link
                    </button>
                  )}
                </div>
                {form.links.map((link, i) => (
                  <div key={i} className="itinerary-link-row">
                    <Input
                      placeholder="URL"
                      value={link.url}
                      onChange={e => handleLinkChange(i, 'url', e.target.value)}
                    />
                    <Input
                      placeholder="Label (optional)"
                      value={link.label}
                      onChange={e => handleLinkChange(i, 'label', e.target.value)}
                    />
                    <button
                      type="button"
                      className="itinerary-action-btn itinerary-action-btn--danger"
                      title="Remove link"
                      style={{ opacity: 1 }}
                      onClick={() => handleRemoveLink(i)}
                    >
                      <svg width="14" height="14" viewBox="0 0 14 14">
                        <line x1="3" y1="7" x2="11" y2="7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>

              {error && <p className="modal-error">{error}</p>}
              <div className="modal-actions">
                <Button type="button" variant="secondary" onClick={resetForm}>
                  Cancel
                </Button>
                <Button type="submit" disabled={saving}>
                  {saving ? 'Saving...' : editingEvent ? 'Update' : 'Add Event'}
                </Button>
              </div>
            </form>
          ) : events.length === 0 ? (
            /* Empty state */
            <div className="itinerary-empty">
              <svg width="64" height="64" viewBox="0 0 64 64" className="itinerary-empty-icon">
                <rect x="12" y="8" width="40" height="48" rx="4" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" strokeDasharray="4,3" />
                <line x1="20" y1="22" x2="44" y2="22" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.4" />
                <line x1="20" y1="30" x2="40" y2="30" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.3" />
                <line x1="20" y1="38" x2="36" y2="38" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.2" />
              </svg>
              <p className="itinerary-empty-text">
                {isOwner
                  ? 'No events planned yet. Start mapping out the adventure!'
                  : 'No events planned yet. The trail master is still charting the course.'}
              </p>
            </div>
          ) : (
            /* Event cards grouped by day */
            <div className="itinerary-timeline">
              {Array.from(grouped.entries()).map(([date, dayEvents]) => {
                const dayCost = getDayCost(dayEvents);
                const dayNum = getDayNumber(date, allDates);
                return (
                  <div key={date} className="itinerary-day">
                    <div className="itinerary-day-header">
                      <div className="itinerary-day-pill">
                        <span className="itinerary-day-date">
                          Day {dayNum}: {date}
                        </span>
                        {dayCost !== null && (
                          <span className="itinerary-day-cost">{formatCost(dayCost)}</span>
                        )}
                      </div>
                    </div>
                    <div className="itinerary-day-events">
                      {dayEvents.map(event => {
                        const categoryIcon = CATEGORY_ICON_MAP[event.category] || CATEGORY_ICON_MAP.other;
                        const categoryLabel = CATEGORY_LABEL_MAP[event.category] || 'Other';
                        const hasCost = event.estimatedCost !== null;
                        const hasDetails = event.description || event.details || event.links.length > 0;
                        return (
                          <div key={event.id} className="itinerary-card">
                            {/* Top: time + category badge */}
                            <div className="itinerary-card-top">
                              <div className="itinerary-card-time">
                                <svg className="itinerary-card-time-icon" width="12" height="12" viewBox="0 0 12 12">
                                  <circle cx="6" cy="6" r="5" fill="none" stroke="currentColor" strokeWidth="1.2" />
                                  <line x1="6" y1="3" x2="6" y2="6" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                                  <line x1="6" y1="6" x2="8" y2="7.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                                </svg>
                                <span>{formatTime(event.eventAt)}</span>
                                {event.eventEndAt && (
                                  <span className="itinerary-card-time-end">&ndash; {formatTime(event.eventEndAt)}</span>
                                )}
                              </div>
                              <span className={`itinerary-card-badge itinerary-card-badge--${event.category}`}>
                                <span className="itinerary-card-badge-icon">{categoryIcon}</span>
                                {categoryLabel}
                              </span>
                            </div>

                            {/* Body: title, location */}
                            <div className="itinerary-card-body">
                              <div className="itinerary-card-title">{event.title}</div>
                              {event.location && (
                                <div className="itinerary-card-location">
                                  <svg className="itinerary-card-location-icon" width="11" height="11" viewBox="0 0 11 11">
                                    <path d="M5.5,1 C3.5,1 2,2.5 2,4.3 C2,6.8 5.5,10 5.5,10 C5.5,10 9,6.8 9,4.3 C9,2.5 7.5,1 5.5,1 Z" fill="none" stroke="currentColor" strokeWidth="1.1" />
                                    <circle cx="5.5" cy="4.3" r="1.2" fill="none" stroke="currentColor" strokeWidth="1" />
                                  </svg>
                                  {event.location}
                                </div>
                              )}
                            </div>

                            {/* Details: description, notes, links — always visible */}
                            {hasDetails && (
                              <div className="itinerary-card-details">
                                {event.description && (
                                  <p className="itinerary-card-desc">{event.description}</p>
                                )}
                                {event.details && (
                                  <p className="itinerary-card-notes">
                                    <svg width="12" height="12" viewBox="0 0 12 12" className="itinerary-notes-icon">
                                      <rect x="1" y="1" width="10" height="10" rx="2" fill="none" stroke="currentColor" strokeWidth="1" />
                                      <line x1="3" y1="4" x2="9" y2="4" stroke="currentColor" strokeWidth="0.8" />
                                      <line x1="3" y1="6" x2="8" y2="6" stroke="currentColor" strokeWidth="0.8" />
                                      <line x1="3" y1="8" x2="6" y2="8" stroke="currentColor" strokeWidth="0.8" />
                                    </svg>
                                    {event.details}
                                  </p>
                                )}
                                {event.links.length > 0 && (
                                  <div className="itinerary-card-links">
                                    {event.links.map(link => (
                                      <a
                                        key={link.id}
                                        href={link.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="itinerary-link-chip"
                                      >
                                        <svg width="12" height="12" viewBox="0 0 12 12" className="itinerary-link-icon">
                                          <path d="M5,7 L7,5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                                          <path d="M6.5,3.5 Q8,2 9.5,3.5 Q11,5 9.5,6.5 L8.5,7.5" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                                          <path d="M5.5,8.5 Q4,10 2.5,8.5 Q1,7 2.5,5.5 L3.5,4.5" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                                        </svg>
                                        {link.label || link.url}
                                      </a>
                                    ))}
                                  </div>
                                )}
                              </div>
                            )}

                            {/* Bottom bar: cost + actions */}
                            <div className="itinerary-card-bottom">
                              <span className={`itinerary-card-cost ${!hasCost ? 'itinerary-card-cost--empty' : ''}`}>
                                {formatCost(event.estimatedCost)}
                              </span>
                              {isOwner && (
                                <div className="itinerary-event-actions">
                                  <button
                                    className="itinerary-action-btn"
                                    title="Edit"
                                    onClick={e => { e.stopPropagation(); handleEdit(event); }}
                                  >
                                    <svg width="15" height="15" viewBox="0 0 15 15">
                                      <path d="M10.5,2.5 L12.5,4.5 L5,12 L3,12 L3,10 Z" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round" />
                                    </svg>
                                  </button>
                                  <button
                                    className="itinerary-action-btn itinerary-action-btn--danger"
                                    title="Delete"
                                    disabled={deletingId === event.id}
                                    onClick={e => { e.stopPropagation(); handleDelete(event.id); }}
                                  >
                                    <svg width="15" height="15" viewBox="0 0 15 15">
                                      <path d="M3.5,4.5 L11.5,4.5 L10.5,13 L4.5,13 Z" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round" />
                                      <line x1="2.5" y1="4.5" x2="12.5" y2="4.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                                      <line x1="5.5" y1="2" x2="9.5" y2="2" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
                                    </svg>
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="itinerary-footer">
          {isOwner && !showForm && (
            <Button className="itinerary-add-btn" onClick={openAddForm}>
              <svg width="14" height="14" viewBox="0 0 14 14">
                <line x1="7" y1="2" x2="7" y2="12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <line x1="2" y1="7" x2="12" y2="7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              Add Event
            </Button>
          )}
          {!showForm && itinerary?.totalEstimatedCost !== null && itinerary?.totalEstimatedCost !== undefined && (
            <span className="itinerary-trip-total">
              Trip Total:
              <span className="itinerary-trip-total-amount">
                {formatCost(itinerary.totalEstimatedCost)}
              </span>
            </span>
          )}
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
    </Modal>
  );
}
