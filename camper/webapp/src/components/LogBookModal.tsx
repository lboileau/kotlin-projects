import { useState, useEffect, useCallback } from 'react';
import { api, type LogBookFaqResponse, type LogBookJournalEntryResponse } from '../api/client';
import { Button } from './ui/Button';
import { Input } from './ui/Input';
import { Modal } from './ui/Modal';
import './LogBookModal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  userId: string;
  userRole: string;
  members: Array<{ id: string; displayName: string }>;
  refreshKey?: number;
}

type Section = 'faqs' | 'journal';

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
}

function getMemberName(userId: string, members: Array<{ id: string; displayName: string }>): string {
  return members.find(m => m.id === userId)?.displayName || 'Unknown';
}

export function LogBookModal({ isOpen, onClose, planId, userId, userRole, members, refreshKey }: Props) {
  const [section, setSection] = useState<Section>('faqs');

  // FAQ state
  const [faqs, setFaqs] = useState<LogBookFaqResponse[]>([]);
  const [faqsLoading, setFaqsLoading] = useState(true);
  const [newQuestion, setNewQuestion] = useState('');
  const [askingFaq, setAskingFaq] = useState(false);
  const [answeringFaqId, setAnsweringFaqId] = useState<string | null>(null);
  const [answerText, setAnswerText] = useState('');
  const [savingAnswer, setSavingAnswer] = useState(false);
  const [deletingFaqId, setDeletingFaqId] = useState<string | null>(null);

  // Journal state
  const [entries, setEntries] = useState<LogBookJournalEntryResponse[]>([]);
  const [entriesLoading, setEntriesLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [showNewEntry, setShowNewEntry] = useState(false);
  const [newEntryContent, setNewEntryContent] = useState('');
  const [savingEntry, setSavingEntry] = useState(false);
  const [editingEntryId, setEditingEntryId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);
  const [deletingEntryId, setDeletingEntryId] = useState<string | null>(null);

  const isOwnerOrManager = userRole === 'OWNER' || userRole === 'MANAGER';

  const loadFaqs = useCallback(async () => {
    try {
      const data = await api.getLogBookFaqs(planId);
      setFaqs(data);
    } catch {
      setFaqs([]);
    } finally {
      setFaqsLoading(false);
    }
  }, [planId]);

  const loadEntries = useCallback(async () => {
    try {
      const data = await api.getLogBookJournalEntries(planId);
      setEntries(data);
      return data;
    } catch {
      setEntries([]);
      return [];
    } finally {
      setEntriesLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    if (isOpen) {
      loadFaqs();
      loadEntries();
    }
  }, [isOpen, loadFaqs, loadEntries]);

  useEffect(() => {
    if (isOpen && refreshKey !== undefined && refreshKey > 0) {
      loadFaqs();
      loadEntries();
    }
  }, [refreshKey]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAskFaq = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newQuestion.trim()) return;
    setAskingFaq(true);
    try {
      await api.createLogBookFaq(planId, { question: newQuestion.trim() });
      setNewQuestion('');
      await loadFaqs();
    } finally {
      setAskingFaq(false);
    }
  };

  const handleAnswerFaq = async (faqId: string) => {
    if (!answerText.trim()) return;
    setSavingAnswer(true);
    try {
      await api.answerLogBookFaq(planId, faqId, { answer: answerText.trim() });
      setAnsweringFaqId(null);
      setAnswerText('');
      await loadFaqs();
    } finally {
      setSavingAnswer(false);
    }
  };

  const handleDeleteFaq = async (faqId: string) => {
    setDeletingFaqId(faqId);
    try {
      await api.deleteLogBookFaq(planId, faqId);
      await loadFaqs();
    } finally {
      setDeletingFaqId(null);
    }
  };

  const handleCreateEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newEntryContent.trim()) return;
    setSavingEntry(true);
    try {
      await api.createLogBookJournalEntry(planId, { content: newEntryContent.trim() });
      setNewEntryContent('');
      setShowNewEntry(false);
      const updated = await loadEntries();
      setCurrentPage(updated.length - 1);
    } finally {
      setSavingEntry(false);
    }
  };

  const handleUpdateEntry = async (entryId: string) => {
    if (!editContent.trim()) return;
    setSavingEdit(true);
    try {
      await api.updateLogBookJournalEntry(planId, entryId, { content: editContent.trim() });
      setEditingEntryId(null);
      setEditContent('');
      await loadEntries();
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDeleteEntry = async (entryId: string) => {
    setDeletingEntryId(entryId);
    try {
      await api.deleteLogBookJournalEntry(planId, entryId);
      const updated = await loadEntries();
      if (currentPage >= updated.length && currentPage > 0) {
        setCurrentPage(updated.length - 1);
      }
    } finally {
      setDeletingEntryId(null);
    }
  };

  const currentEntry = entries[currentPage];
  const totalPages = entries.length;

  return (
    <Modal isOpen={isOpen} onClose={onClose} className="logbook-modal">
        {/* Header */}
        <div className="logbook-header">
          <div className="logbook-header-icon">
            <svg width="36" height="36" viewBox="0 0 36 36">
              <rect x="6" y="4" width="24" height="28" rx="2" fill="var(--parchment)" stroke="#8B4513" strokeWidth="1.5" />
              <rect x="16" y="3" width="3" height="30" rx="1" fill="#5A3210" />
              <line x1="9" y1="12" x2="15" y2="12" stroke="var(--tan-deep)" strokeWidth="0.8" opacity="0.5" />
              <line x1="9" y1="16" x2="14" y2="16" stroke="var(--tan-deep)" strokeWidth="0.8" opacity="0.4" />
              <line x1="20" y1="12" x2="27" y2="12" stroke="var(--tan-deep)" strokeWidth="0.8" opacity="0.5" />
              <line x1="20" y1="16" x2="26" y2="16" stroke="var(--tan-deep)" strokeWidth="0.8" opacity="0.4" />
              <path d="M24,4 L24,-1 L26,1.5 L28,-1 L28,4" fill="var(--rose-deep)" strokeWidth="0.5" />
            </svg>
          </div>
          <h2 className="modal-title">Camp Log Book</h2>
          <div className="modal-divider">
            <svg width="120" height="12" viewBox="0 0 120 12">
              <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
            </svg>
          </div>
        </div>

        {/* Section tabs */}
        <div className="logbook-tabs">
          <button
            className={`logbook-tab ${section === 'faqs' ? 'logbook-tab--active' : ''}`}
            onClick={() => setSection('faqs')}
          >
            <svg width="16" height="16" viewBox="0 0 16 16">
              <circle cx="8" cy="8" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.2" />
              <text x="8" y="11.5" textAnchor="middle" fontSize="9" fill="currentColor" fontWeight="bold">?</text>
            </svg>
            FAQs
          </button>
          <button
            className={`logbook-tab ${section === 'journal' ? 'logbook-tab--active' : ''}`}
            onClick={() => setSection('journal')}
          >
            <svg width="16" height="16" viewBox="0 0 16 16">
              <rect x="2" y="1" width="12" height="14" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.2" />
              <line x1="5" y1="5" x2="11" y2="5" stroke="currentColor" strokeWidth="0.8" />
              <line x1="5" y1="8" x2="10" y2="8" stroke="currentColor" strokeWidth="0.8" />
              <line x1="5" y1="11" x2="9" y2="11" stroke="currentColor" strokeWidth="0.8" />
            </svg>
            Journal
          </button>
        </div>

        {/* Body */}
        <div className="logbook-body">
          {section === 'faqs' ? (
            /* FAQs section */
            <div className="logbook-faqs">
              {faqsLoading ? (
                <div className="logbook-loading">
                  <div className="logbook-loading-dot" />
                  <div className="logbook-loading-dot" />
                  <div className="logbook-loading-dot" />
                </div>
              ) : faqs.length === 0 ? (
                <div className="logbook-empty">
                  <svg width="48" height="48" viewBox="0 0 48 48" className="logbook-empty-icon">
                    <circle cx="24" cy="24" r="18" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" strokeDasharray="4,3" />
                    <text x="24" y="30" textAnchor="middle" fontSize="18" fill="var(--tan-deep)" opacity="0.5" fontWeight="bold">?</text>
                  </svg>
                  <p className="logbook-empty-text">
                    No questions yet. Ask something about the trip!
                  </p>
                </div>
              ) : (
                <div className="logbook-faq-list">
                  {faqs.map(faq => (
                    <div key={faq.id} className="logbook-faq-item">
                      <div className="logbook-faq-question">
                        <span className="logbook-faq-q-mark">Q:</span>
                        <div className="logbook-faq-q-content">
                          <span className="logbook-faq-q-text">{faq.question}</span>
                          <span className="logbook-faq-meta">
                            Asked by {getMemberName(faq.askedById, members)}
                          </span>
                        </div>
                        {(faq.askedById === userId || isOwnerOrManager) && (
                          <button
                            className="logbook-action-btn logbook-action-btn--danger"
                            title="Delete"
                            disabled={deletingFaqId === faq.id}
                            onClick={() => handleDeleteFaq(faq.id)}
                          >
                            <svg width="14" height="14" viewBox="0 0 14 14">
                              <path d="M3,4 L11,4 L10,12 L4,12 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
                              <line x1="2" y1="4" x2="12" y2="4" stroke="currentColor" strokeWidth="1.2" />
                              <line x1="5" y1="2" x2="9" y2="2" stroke="currentColor" strokeWidth="1.2" />
                            </svg>
                          </button>
                        )}
                      </div>
                      {faq.answer && answeringFaqId !== faq.id ? (
                        <div className="logbook-faq-answer">
                          <span className="logbook-faq-a-mark">A:</span>
                          <div className="logbook-faq-a-content">
                            <span className="logbook-faq-a-text">{faq.answer}</span>
                            <span className="logbook-faq-meta">
                              Answered by {getMemberName(faq.answeredById!, members)}
                              {isOwnerOrManager && (
                                <button
                                  className="logbook-edit-answer-btn"
                                  onClick={() => { setAnsweringFaqId(faq.id); setAnswerText(faq.answer!); }}
                                >
                                  Edit
                                </button>
                              )}
                            </span>
                          </div>
                        </div>
                      ) : answeringFaqId === faq.id ? (
                        <div className="logbook-faq-answer-form">
                          <textarea
                            className="modal-input logbook-textarea"
                            placeholder="Write your answer..."
                            value={answerText}
                            onChange={e => setAnswerText(e.target.value)}
                            rows={2}
                            autoFocus
                          />
                          <div className="logbook-faq-answer-actions">
                            <Button
                              variant="secondary"
                              onClick={() => { setAnsweringFaqId(null); setAnswerText(''); }}
                            >
                              Cancel
                            </Button>
                            <Button
                              disabled={savingAnswer || !answerText.trim()}
                              onClick={() => handleAnswerFaq(faq.id)}
                            >
                              {savingAnswer ? 'Saving...' : faq.answer ? 'Save' : 'Answer'}
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="logbook-faq-awaiting">
                          <span className="logbook-faq-awaiting-text">Awaiting answer...</span>
                          {isOwnerOrManager && (
                            <button
                              className="logbook-answer-btn"
                              onClick={() => { setAnsweringFaqId(faq.id); setAnswerText(''); }}
                            >
                              Answer
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {/* Ask new question form */}
              <form className="logbook-ask-form" onSubmit={handleAskFaq}>
                <Input
                  placeholder="Ask a question about the trip..."
                  value={newQuestion}
                  onChange={e => setNewQuestion(e.target.value)}
                />
                <Button type="submit" disabled={askingFaq || !newQuestion.trim()}>
                  {askingFaq ? '...' : 'Ask'}
                </Button>
              </form>
            </div>
          ) : (
            /* Journal section */
            <div className="logbook-journal">
              {entriesLoading ? (
                <div className="logbook-loading">
                  <div className="logbook-loading-dot" />
                  <div className="logbook-loading-dot" />
                  <div className="logbook-loading-dot" />
                </div>
              ) : showNewEntry ? (
                <form className="logbook-new-entry-form" onSubmit={handleCreateEntry}>
                  <h3 className="logbook-form-title">New Journal Entry</h3>
                  <textarea
                    className="modal-input logbook-textarea logbook-textarea--large"
                    placeholder="Write your journal entry..."
                    value={newEntryContent}
                    onChange={e => setNewEntryContent(e.target.value)}
                    rows={6}
                    autoFocus
                  />
                  <div className="modal-actions">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => { setShowNewEntry(false); setNewEntryContent(''); }}
                    >
                      Cancel
                    </Button>
                    <Button type="submit" disabled={savingEntry || !newEntryContent.trim()}>
                      {savingEntry ? 'Saving...' : 'Add Entry'}
                    </Button>
                  </div>
                </form>
              ) : totalPages === 0 ? (
                <div className="logbook-empty">
                  <svg width="48" height="48" viewBox="0 0 48 48" className="logbook-empty-icon">
                    <rect x="8" y="4" width="32" height="40" rx="3" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" strokeDasharray="4,3" />
                    <line x1="14" y1="16" x2="34" y2="16" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.3" />
                    <line x1="14" y1="22" x2="30" y2="22" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.25" />
                    <line x1="14" y1="28" x2="32" y2="28" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.2" />
                  </svg>
                  <p className="logbook-empty-text">
                    No journal entries yet. Be the first to write about the adventure!
                  </p>
                </div>
              ) : currentEntry && (
                <div className="logbook-page">
                  <div className="logbook-page-header">
                    <span className="logbook-page-number">Page {currentEntry.pageNumber}</span>
                    <span className="logbook-page-author">
                      by {getMemberName(currentEntry.userId, members)}
                    </span>
                  </div>
                  {editingEntryId === currentEntry.id ? (
                    <div className="logbook-edit-form">
                      <textarea
                        className="modal-input logbook-textarea logbook-textarea--large"
                        value={editContent}
                        onChange={e => setEditContent(e.target.value)}
                        rows={6}
                        autoFocus
                      />
                      <div className="modal-actions">
                        <Button
                          variant="secondary"
                          onClick={() => { setEditingEntryId(null); setEditContent(''); }}
                        >
                          Cancel
                        </Button>
                        <Button
                          disabled={savingEdit || !editContent.trim()}
                          onClick={() => handleUpdateEntry(currentEntry.id)}
                        >
                          {savingEdit ? 'Saving...' : 'Save'}
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className="logbook-page-content">
                      <p className="logbook-page-text">{currentEntry.content}</p>
                      <span className="logbook-page-date">{formatDate(currentEntry.createdAt)}</span>
                    </div>
                  )}
                  {editingEntryId !== currentEntry.id && (
                    <div className="logbook-page-actions">
                      {currentEntry.userId === userId && (
                        <button
                          className="logbook-action-btn"
                          title="Edit"
                          onClick={() => { setEditingEntryId(currentEntry.id); setEditContent(currentEntry.content); }}
                        >
                          <svg width="14" height="14" viewBox="0 0 14 14">
                            <path d="M10,2 L12,4 L5,11 L3,11 L3,9 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
                          </svg>
                        </button>
                      )}
                      {(currentEntry.userId === userId || isOwnerOrManager) && (
                        <button
                          className="logbook-action-btn logbook-action-btn--danger"
                          title="Delete"
                          disabled={deletingEntryId === currentEntry.id}
                          onClick={() => handleDeleteEntry(currentEntry.id)}
                        >
                          <svg width="14" height="14" viewBox="0 0 14 14">
                            <path d="M3,4 L11,4 L10,12 L4,12 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
                            <line x1="2" y1="4" x2="12" y2="4" stroke="currentColor" strokeWidth="1.2" />
                            <line x1="5" y1="2" x2="9" y2="2" stroke="currentColor" strokeWidth="1.2" />
                          </svg>
                        </button>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* Page navigation */}
              {totalPages > 0 && !showNewEntry && (
                <div className="logbook-page-nav">
                  <button
                    className="logbook-nav-btn"
                    disabled={currentPage === 0}
                    onClick={() => setCurrentPage(p => p - 1)}
                  >
                    <svg width="16" height="16" viewBox="0 0 16 16">
                      <path d="M10,3 L5,8 L10,13" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </button>
                  <span className="logbook-nav-info">
                    {currentPage + 1} of {totalPages}
                  </span>
                  <button
                    className="logbook-nav-btn"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setCurrentPage(p => p + 1)}
                  >
                    <svg width="16" height="16" viewBox="0 0 16 16">
                      <path d="M6,3 L11,8 L6,13" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="logbook-footer">
          {section === 'journal' && !showNewEntry && (
            <Button className="logbook-add-btn" onClick={() => setShowNewEntry(true)}>
              <svg width="14" height="14" viewBox="0 0 14 14">
                <line x1="7" y1="2" x2="7" y2="12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <line x1="2" y1="7" x2="12" y2="7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              New Entry
            </Button>
          )}
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
    </Modal>
  );
}
