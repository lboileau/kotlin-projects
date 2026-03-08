import { useState, useEffect, useCallback, useRef } from 'react';
import { api, type Item, type PlanMember } from '../api/client';
import './GearModal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  planOwnerId: string;
  members: PlanMember[];
  currentUserId: string;
}

const CATEGORIES = [
  { value: 'camp', label: 'Camp', icon: '△' },
  { value: 'canoe', label: 'Canoe', icon: '◠' },
  { value: 'kitchen', label: 'Kitchen', icon: '◉' },
  { value: 'personal', label: 'Personal', icon: '◈' },
  { value: 'food_item', label: 'Food', icon: '◎' },
  { value: 'misc', label: 'Misc', icon: '◇' },
];

function getCategoryIcon(category: string): string {
  return CATEGORIES.find(c => c.value === category)?.icon ?? '◇';
}

function getCategoryLabel(category: string): string {
  return CATEGORIES.find(c => c.value === category)?.label ?? category;
}

interface ItemRowProps {
  item: Item;
  canEdit: boolean;
  onTogglePacked: (item: Item) => void;
  onDelete: (item: Item) => void;
  onUpdate: (item: Item, name: string, quantity: number, category: string) => void;
}

function ItemRow({ item, canEdit, onTogglePacked, onDelete, onUpdate }: ItemRowProps) {
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
          {CATEGORIES.map(c => (
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
      <span className="gear-item-category-icon" title={getCategoryLabel(item.category)}>
        {getCategoryIcon(item.category)}
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
  onAdd: (name: string, category: string, quantity: number) => Promise<void>;
}

function AddItemForm({ onAdd }: AddItemFormProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('camp');
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
        placeholder="Add gear item..."
        disabled={adding}
      />
      <select
        className="gear-add-category"
        value={category}
        onChange={e => setCategory(e.target.value)}
        disabled={adding}
      >
        {CATEGORIES.map(c => (
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
  ownerType: string;
  ownerId: string;
  onRefresh: () => void;
  defaultExpanded?: boolean;
  accentColor?: string;
  canEdit: boolean;
}

function ChecklistSection({ title, subtitle, items, ownerType, ownerId, onRefresh, defaultExpanded = true, accentColor, canEdit }: ChecklistSectionProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  const packedCount = items.filter(i => i.packed).length;
  const totalCount = items.length;
  const progress = totalCount > 0 ? (packedCount / totalCount) * 100 : 0;

  const handleAdd = async (name: string, category: string, quantity: number) => {
    await api.createItem({ name, category, quantity, packed: false, ownerType, ownerId });
    onRefresh();
  };

  const handleTogglePacked = async (item: Item) => {
    await api.updateItem(item.id, { name: item.name, category: item.category, quantity: item.quantity, packed: !item.packed });
    onRefresh();
  };

  const handleDelete = async (item: Item) => {
    await api.deleteItem(item.id);
    onRefresh();
  };

  const handleUpdate = async (item: Item, name: string, quantity: number, category: string) => {
    await api.updateItem(item.id, { name, category, quantity, packed: item.packed });
    onRefresh();
  };

  // Group items by category
  const grouped = items.reduce<Record<string, Item[]>>((acc, item) => {
    (acc[item.category] ??= []).push(item);
    return acc;
  }, {});

  const categoryOrder = CATEGORIES.map(c => c.value);
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
                <span className="gear-category-icon">{getCategoryIcon(cat)}</span>
                <span>{getCategoryLabel(cat)}</span>
              </div>
              {grouped[cat].map(item => (
                <ItemRow
                  key={item.id}
                  item={item}
                  canEdit={canEdit}
                  onTogglePacked={handleTogglePacked}
                  onDelete={handleDelete}
                  onUpdate={handleUpdate}
                />
              ))}
            </div>
          ))}
          {totalCount === 0 && (
            <p className="gear-empty">{canEdit ? 'No items yet — add some gear below.' : 'No items yet.'}</p>
          )}
          {canEdit && <AddItemForm onAdd={handleAdd} />}
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

export function GearModal({ isOpen, onClose, planId, planOwnerId, members, currentUserId }: Props) {
  const [planItems, setPlanItems] = useState<Item[]>([]);
  const [memberItems, setMemberItems] = useState<Record<string, Item[]>>({});
  const [loading, setLoading] = useState(true);

  const loadItems = useCallback(async () => {
    try {
      const planData = await api.getItems('plan', planId);
      setPlanItems(planData);

      const memberData: Record<string, Item[]> = {};
      const results = await Promise.all(
        members.map(m => api.getItems('user', m.userId).then(items => ({ userId: m.userId, items })))
      );
      for (const r of results) {
        memberData[r.userId] = r.items;
      }
      setMemberItems(memberData);
    } catch {
      // fail silently
    } finally {
      setLoading(false);
    }
  }, [planId, members]);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      loadItems();
    }
  }, [isOpen, loadItems]);

  if (!isOpen) return null;

  // Sort members: current user first, then named, then pending
  const sortedMembers = [...members].sort((a, b) => {
    if (a.userId === currentUserId) return -1;
    if (b.userId === currentUserId) return 1;
    if (a.username && !b.username) return -1;
    if (!a.username && b.username) return 1;
    return 0;
  });

  const totalPlanPacked = planItems.filter(i => i.packed).length;
  const totalPlanCount = planItems.length;
  const allMemberItems = Object.values(memberItems).flat();
  const totalPersonalPacked = allMemberItems.filter(i => i.packed).length;
  const totalPersonalCount = allMemberItems.length;
  const grandTotal = totalPlanCount + totalPersonalCount;
  const grandPacked = totalPlanPacked + totalPersonalPacked;

  return (
    <div className="modal-overlay gear-modal-overlay" onClick={onClose}>
      <div className="gear-modal" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="gear-modal-header">
          <div className="gear-modal-header-left">
            <div className="gear-modal-icon">
              <svg width="36" height="36" viewBox="0 0 48 48">
                <rect x="12" y="8" width="24" height="32" rx="6" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="2" />
                <rect x="16" y="12" width="16" height="10" rx="3" fill="var(--sage-deep)" />
                <path d="M15,14 Q12,28 15,36" fill="none" stroke="var(--sage-dark)" strokeWidth="2" strokeLinecap="round" />
                <path d="M33,14 Q36,28 33,36" fill="none" stroke="var(--sage-dark)" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <h2 className="gear-modal-title">Equipment & Gear</h2>
              <p className="gear-modal-subtitle">
                {grandTotal === 0
                  ? 'Start packing for your adventure'
                  : `${grandPacked} of ${grandTotal} items packed`}
              </p>
            </div>
          </div>
          <button className="gear-modal-close" onClick={onClose} aria-label="Close">
            <svg width="20" height="20" viewBox="0 0 20 20">
              <path d="M5,5 L15,15 M15,5 L5,15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        {/* Overall progress */}
        {grandTotal > 0 && (
          <div className="gear-overall-progress">
            <div className="gear-overall-bar">
              <div className="gear-overall-fill" style={{ width: `${grandTotal > 0 ? (grandPacked / grandTotal) * 100 : 0}%` }} />
            </div>
          </div>
        )}

        {/* Content */}
        <div className="gear-modal-body">
          {loading ? (
            <div className="gear-loading">
              <div className="gear-loading-pack" />
              <p>Unpacking the supply chest...</p>
            </div>
          ) : (
            <>
              {/* Plan checklist */}
              <ChecklistSection
                title="Shared Camp Gear"
                subtitle="For the whole group"
                items={planItems}
                ownerType="plan"
                ownerId={planId}
                onRefresh={loadItems}
                defaultExpanded={true}
                canEdit={currentUserId === planOwnerId}
              />

              {/* Divider */}
              <div className="gear-divider">
                <svg width="200" height="16" viewBox="0 0 200 16">
                  <path d="M0,8 Q50,2 100,8 Q150,14 200,8" fill="none" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.5" />
                </svg>
                <span className="gear-divider-label">Personal Packs</span>
                <svg width="200" height="16" viewBox="0 0 200 16">
                  <path d="M0,8 Q50,14 100,8 Q150,2 200,8" fill="none" stroke="var(--tan-deep)" strokeWidth="1" opacity="0.5" />
                </svg>
              </div>

              {/* Member checklists */}
              {sortedMembers.map((member, i) => {
                const items = memberItems[member.userId] ?? [];
                const isCurrentUser = member.userId === currentUserId;
                const displayName = member.username || 'Pending Adventurer';
                return (
                  <ChecklistSection
                    key={member.userId}
                    title={isCurrentUser ? `${displayName} (You)` : displayName}
                    items={items}
                    ownerType="user"
                    ownerId={member.userId}
                    onRefresh={loadItems}
                    defaultExpanded={isCurrentUser}
                    accentColor={MEMBER_COLORS[i % MEMBER_COLORS.length]}
                    canEdit={isCurrentUser}
                  />
                );
              })}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
