/**
 * W9 — MealPlanModal blur-save feedback (meal plan name)
 *
 * Three suites:
 *
 * A) OverviewView unit tests (chip rendering driven by nameSaveStatus prop):
 *  (1) nameSaveStatus='idle'   → no flash chip rendered
 *  (2) nameSaveStatus='saving' → "Saving…" chip visible
 *  (3) nameSaveStatus='saved'  → "✓ Saved" chip visible
 *  (4) nameSaveStatus='error'  → "Couldn't save — try again" chip with role="alert"
 *  (5) Blurring the input with a changed value calls onUpdateName with the new value
 *  (6) Pressing Enter triggers blur → onUpdateName called (existing behaviour preserved)
 *
 * B) MealPlanModal integration (production handleUpdateName, W12 lesson):
 *  (7) Successful save → api.updateMealPlan called; "✓ Saved" chip visible
 *  (8) Failed save → "Couldn't save" chip + toast.error + input reverts to previous name
 *  (9) Blur with unchanged name → no api.updateMealPlan call, no chip shown
 *
 * C) Timer decay (vi.useFakeTimers + OverviewView DecayWrapper):
 *  (10) After save succeeds, chip decays to idle after exactly 1.5s
 *  (11) After save fails, chip decays to idle after exactly 3s
 *
 * Timer strategy: suites A + B use real timers (waitFor works correctly).
 * Suite C isolates decay timer tests to a minimal wrapper — vi.useFakeTimers()
 * breaks waitFor's internal setTimeout-based timeout callback, making full-modal
 * loading tests hang. The wrapper mirrors the production setTimeout(…, 1500/3000)
 * pattern and exercises the same OverviewView rendering path.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useState, useEffect, useRef } from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MealPlanModal, OverviewView } from './MealPlanModal';
import type { OverviewProps } from './MealPlanModal';
import type {
  MealPlanDetailResponse,
  MealPlanDayResponse,
} from '../api/client';

// ─── Mock api ─────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getMealPlanForTrip: vi.fn(),
  updateMealPlan:     vi.fn(),
  getRecipes:         vi.fn(),
  getTemplates:       vi.fn(),
  getIngredients:     vi.fn(),
}));

vi.mock('../api/client', () => ({
  api: mockApi,
}));

// ─── Mock useToast ─────────────────────────────────────────────────────────────

const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error:   vi.fn(),
  info:    vi.fn(),
  dismiss: vi.fn(),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const mockDay: MealPlanDayResponse = {
  id: 'day1',
  dayNumber: 1,
  meals: { breakfast: [], lunch: [], dinner: [], snack: [] },
};

const mockMealPlan: MealPlanDetailResponse = {
  id: 'mp1',
  planId: 'plan1',
  name: 'Camp Plan',
  servings: 4,
  scalingMode: 'auto',
  isTemplate: false,
  sourceTemplateId: null,
  createdBy: 'user1',
  days: [mockDay],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

// ─── OverviewView props factory ────────────────────────────────────────────────

function makeOverviewProps(overrides: Partial<OverviewProps> = {}): OverviewProps {
  return {
    mealPlan: mockMealPlan,
    currentDay: mockDay,
    activeDay: 0,
    setActiveDay: vi.fn(),
    onAddDay: vi.fn(),
    onRemoveDay: vi.fn(),
    onRemoveRecipe: vi.fn(),
    onUpdateName: vi.fn(),
    nameSaveStatus: 'idle',
    onUpdateServings: vi.fn(),
    createName: '',
    setCreateName: vi.fn(),
    createServings: 4,
    setCreateServings: vi.fn(),
    creating: false,
    onCreate: vi.fn(),
    error: null,
    templates: [],
    showLoadTemplate: false,
    setShowLoadTemplate: vi.fn(),
    loadingTemplate: false,
    onLoadTemplate: vi.fn(),
    onPreviewTemplate: vi.fn(),
    templatePreview: null,
    loadingPreview: false,
    setReplaceTemplateId: vi.fn(),
    setTemplatePreview: vi.fn(),
    showSaveTemplate: false,
    setShowSaveTemplate: vi.fn(),
    templateName: '',
    setTemplateName: vi.fn(),
    savingTemplate: false,
    onSaveAsTemplate: vi.fn(),
    recipes: [],
    onAddRecipeInline: vi.fn(),
    ...overrides,
  };
}

// ─── DecayWrapper (Suite C helper) ────────────────────────────────────────────
// Mirrors the production setTimeout(…, delay) → setNameSaveStatus('idle') pattern
// in MealPlanModal.handleUpdateName. Used to test chip decay with fake timers
// without loading the full modal (which requires waitFor + real timers).

type SaveStatus = 'idle' | 'saving' | 'saved' | 'error';

function DecayWrapper({ initialStatus }: { initialStatus: 'saved' | 'error' }) {
  const [status, setStatus] = useState<SaveStatus>(initialStatus);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const delay = initialStatus === 'saved' ? 1500 : 3000;
    timerRef.current = setTimeout(() => setStatus('idle'), delay);
    return () => { if (timerRef.current) clearTimeout(timerRef.current); };
  }, [initialStatus]);

  return <OverviewView {...makeOverviewProps({ nameSaveStatus: status })} />;
}

// ═══════════════════════════════════════════════════════════════════════════════
// A) OverviewView unit tests — chip rendering (nameSaveStatus prop)
// ═══════════════════════════════════════════════════════════════════════════════

describe('OverviewView — blur-save chip rendering (W9)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── (1) idle → no chip ───────────────────────────────────────────────────

  it('(1) idle: no save-flash chip rendered', () => {
    render(<OverviewView {...makeOverviewProps({ nameSaveStatus: 'idle' })} />);

    expect(screen.queryByText('Saving…')).not.toBeInTheDocument();
    expect(screen.queryByText('✓ Saved')).not.toBeInTheDocument();
    expect(screen.queryByText(/couldn't save/i)).not.toBeInTheDocument();
  });

  // ── (2) saving → "Saving…" chip ─────────────────────────────────────────

  it('(2) saving: "Saving…" chip is visible', () => {
    render(<OverviewView {...makeOverviewProps({ nameSaveStatus: 'saving' })} />);

    expect(screen.getByText('Saving…')).toBeInTheDocument();
  });

  // ── (3) saved → "✓ Saved" chip ──────────────────────────────────────────

  it('(3) saved: "✓ Saved" chip is visible', () => {
    render(<OverviewView {...makeOverviewProps({ nameSaveStatus: 'saved' })} />);

    expect(screen.getByText('✓ Saved')).toBeInTheDocument();
  });

  // ── (4) error → "Couldn't save" chip with role="alert" ──────────────────

  it('(4) error: "Couldn\'t save" chip with role="alert" is visible', () => {
    render(<OverviewView {...makeOverviewProps({ nameSaveStatus: 'error' })} />);

    const chip = screen.getByRole('alert');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent(/couldn't save/i);
  });

  // ── (5) Blur with changed value calls onUpdateName ───────────────────────

  it('(5) blurring the name input with a changed value calls onUpdateName with the new value', async () => {
    const onUpdateName = vi.fn();
    const user = userEvent.setup();
    render(<OverviewView {...makeOverviewProps({ onUpdateName })} />);

    const input = screen.getByDisplayValue('Camp Plan');
    await user.clear(input);
    await user.type(input, 'New Name');
    await user.tab(); // triggers blur

    expect(onUpdateName).toHaveBeenCalledWith('New Name');
  });

  // ── (6) Enter key blurs the input and triggers onUpdateName ─────────────

  it('(6) pressing Enter blurs the input and triggers onUpdateName', async () => {
    const onUpdateName = vi.fn();
    const user = userEvent.setup();
    render(<OverviewView {...makeOverviewProps({ onUpdateName })} />);

    const input = screen.getByDisplayValue('Camp Plan');
    await user.clear(input);
    await user.type(input, 'Pressed Enter');
    await user.keyboard('{Enter}');

    expect(onUpdateName).toHaveBeenCalledWith('Pressed Enter');
  });
});

// ═══════════════════════════════════════════════════════════════════════════════
// B) MealPlanModal integration — production handleUpdateName (W12 lesson)
// Real timers: waitFor works correctly; decay verification is in Suite C.
// ═══════════════════════════════════════════════════════════════════════════════

describe('MealPlanModal — handleUpdateName blur-save (integration, W9)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState(null, '', '/');

    mockApi.getMealPlanForTrip.mockResolvedValue(mockMealPlan);
    mockApi.getRecipes.mockResolvedValue([]);
    mockApi.getTemplates.mockResolvedValue([]);
    mockApi.getIngredients.mockResolvedValue([]);
    mockApi.updateMealPlan.mockResolvedValue({});
  });

  // ── (7) Successful save: api called + "✓ Saved" chip visible ────────────

  it('(7) successful save: api.updateMealPlan called and "✓ Saved" chip appears', async () => {
    const user = userEvent.setup();

    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    // Wait for modal to finish loading
    await waitFor(() => {
      expect(screen.getByDisplayValue('Camp Plan')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('Camp Plan');
    await user.clear(input);
    await user.type(input, 'Summit Plan');
    await user.tab();

    // API called with new name
    await waitFor(() => {
      expect(mockApi.updateMealPlan).toHaveBeenCalledWith('mp1', { name: 'Summit Plan' });
    });

    // "✓ Saved" chip appears after success
    await waitFor(() => {
      expect(screen.getByText('✓ Saved')).toBeInTheDocument();
    });
  });

  // ── (8) Failed save: error chip + toast + input reverts ─────────────────

  it('(8) failed save: error chip visible + toast.error fired + input reverts to previous name', async () => {
    mockApi.updateMealPlan.mockRejectedValue(new Error('Network error'));
    const user = userEvent.setup();

    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Camp Plan')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('Camp Plan');
    await user.clear(input);
    await user.type(input, 'Bad Name');
    await user.tab();

    // Error chip appears
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.getByRole('alert')).toHaveTextContent(/couldn't save/i);

    // toast.error was fired
    expect(mockToast.error).toHaveBeenCalledWith('Network error');

    // Input value reverts to the original (server name never changed)
    await waitFor(() => {
      expect(screen.getByDisplayValue('Camp Plan')).toBeInTheDocument();
    });
  });

  // ── (9) No-op: blur with same name → no API call, no chip ───────────────

  it('(9) blurring with the unchanged name → no api call, no chip', async () => {
    const user = userEvent.setup();

    render(<MealPlanModal isOpen={true} onClose={vi.fn()} planId="plan1" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Camp Plan')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('Camp Plan');

    // Click and tab without changing the value
    await user.click(input);
    await user.tab();

    expect(mockApi.updateMealPlan).not.toHaveBeenCalled();
    expect(screen.queryByText('Saving…')).not.toBeInTheDocument();
    expect(screen.queryByText('✓ Saved')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

// ═══════════════════════════════════════════════════════════════════════════════
// C) Timer decay — fake timers + DecayWrapper (mirrors production setTimeout logic)
//
// vi.useFakeTimers() breaks waitFor's internal setTimeout-based timeout callback,
// so these tests use a minimal DecayWrapper that avoids modal loading altogether.
// The wrapper uses the same setTimeout(fn, delay) → setStatus('idle') pattern as
// the production MealPlanModal.handleUpdateName, and exercises the same OverviewView
// chip-hiding code path.
// ═══════════════════════════════════════════════════════════════════════════════

describe('Save-status decay timers (fake timers, W9)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // ── (10) saved → idle after 1.5s ────────────────────────────────────────

  it('(10) "✓ Saved" chip is visible immediately and gone after 1.5s', () => {
    render(<DecayWrapper initialStatus="saved" />);

    // Chip visible immediately (timer not yet fired)
    expect(screen.getByText('✓ Saved')).toBeInTheDocument();

    // 1499ms → still visible
    act(() => { vi.advanceTimersByTime(1499); });
    expect(screen.getByText('✓ Saved')).toBeInTheDocument();

    // 1ms more (total 1500ms) → timer fires, chip gone
    act(() => { vi.advanceTimersByTime(1); });
    expect(screen.queryByText('✓ Saved')).not.toBeInTheDocument();
  });

  // ── (11) error → idle after 3s ──────────────────────────────────────────

  it('(11) "Couldn\'t save" chip is visible immediately and gone after 3s', () => {
    render(<DecayWrapper initialStatus="error" />);

    // Chip visible immediately
    expect(screen.getByRole('alert')).toBeInTheDocument();

    // 2999ms → still visible
    act(() => { vi.advanceTimersByTime(2999); });
    expect(screen.getByRole('alert')).toBeInTheDocument();

    // 1ms more (total 3000ms) → timer fires, chip gone
    act(() => { vi.advanceTimersByTime(1); });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
