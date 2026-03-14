import { useState, useEffect, useCallback, useRef } from 'react';
import { api, type Item, type PlanMember } from '../api/client';
import { Modal } from './ui/Modal';
import './GearModal.css';

export interface CategoryDef {
  value: string;
  label: string;
  icon: string;
}

interface ChecklistModalConfig {
  title: string;
  icon: React.ReactNode;
  emptyText: string;
  loadingText: string;
  addPlaceholder: string;
  sharedTitle: string;
  sharedSubtitle: string;
  personalDividerLabel: string;
  categories: CategoryDef[];
  planOnly?: boolean;
  dayTabs?: boolean;
}

interface ChecklistModalProps {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  planOwnerId: string;
  members: PlanMember[];
  currentUserId: string;
  config: ChecklistModalConfig;
}

function getCategoryIcon(category: string, categories: CategoryDef[]): string {
  return categories.find(c => c.value === category)?.icon ?? '◇';
}

function getCategoryLabel(category: string, categories: CategoryDef[]): string {
  return categories.find(c => c.value === category)?.label ?? category;
}

interface ItemRowProps {
  item: Item;
  canEdit: boolean;
  categories: CategoryDef[];
  onTogglePacked: (item: Item) => void;
  onDelete: (item: Item) => void;
  onUpdate: (item: Item, name: string, quantity: number, category: string) => void;
}

function ItemRow({ item, canEdit, categories, onTogglePacked, onDelete, onUpdate }: ItemRowProps) {
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState(item.name);
  const [editQty, setEditQty] = useState(item.quantity);
  const [editCategory, setEditCategory] = useState(item.category);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (editing && inputRef.current) inputRef.current.focus();
  }, [editing]);

  const handleSave = () => {
    if (editName.trim()) {
      onUpdate(item, editName.trim(), editQty, editCategory);
      setEditing(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSave();
    if (e.key === 'Escape') {
      setEditName(item.name);
      setEditQty(item.quantity);
      setEditCategory(item.category);
      setEditing(false);
    }
  };

  if (editing) {
    return (
      <div className="gear-item gear-item--editing">
        <input
          ref={inputRef}
          className="gear-item-edit-name"
          value={editName}
          onChange={e => setEditName(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Item name"
        />
        <select
          className="gear-item-edit-category"
          value={editCategory}
          onChange={e => setEditCategory(e.target.value)}
        >
          {categories.map(c => (
            <option key={c.value} value={c.value}>{c.icon} {c.label}</option>
          ))}
        </select>
        <div className="gear-item-edit-qty">
          <button className="gear-qty-btn" onClick={() => setEditQty(Math.max(1, editQty - 1))}>−</button>
          <span className="gear-qty-value">{editQty}</span>
          <button className="gear-qty-btn" onClick={() => setEditQty(editQty + 1)}>+</button>
        </div>
        <div className="gear-item-edit-actions">
          <button className="gear-save-btn" onClick={handleSave}>✓</button>
          <button className="gear-cancel-btn" onClick={() => { setEditName(item.name); setEditQty(item.quantity); setEditCategory(item.category); setEditing(false); }}>✕</button>
        </div>
      </div>
    );
  }

  return (
    <div className={`gear-item ${item.packed ? 'gear-item--packed' : ''}`}>
      <button
        className={`gear-check ${item.packed ? 'gear-check--checked' : ''}`}
        onClick={() => canEdit && onTogglePacked(item)}
        disabled={!canEdit}
        aria-label={item.packed ? 'Unpack item' : 'Pack item'}
      >
        {item.packed && (
          <svg width="12" height="12" viewBox="0 0 12 12">
            <path d="M2,6 L5,9 L10,3" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        )}
      </button>
      <span className="gear-item-category-icon" title={getCategoryLabel(item.category, categories)}>
        {getCategoryIcon(item.category, categories)}
      </span>
      <span className="gear-item-name">{item.name}</span>
      {item.quantity > 1 && <span className="gear-item-qty">×{item.quantity}</span>}
      {canEdit && (
        <div className="gear-item-actions">
          <button className="gear-item-edit" onClick={() => setEditing(true)} title="Edit">
            <svg width="14" height="14" viewBox="0 0 14 14">
              <path d="M10,2 L12,4 L5,11 L2,12 L3,9 Z" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinejoin="round" />
            </svg>
          </button>
          <button className="gear-item-delete" onClick={() => onDelete(item)} title="Remove">
            <svg width="14" height="14" viewBox="0 0 14 14">
              <path d="M3,3 L11,11 M11,3 L3,11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      )}
    </div>
  );
}

interface AddItemFormProps {
  categories: CategoryDef[];
  placeholder: string;
  onAdd: (name: string, category: string, quantity: number) => Promise<void>;
}

function AddItemForm({ categories, placeholder, onAdd }: AddItemFormProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState(categories[0]?.value ?? '');
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setAdding(true);
    try {
      await onAdd(name.trim(), category, quantity);
      setName('');
      setQuantity(1);
      inputRef.current?.focus();
    } finally {
      setAdding(false);
    }
  };

  return (
    <form className="gear-add-form" onSubmit={handleSubmit}>
      <input
        ref={inputRef}
        className="gear-add-input"
        value={name}
        onChange={e => setName(e.target.value)}
        placeholder={placeholder}
        disabled={adding}
      />
      <select
        className="gear-add-category"
        value={category}
        onChange={e => setCategory(e.target.value)}
        disabled={adding}
      >
        {categories.map(c => (
          <option key={c.value} value={c.value}>{c.icon} {c.label}</option>
        ))}
      </select>
      <div className="gear-add-qty">
        <button type="button" className="gear-qty-btn" onClick={() => setQuantity(Math.max(1, quantity - 1))} disabled={adding}>−</button>
        <span className="gear-qty-value">{quantity}</span>
        <button type="button" className="gear-qty-btn" onClick={() => setQuantity(quantity + 1)} disabled={adding}>+</button>
      </div>
      <button type="submit" className="gear-add-btn" disabled={adding || !name.trim()}>
        {adding ? '...' : '+'}
      </button>
    </form>
  );
}

interface ChecklistSectionProps {
  title: string;
  subtitle?: string;
  items: Item[];
  onAdd: (name: string, category: string, quantity: number) => Promise<void>;
  onToggle: (item: Item) => Promise<void>;
  onDelete: (item: Item) => Promise<void>;
  onUpdate: (item: Item, name: string, quantity: number, category: string) => Promise<void>;
  defaultExpanded?: boolean;
  accentColor?: string;
  canEdit: boolean;
  categories: CategoryDef[];
  addPlaceholder: string;
  emptyText: string;
}

function ChecklistSection({ title, subtitle, items, onAdd, onToggle, onDelete, onUpdate, defaultExpanded = true, accentColor, canEdit, categories, addPlaceholder, emptyText }: ChecklistSectionProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  const packedCount = items.filter(i => i.packed).length;
  const totalCount = items.length;
  const progress = totalCount > 0 ? (packedCount / totalCount) * 100 : 0;

  // Group items by category
  const grouped = items.reduce<Record<string, Item[]>>((acc, item) => {
    (acc[item.category] ??= []).push(item);
    return acc;
  }, {});

  const categoryOrder = categories.map(c => c.value);
  const sortedCategories = Object.keys(grouped).sort((a, b) => {
    const ai = categoryOrder.indexOf(a);
    const bi = categoryOrder.indexOf(b);
    return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi);
  });

  return (
    <div className="gear-section" style={accentColor ? { '--section-accent': accentColor } as React.CSSProperties : undefined}>
      <button className="gear-section-header" onClick={() => setExpanded(!expanded)}>
        <div className="gear-section-title-row">
          <svg className={`gear-section-chevron ${expanded ? 'gear-section-chevron--open' : ''}`} width="16" height="16" viewBox="0 0 16 16">
            <path d="M5,3 L11,8 L5,13" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <h3 className="gear-section-title">{title}</h3>
          {subtitle && <span className="gear-section-subtitle">{subtitle}</span>}
        </div>
        <div className="gear-section-stats">
          <span className="gear-section-count">{packedCount}/{totalCount}</span>
          <div className="gear-progress-bar">
            <div className="gear-progress-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
      </button>

      {expanded && (
        <div className="gear-section-body">
          {sortedCategories.map(cat => (
            <div key={cat} className="gear-category-group">
              <div className="gear-category-label">
                <span className="gear-category-icon">{getCategoryIcon(cat, categories)}</span>
                <span>{getCategoryLabel(cat, categories)}</span>
              </div>
              {grouped[cat].map(item => (
                <ItemRow
                  key={item.id}
                  item={item}
                  canEdit={canEdit}
                  categories={categories}
                  onTogglePacked={onToggle}
                  onDelete={onDelete}
                  onUpdate={onUpdate}
                />
              ))}
            </div>
          ))}
          {totalCount === 0 && (
            <p className="gear-empty">{canEdit ? emptyText : 'No items yet.'}</p>
          )}
          {canEdit && <AddItemForm categories={categories} placeholder={addPlaceholder} onAdd={onAdd} />}
        </div>
      )}
    </div>
  );
}

const MEMBER_COLORS = [
  'var(--lavender)',
  'var(--sage)',
  'var(--rose)',
  'var(--mint)',
  'var(--butter)',
  'var(--tan)',
];

// Day-prefix helpers for dayTabs mode
function toDayCategory(day: number, baseCategory: string): string {
  return `day${day}:${baseCategory}`;
}

function parseDayCategory(category: string): { day: number; base: string } | null {
  const match = category.match(/^day(\d+):(.+)$/);
  if (match) return { day: parseInt(match[1]), base: match[2] };
  return null;
}

function ChecklistModal({ isOpen, onClose, planId, planOwnerId, members, currentUserId, config }: ChecklistModalProps) {
  const [planItems, setPlanItems] = useState<Item[]>([]);
  const [memberItems, setMemberItems] = useState<Record<string, Item[]>>({});
  const [loading, setLoading] = useState(true);
  const [activeDay, setActiveDay] = useState(1);
  const [numDays, setNumDays] = useState(1);

  const loadItems = useCallback(async () => {
    try {
      const planData = await api.getItems('plan', planId);
      setPlanItems(planData);

      // Derive number of days from existing items
      if (config.dayTabs) {
        let maxDay = 1;
        for (const item of planData) {
          const parsed = parseDayCategory(item.category);
          if (parsed && parsed.day > maxDay) maxDay = parsed.day;
        }
        setNumDays(maxDay);
      }

      if (!config.planOnly) {
        const memberData: Record<string, Item[]> = {};
        const results = await Promise.all(
          members.map(m => api.getItems('user', m.userId, planId).then(items => ({ userId: m.userId, items })))
        );
        for (const r of results) {
          memberData[r.userId] = r.items;
        }
        setMemberItems(memberData);
      }
    } catch {
      // fail silently
    } finally {
      setLoading(false);
    }
  }, [planId, members, config.planOnly, config.dayTabs]);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      loadItems();
    }
  }, [isOpen, loadItems]);

  // Clamp activeDay when numDays shrinks
  useEffect(() => {
    if (activeDay > numDays) setActiveDay(numDays);
  }, [numDays, activeDay]);

  // Filter items to only those matching this modal's categories
  const categoryValues = new Set(config.categories.map(c => c.value));

  let filteredPlanItems: Item[];
  if (config.dayTabs) {
    // For day-tabbed modals, all items have day-prefixed categories (e.g. day1:breakfast)
    // Filter to items whose base category matches, regardless of day
    filteredPlanItems = planItems.filter(i => {
      const parsed = parseDayCategory(i.category);
      return parsed && categoryValues.has(parsed.base);
    });
  } else {
    filteredPlanItems = planItems.filter(i => categoryValues.has(i.category));
  }

  // Items for the active day (stripped categories for display)
  const activeDayItems: Item[] = config.dayTabs
    ? filteredPlanItems
        .filter(i => {
          const parsed = parseDayCategory(i.category);
          return parsed && parsed.day === activeDay;
        })
        .map(i => {
          const parsed = parseDayCategory(i.category)!;
          return { ...i, category: parsed.base };
        })
    : filteredPlanItems;

  const filteredMemberItems: Record<string, Item[]> = {};
  for (const [userId, items] of Object.entries(memberItems)) {
    filteredMemberItems[userId] = items.filter(i => categoryValues.has(i.category));
  }

  // Check if current user can edit shared gear (owner or manager)
  const currentMember = members.find(m => m.userId === currentUserId);
  const canEditShared = currentUserId === planOwnerId || currentMember?.role === 'manager';

  // Filter out pending members (no username), sort current user first
  const sortedMembers = [...members].filter(m => m.username || m.userId === currentUserId).sort((a, b) => {
    if (a.userId === currentUserId) return -1;
    if (b.userId === currentUserId) return 1;
    return 0;
  });

  const totalPlanPacked = filteredPlanItems.filter(i => i.packed).length;
  const totalPlanCount = filteredPlanItems.length;
  const allMemberItems = config.planOnly ? [] : Object.values(filteredMemberItems).flat();
  const totalPersonalPacked = allMemberItems.filter(i => i.packed).length;
  const totalPersonalCount = allMemberItems.length;
  const grandTotal = totalPlanCount + totalPersonalCount;
  const grandPacked = totalPlanPacked + totalPersonalPacked;

  // CRUD callbacks — with day prefix when dayTabs
  const makePlanCrud = (ownerType: string, ownerId: string) => ({
    onAdd: async (name: string, category: string, quantity: number) => {
      const cat = config.dayTabs ? toDayCategory(activeDay, category) : category;
      await api.createItem({ name, category: cat, quantity, packed: false, ownerType, ownerId });
      loadItems();
    },
    onToggle: async (item: Item) => {
      // item may have stripped category — find original
      const realCategory = config.dayTabs ? toDayCategory(activeDay, item.category) : item.category;
      await api.updateItem(item.id, { name: item.name, category: realCategory, quantity: item.quantity, packed: !item.packed });
      loadItems();
    },
    onDelete: async (item: Item) => {
      await api.deleteItem(item.id);
      loadItems();
    },
    onUpdate: async (item: Item, name: string, quantity: number, category: string) => {
      const cat = config.dayTabs ? toDayCategory(activeDay, category) : category;
      await api.updateItem(item.id, { name, category: cat, quantity, packed: item.packed });
      loadItems();
    },
  });

  const makeMemberCrud = (userId: string) => ({
    onAdd: async (name: string, category: string, quantity: number) => {
      await api.createItem({ name, category, quantity, packed: false, ownerType: 'user', ownerId: userId, planId });
      loadItems();
    },
    onToggle: async (item: Item) => {
      await api.updateItem(item.id, { name: item.name, category: item.category, quantity: item.quantity, packed: !item.packed });
      loadItems();
    },
    onDelete: async (item: Item) => {
      await api.deleteItem(item.id);
      loadItems();
    },
    onUpdate: async (item: Item, name: string, quantity: number, category: string) => {
      await api.updateItem(item.id, { name, category, quantity, packed: item.packed });
      loadItems();
    },
  });

  const handleAddDay = () => {
    const newDay = numDays + 1;
    setNumDays(newDay);
    setActiveDay(newDay);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg" className="gear-modal">
        {/* Header */}
        <div className="gear-modal-header">
          <div className="gear-modal-header-left">
            <div className="gear-modal-icon">
              {config.icon}
            </div>
            <div>
              <h2 className="gear-modal-title">{config.title}</h2>
              <p className="gear-modal-subtitle">
                {grandTotal === 0
                  ? 'Start packing for your adventure'
                  : `${grandPacked} of ${grandTotal} items packed`}
              </p>
            </div>
          </div>
        </div>

        {/* Overall progress */}
        {grandTotal > 0 && (
          <div className="gear-overall-progress">
            <div className="gear-overall-bar">
              <div className="gear-overall-fill" style={{ width: `${(grandPacked / grandTotal) * 100}%` }} />
            </div>
          </div>
        )}

        {/* Content */}
        <div className="gear-modal-body">
          {loading ? (
            <div className="gear-loading">
              <div className="gear-loading-pack" />
              <p>{config.loadingText}</p>
            </div>
          ) : (
            <>
              {/* Day tabs (for dayTabs mode) */}
              {config.dayTabs && (
                <div className="gear-day-tabs">
                  {Array.from({ length: numDays }, (_, i) => i + 1).map(day => (
                    <button
                      key={day}
                      className={`gear-day-tab ${day === activeDay ? 'gear-day-tab--active' : ''}`}
                      onClick={() => setActiveDay(day)}
                    >
                      Day {day}
                    </button>
                  ))}
                  <button className="gear-day-tab gear-day-tab--add" onClick={handleAddDay} title="Add another day">
                    +
                  </button>
                </div>
              )}

              {/* Plan checklist */}
              <ChecklistSection
                title={config.dayTabs ? `Day ${activeDay}` : config.sharedTitle}
                subtitle={config.dayTabs ? undefined : config.sharedSubtitle}
                items={activeDayItems}
                {...makePlanCrud('plan', planId)}
                defaultExpanded={true}
                canEdit={config.planOnly || canEditShared}
                categories={config.categories}
                addPlaceholder={config.addPlaceholder}
                emptyText={config.emptyText}
              />

              {!config.planOnly && (
                <>
                  {/* Divider */}
                  <div className="gear-divider">
                    <svg width="200" height="16" viewBox="0 0 200 16">
                      <path d="M0,8 Q50,2 100,8 Q150,14 200,8" fill="none" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.5" />
                    </svg>
                    <span className="gear-divider-label">{config.personalDividerLabel}</span>
                    <svg width="200" height="16" viewBox="0 0 200 16">
                      <path d="M0,8 Q50,14 100,8 Q150,2 200,8" fill="none" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.5" />
                    </svg>
                  </div>

                  {/* Member checklists */}
                  {sortedMembers.map((member, i) => {
                    const items = filteredMemberItems[member.userId] ?? [];
                    const isCurrentUser = member.userId === currentUserId;
                    const displayName = member.username || 'Pending Adventurer';
                    const crud = makeMemberCrud(member.userId);
                    return (
                      <ChecklistSection
                        key={member.userId}
                        title={isCurrentUser ? `${displayName} (You)` : displayName}
                        items={items}
                        {...crud}
                        defaultExpanded={isCurrentUser}
                        accentColor={MEMBER_COLORS[i % MEMBER_COLORS.length]}
                        canEdit={isCurrentUser}
                        categories={config.categories}
                        addPlaceholder={config.addPlaceholder}
                        emptyText={config.emptyText}
                      />
                    );
                  })}
                </>
              )}
            </>
          )}
        </div>
    </Modal>
  );
}

// ── Gear Modal ──────────────────────────────

const GEAR_CATEGORIES: CategoryDef[] = [
  { value: 'camp', label: 'Camp', icon: '△' },
  { value: 'canoe', label: 'Canoe', icon: '◠' },
  { value: 'kitchen', label: 'Kitchen', icon: '◉' },
  { value: 'personal', label: 'Personal', icon: '◈' },
  { value: 'food_item', label: 'Food', icon: '◎' },
  { value: 'misc', label: 'Misc', icon: '◇' },
];

const GEAR_CONFIG: ChecklistModalConfig = {
  title: 'Equipment & Gear',
  icon: (
    <svg width="36" height="36" viewBox="0 0 48 48">
      <rect x="12" y="8" width="24" height="32" rx="6" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="2" />
      <rect x="16" y="12" width="16" height="10" rx="3" fill="var(--sage-deep)" />
      <path d="M15,14 Q12,28 15,36" fill="none" stroke="var(--sage-dark)" strokeWidth="2" strokeLinecap="round" />
      <path d="M33,14 Q36,28 33,36" fill="none" stroke="var(--sage-dark)" strokeWidth="2" strokeLinecap="round" />
    </svg>
  ),
  emptyText: 'No items yet — add some gear below.',
  loadingText: 'Unpacking the supply chest...',
  addPlaceholder: 'Add gear item...',
  sharedTitle: 'Shared Camp Gear',
  sharedSubtitle: 'For the whole group',
  personalDividerLabel: 'Personal Packs',
  categories: GEAR_CATEGORIES,
};

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  planOwnerId: string;
  members: PlanMember[];
  currentUserId: string;
}

export function GearModal(props: ModalProps) {
  return <ChecklistModal {...props} config={GEAR_CONFIG} />;
}

