import { useState, useEffect, useCallback } from 'react';
import { api, type ItineraryEvent } from '../api/client';
import { Button } from './ui/Button';
import { Input } from './ui/Input';
import { Modal } from './ui/Modal';
import './ItineraryModal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  isOwner: boolean;
  refreshKey?: number;
}

interface EventFormData {
  title: string;
  description: string;
  details: string;
  eventAt: string;
}

const EMPTY_FORM: EventFormData = { title: '', description: '', details: '', eventAt: '' };

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

export function ItineraryModal({ isOpen, onClose, planId, isOwner, refreshKey }: Props) {
  const [events, setEvents] = useState<ItineraryEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingEvent, setEditingEvent] = useState<ItineraryEvent | null>(null);
  const [form, setForm] = useState<EventFormData>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const loadEvents = useCallback(async () => {
    try {
      const itinerary = await api.getItinerary(planId);
      setEvents(itinerary.events);
    } catch {
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

  const handleEdit = (event: ItineraryEvent) => {
    setEditingEvent(event);
    setForm({
      title: event.title,
      description: event.description || '',
      details: event.details || '',
      eventAt: toLocalDatetime(event.eventAt),
    });
    setShowForm(true);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) { setError('Title is required'); return; }
    if (!form.eventAt) { setError('Date & time is required'); return; }

    setSaving(true);
    setError('');
    try {
      const data = {
        title: form.title.trim(),
        description: form.description.trim() || null,
        details: form.details.trim() || null,
        eventAt: new Date(form.eventAt).toISOString(),
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
              <Input
                placeholder="Event title"
                value={form.title}
                onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                autoFocus
              />
              <Input
                type="datetime-local"
                value={form.eventAt}
                onChange={e => setForm(f => ({ ...f, eventAt: e.target.value }))}
              />
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
            /* Event timeline */
            <div className="itinerary-timeline">
              {Array.from(grouped.entries()).map(([date, dayEvents]) => (
                <div key={date} className="itinerary-day">
                  <div className="itinerary-day-header">
                    <span className="itinerary-day-date">{date}</span>
                    <span className="itinerary-day-count">{dayEvents.length} event{dayEvents.length !== 1 ? 's' : ''}</span>
                  </div>
                  {dayEvents.map(event => {
                    const isExpanded = expandedId === event.id;
                    const hasExtra = event.description || event.details;
                    return (
                      <div key={event.id} className={`itinerary-event ${isExpanded ? 'expanded' : ''}`}>
                        <div
                          className="itinerary-event-main"
                          onClick={() => hasExtra && setExpandedId(isExpanded ? null : event.id)}
                          style={{ cursor: hasExtra ? 'pointer' : 'default' }}
                        >
                          <div className="itinerary-event-time-col">
                            <span className="itinerary-event-time">{formatTime(event.eventAt)}</span>
                            <div className="itinerary-event-dot" />
                          </div>
                          <div className="itinerary-event-content">
                            <span className="itinerary-event-title">{event.title}</span>
                            {hasExtra && !isExpanded && (
                              <span className="itinerary-event-expand-hint">tap for details</span>
                            )}
                          </div>
                          {isOwner && (
                            <div className="itinerary-event-actions">
                              <button
                                className="itinerary-action-btn"
                                title="Edit"
                                onClick={e => { e.stopPropagation(); handleEdit(event); }}
                              >
                                <svg width="14" height="14" viewBox="0 0 14 14">
                                  <path d="M10,2 L12,4 L5,11 L3,11 L3,9 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
                                </svg>
                              </button>
                              <button
                                className="itinerary-action-btn itinerary-action-btn--danger"
                                title="Delete"
                                disabled={deletingId === event.id}
                                onClick={e => { e.stopPropagation(); handleDelete(event.id); }}
                              >
                                <svg width="14" height="14" viewBox="0 0 14 14">
                                  <path d="M3,4 L11,4 L10,12 L4,12 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
                                  <line x1="2" y1="4" x2="12" y2="4" stroke="currentColor" strokeWidth="1.2" />
                                  <line x1="5" y1="2" x2="9" y2="2" stroke="currentColor" strokeWidth="1.2" />
                                </svg>
                              </button>
                            </div>
                          )}
                        </div>
                        {isExpanded && (
                          <div className="itinerary-event-details">
                            {event.description && (
                              <p className="itinerary-event-desc">{event.description}</p>
                            )}
                            {event.details && (
                              <p className="itinerary-event-notes">
                                <svg width="12" height="12" viewBox="0 0 12 12" className="itinerary-notes-icon">
                                  <rect x="1" y="1" width="10" height="10" rx="2" fill="none" stroke="currentColor" strokeWidth="1" />
                                  <line x1="3" y1="4" x2="9" y2="4" stroke="currentColor" strokeWidth="0.8" />
                                  <line x1="3" y1="6" x2="8" y2="6" stroke="currentColor" strokeWidth="0.8" />
                                  <line x1="3" y1="8" x2="6" y2="8" stroke="currentColor" strokeWidth="0.8" />
                                </svg>
                                {event.details}
                              </p>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="itinerary-footer">
          {isOwner && !showForm && (
            <Button className="itinerary-add-btn" onClick={() => { setShowForm(true); setEditingEvent(null); setForm(EMPTY_FORM); }}>
              <svg width="14" height="14" viewBox="0 0 14 14">
                <line x1="7" y1="2" x2="7" y2="12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <line x1="2" y1="7" x2="12" y2="7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              Add Event
            </Button>
          )}
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
    </Modal>
  );
}
