import { useState, useEffect, useCallback, useRef } from 'react';
import { api, type GearPackSummary, type GearPackDetail, type GearPackItemSearchResult } from '../api/client';
import { ConfirmModal } from './ui/ConfirmModal';
import './GearPacksPanel.css';

const ITEM_CATEGORIES = [
  { value: 'camp', label: 'Camp' },
  { value: 'canoe', label: 'Canoe' },
  { value: 'kitchen', label: 'Kitchen' },
  { value: 'personal', label: 'Personal' },
  { value: 'food_item', label: 'Food' },
  { value: 'misc', label: 'Misc' },
];

function PackIcon({ packName }: { packName: string }) {
  const name = packName.toLowerCase();
  if (name.includes('cooking') || name.includes('kitchen')) {
    return (
      <svg className="gear-pack-card-icon" width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
        <ellipse cx="10" cy="14" rx="7" ry="4" fill="none" stroke="currentColor" strokeWidth="1.4" />
        <path d="M5,14 Q5,8 10,7 Q15,8 15,14" fill="none" stroke="currentColor" strokeWidth="1.4" />
        <line x1="10" y1="7" x2="10" y2="4" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
        <line x1="7" y1="5" x2="7" y2="3" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" opacity="0.6" />
        <line x1="13" y1="5" x2="13" y2="3" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" opacity="0.6" />
      </svg>
    );
  }
  if (name.includes('sleep')) {
    return (
      <svg className="gear-pack-card-icon" width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
        <path d="M3,16 L10,5 L17,16 Z" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
        <line x1="6" y1="16" x2="14" y2="16" stroke="currentColor" strokeWidth="1.4" />
      </svg>
    );
  }
  if (name.includes('first aid') || name.includes('medical')) {
    return (
      <svg className="gear-pack-card-icon" width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
        <rect x="3" y="5" width="14" height="10" rx="2" fill="none" stroke="currentColor" strokeWidth="1.4" />
        <line x1="10" y1="8" x2="10" y2="12" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        <line x1="8" y1="10" x2="12" y2="10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </svg>
    );
  }
  // Default: backpack icon
  return (
    <svg className="gear-pack-card-icon" width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
      <rect x="5" y="6" width="10" height="11" rx="2" fill="none" stroke="currentColor" strokeWidth="1.4" />
      <path d="M7,6 V4.5 A3,3 0 0 1 13,4.5 V6" fill="none" stroke="currentColor" strokeWidth="1.4" />
      <rect x="7" y="10" width="6" height="3" rx="0.5" fill="none" stroke="currentColor" strokeWidth="1.2" />
    </svg>
  );
}

interface GearPacksPanelProps {
  planId: string;
  memberCount: number;
  canEdit: boolean;
  onItemsChanged: () => void;
  currentUserId: string;
}

export function GearPacksPanel({ planId, memberCount, canEdit, onItemsChanged, currentUserId }: GearPacksPanelProps) {
  const [expanded, setExpanded] = useState(false);
  const [packs, setPacks] = useState<GearPackSummary[]>([]);
  const [loading, setLoading] = useState(false);

  // Preview state
  const [previewPackId, setPreviewPackId] = useState<string | null>(null);
  const [packDetail, setPackDetail] = useState<GearPackDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [groupSize, setGroupSize] = useState(memberCount || 1);
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);
  const [applySuccess, setApplySuccess] = useState<string | null>(null);

  // Create form state
  const [createFormOpen, setCreateFormOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createDescription, setCreateDescription] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Edit state
  const [editingPackId, setEditingPackId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editDetail, setEditDetail] = useState<GearPackDetail | null>(null);
  const [loadingEditDetail, setLoadingEditDetail] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Delete confirm state
  const [deleteConfirmPackId, setDeleteConfirmPackId] = useState<string | null>(null);

  // Add item state
  const [addItemName, setAddItemName] = useState('');
  const [addItemCategory, setAddItemCategory] = useState('camp');
  const [addItemQuantity, setAddItemQuantity] = useState(1);
  const [addItemScalable, setAddItemScalable] = useState(false);
  const [addingItem, setAddingItem] = useState(false);
  const [addItemError, setAddItemError] = useState<string | null>(null);
  const [searchResults, setSearchResults] = useState<GearPackItemSearchResult[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadPacks = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.getGearPacks();
      setPacks(data);
    } catch {
      // fail silently
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (expanded && packs.length === 0) {
      loadPacks();
    }
  }, [expanded, packs.length, loadPacks]);

  useEffect(() => {
    setGroupSize(memberCount || 1);
  }, [memberCount]);

  // ── Preview ──────────────────────────────

  const handlePreview = async (packId: string) => {
    if (previewPackId === packId) {
      setPreviewPackId(null);
      setPackDetail(null);
      return;
    }
    setPreviewPackId(packId);
    setPackDetail(null);
    setLoadingDetail(true);
    try {
      const detail = await api.getGearPack(packId);
      setPackDetail(detail);
    } catch {
      setPreviewPackId(null);
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleApply = async (packId: string) => {
    setApplying(true);
    setApplyError(null);
    setApplySuccess(null);
    try {
      const result = await api.applyGearPack(packId, { planId, groupSize });
      setApplySuccess(`Added ${result.appliedCount} items to your gear list!`);
      setPreviewPackId(null);
      setPackDetail(null);
      onItemsChanged();
      setTimeout(() => setApplySuccess(null), 3000);
    } catch (e) {
      setApplyError(e instanceof Error ? e.message : 'Failed to apply gear pack');
    } finally {
      setApplying(false);
    }
  };

  const computeQuantity = (defaultQuantity: number, scalable: boolean): number => {
    return scalable ? defaultQuantity * groupSize : defaultQuantity;
  };

  // ── Create ───────────────────────────────

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createName.trim()) return;
    setCreating(true);
    setCreateError(null);
    try {
      await api.createGearPack({ name: createName.trim(), description: createDescription.trim() });
      setCreateFormOpen(false);
      setCreateName('');
      setCreateDescription('');
      setPacks([]); // force reload
      loadPacks();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create gear pack');
    } finally {
      setCreating(false);
    }
  };

  const handleCreateCancel = () => {
    setCreateFormOpen(false);
    setCreateName('');
    setCreateDescription('');
    setCreateError(null);
  };

  // ── Edit ─────────────────────────────────

  const handleEditStart = async (pack: GearPackSummary) => {
    setEditingPackId(pack.id);
    setEditName(pack.name);
    setEditDescription(pack.description);
    setEditDetail(null);
    setEditError(null);
    setAddItemName('');
    setAddItemCategory('camp');
    setAddItemError(null);
    setSearchResults([]);
    setShowSuggestions(false);
    // Close preview for this pack
    if (previewPackId === pack.id) {
      setPreviewPackId(null);
      setPackDetail(null);
    }

    setLoadingEditDetail(true);
    try {
      const detail = await api.getGearPack(pack.id);
      setEditDetail(detail);
    } catch {
      // fail silently
    } finally {
      setLoadingEditDetail(false);
    }
  };

  const handleEditCancel = () => {
    setEditingPackId(null);
    setEditDetail(null);
    setEditError(null);
    setAddItemName('');
    setAddItemCategory('camp');
    setAddItemError(null);
    setSearchResults([]);
    setShowSuggestions(false);
  };

  const handleEditSave = async () => {
    if (!editingPackId || !editName.trim()) return;
    setSaving(true);
    setEditError(null);
    try {
      await api.updateGearPack(editingPackId, { name: editName.trim(), description: editDescription.trim() });
      loadPacks();
    } catch (err) {
      setEditError(err instanceof Error ? err.message : 'Failed to save changes');
    } finally {
      setSaving(false);
    }
  };

  // ── Delete ───────────────────────────────

  const handleDeleteConfirm = async (packId: string) => {
    try {
      await api.deleteGearPack(packId);
      setDeleteConfirmPackId(null);
      if (editingPackId === packId) {
        setEditingPackId(null);
        setEditDetail(null);
      }
      if (previewPackId === packId) {
        setPreviewPackId(null);
        setPackDetail(null);
      }
      loadPacks();
      onItemsChanged();
    } catch {
      // fail silently
    }
  };

  // ── Item management ──────────────────────

  const handleAddItemNameChange = (value: string) => {
    setAddItemName(value);
    setShowSuggestions(false);

    if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);

    if (value.trim().length >= 2) {
      searchTimeoutRef.current = setTimeout(async () => {
        try {
          const results = await api.searchGearPackItems(value.trim());
          setSearchResults(results);
          setShowSuggestions(results.length > 0);
        } catch {
          setSearchResults([]);
        }
      }, 300);
    } else {
      setSearchResults([]);
    }
  };

  const handleSelectSuggestion = (result: GearPackItemSearchResult) => {
    setAddItemName(result.name);
    setAddItemCategory(result.category);
    setAddItemQuantity(result.defaultQuantity);
    setAddItemScalable(result.scalable);
    setShowSuggestions(false);
    setSearchResults([]);
  };

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPackId || !addItemName.trim()) return;
    setAddingItem(true);
    setAddItemError(null);
    setShowSuggestions(false);
    try {
      await api.addGearPackItem(editingPackId, {
        name: addItemName.trim(),
        category: addItemCategory,
        defaultQuantity: addItemQuantity,
        scalable: addItemScalable,
      });
      setAddItemName('');
      setAddItemCategory('camp');
      setAddItemQuantity(1);
      setAddItemScalable(false);
      setSearchResults([]);
      const detail = await api.getGearPack(editingPackId);
      setEditDetail(detail);
      loadPacks();
    } catch (err) {
      setAddItemError(err instanceof Error ? err.message : 'Failed to add item');
    } finally {
      setAddingItem(false);
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    if (!editingPackId) return;
    try {
      await api.removeGearPackItem(editingPackId, itemId);
      const detail = await api.getGearPack(editingPackId);
      setEditDetail(detail);
      loadPacks();
    } catch {
      // fail silently
    }
  };

  // ── Render ───────────────────────────────

  return (
    <div className="gear-packs-panel">
      <button className="gear-packs-header" onClick={() => setExpanded(!expanded)}>
        <div className="gear-packs-header-left">
          <svg className={`gear-packs-chevron ${expanded ? 'gear-packs-chevron--open' : ''}`} width="16" height="16" viewBox="0 0 16 16">
            <path d="M5,3 L11,8 L5,13" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <svg className="gear-packs-icon" width="18" height="18" viewBox="0 0 18 18">
            <rect x="2" y="4" width="14" height="11" rx="2" fill="none" stroke="currentColor" strokeWidth="1.5" />
            <path d="M6,4 V2.5 A1.5,1.5 0 0 1 7.5,1 h3 A1.5,1.5 0 0 1 12,2.5 V4" fill="none" stroke="currentColor" strokeWidth="1.5" />
            <line x1="2" y1="8" x2="16" y2="8" stroke="currentColor" strokeWidth="1.5" />
            <rect x="7" y="6.5" width="4" height="3" rx="0.5" fill="currentColor" />
          </svg>
          <span className="gear-packs-title">Gear Packs</span>
        </div>
        {packs.length > 0 && (
          <span className="gear-packs-badge">{packs.length} available</span>
        )}
      </button>

      {expanded && (
        <div className="gear-packs-body">
          {/* Create Pack */}
          {!createFormOpen ? (
            <button className="gear-pack-create-btn" onClick={() => setCreateFormOpen(true)}>
              + New Pack
            </button>
          ) : (
            <form className="gear-pack-create-form" onSubmit={handleCreateSubmit}>
              <input
                className="gear-pack-input"
                value={createName}
                onChange={e => setCreateName(e.target.value)}
                placeholder="Pack name"
                autoFocus
                disabled={creating}
              />
              <input
                className="gear-pack-input"
                value={createDescription}
                onChange={e => setCreateDescription(e.target.value)}
                placeholder="Description (optional)"
                disabled={creating}
              />
              {createError && <p className="gear-packs-error">{createError}</p>}
              <div className="gear-pack-form-actions">
                <button type="submit" className="gear-pack-apply-btn" disabled={creating || !createName.trim()}>
                  {creating ? '...' : 'Create'}
                </button>
                <button type="button" className="gear-pack-cancel-btn" onClick={handleCreateCancel}>
                  Cancel
                </button>
              </div>
            </form>
          )}

          {loading ? (
            <p className="gear-packs-loading">Loading gear packs...</p>
          ) : packs.length === 0 && !createFormOpen ? (
            <p className="gear-packs-empty">No gear packs available.</p>
          ) : (
            <>
              {applySuccess && (
                <div className="gear-packs-success">{applySuccess}</div>
              )}
              {applyError && (
                <div className="gear-packs-error">{applyError}</div>
              )}
              {packs.map(pack => {
                const isEditing = editingPackId === pack.id;
                const isOwned = pack.createdBy === currentUserId;

                if (isEditing) {
                  return (
                    <div key={pack.id} className="gear-pack-card gear-pack-card--editing">
                      {/* Edit name/description */}
                      <div className="gear-pack-edit-header">
                        <span className="gear-pack-edit-title">Editing: {pack.name}</span>
                        <button className="gear-pack-cancel-icon-btn" onClick={handleEditCancel} title="Close">
                          ✕
                        </button>
                      </div>
                      <div className="gear-pack-edit-fields">
                        <input
                          className="gear-pack-input"
                          value={editName}
                          onChange={e => setEditName(e.target.value)}
                          placeholder="Pack name"
                        />
                        <input
                          className="gear-pack-input"
                          value={editDescription}
                          onChange={e => setEditDescription(e.target.value)}
                          placeholder="Description (optional)"
                        />
                        {editError && <p className="gear-pack-error-inline">{editError}</p>}
                        <div className="gear-pack-form-actions">
                          <button
                            className="gear-pack-apply-btn"
                            onClick={handleEditSave}
                            disabled={saving || !editName.trim()}
                          >
                            {saving ? '...' : 'Save'}
                          </button>
                          <button className="gear-pack-cancel-btn" onClick={handleEditCancel}>
                            Cancel
                          </button>
                        </div>
                      </div>

                      {/* Item management */}
                      <div className="gear-pack-items-manager">
                        <h5 className="gear-pack-items-title">Items</h5>
                        {loadingEditDetail ? (
                          <p className="gear-packs-loading">Loading items...</p>
                        ) : editDetail ? (
                          <>
                            {editDetail.items.length === 0 ? (
                              <p className="gear-packs-empty">No items yet — add one below.</p>
                            ) : (
                              <div className="gear-pack-items-list">
                                {editDetail.items.map(item => (
                                  <div key={item.id} className="gear-pack-item-row gear-pack-item-row--manage">
                                    <span className="gear-pack-item-name">{item.name}</span>
                                    <span className="gear-pack-item-cat-badge">{item.category}</span>
                                    <span className="gear-pack-item-qty">
                                      ×{item.defaultQuantity}{item.scalable ? '/person' : ''}
                                    </span>
                                    <button
                                      className="gear-pack-remove-item-btn"
                                      onClick={() => handleRemoveItem(item.id)}
                                      title="Remove item"
                                    >
                                      ✕
                                    </button>
                                  </div>
                                ))}
                              </div>
                            )}

                            {/* Add item form */}
                            <form className="gear-pack-add-item-form" onSubmit={handleAddItem}>
                              <div className="gear-pack-search-wrapper">
                                <input
                                  className="gear-pack-input gear-pack-input--search"
                                  value={addItemName}
                                  onChange={e => handleAddItemNameChange(e.target.value)}
                                  onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
                                  placeholder="Item name..."
                                  disabled={addingItem}
                                />
                                {showSuggestions && searchResults.length > 0 && (
                                  <div className="gear-pack-suggestions">
                                    {searchResults.map(r => (
                                      <button
                                        key={r.id}
                                        type="button"
                                        className="gear-pack-suggestion"
                                        onMouseDown={() => handleSelectSuggestion(r)}
                                      >
                                        <span className="gear-pack-suggestion-name">{r.name}</span>
                                        <span className="gear-pack-suggestion-meta">from {r.gearPackName}</span>
                                      </button>
                                    ))}
                                  </div>
                                )}
                              </div>
                              <select
                                className="gear-pack-category-select"
                                value={addItemCategory}
                                onChange={e => setAddItemCategory(e.target.value)}
                                disabled={addingItem}
                              >
                                {ITEM_CATEGORIES.map(c => (
                                  <option key={c.value} value={c.value}>{c.label}</option>
                                ))}
                              </select>
                              <div className="gear-pack-qty-stepper">
                                <button
                                  type="button"
                                  className="gear-pack-qty-btn"
                                  onClick={() => setAddItemQuantity(q => Math.max(1, q - 1))}
                                  disabled={addingItem || addItemQuantity <= 1}
                                  title="Decrease quantity"
                                >
                                  −
                                </button>
                                <span className="gear-pack-qty-value">{addItemQuantity}</span>
                                <button
                                  type="button"
                                  className="gear-pack-qty-btn"
                                  onClick={() => setAddItemQuantity(q => q + 1)}
                                  disabled={addingItem}
                                  title="Increase quantity"
                                >
                                  +
                                </button>
                              </div>
                              <label className="gear-pack-scalable-label" title="Multiply quantity by group size when applied">
                                <input
                                  type="checkbox"
                                  checked={addItemScalable}
                                  onChange={e => setAddItemScalable(e.target.checked)}
                                  disabled={addingItem}
                                />
                                <span>Scale w/ group</span>
                              </label>
                              <button
                                type="submit"
                                className="gear-pack-apply-btn"
                                disabled={addingItem || !addItemName.trim()}
                              >
                                {addingItem ? '...' : 'Add'}
                              </button>
                            </form>
                            {addItemError && <p className="gear-packs-error">{addItemError}</p>}
                          </>
                        ) : null}
                      </div>
                    </div>
                  );
                }


                return (
                  <div key={pack.id} className="gear-pack-card">
                    <div className="gear-pack-card-header">
                      <div className="gear-pack-card-info">
                        <PackIcon packName={pack.name} />
                        <span className="gear-pack-card-name">{pack.name}</span>
                        <span className="gear-pack-card-count">{pack.itemCount} items</span>
                      </div>
                      <div className="gear-pack-card-actions">
                        {isOwned && (
                          <>
                            <button
                              className="gear-pack-edit-btn"
                              onClick={() => handleEditStart(pack)}
                            >
                              Edit
                            </button>
                            <button
                              className="gear-pack-delete-btn"
                              onClick={() => setDeleteConfirmPackId(pack.id)}
                            >
                              Delete
                            </button>
                          </>
                        )}
                        <button
                          className="gear-pack-preview-btn"
                          onClick={() => handlePreview(pack.id)}
                        >
                          {previewPackId === pack.id ? 'Hide' : 'Preview'}
                        </button>
                        {canEdit && (
                          <button
                            className="gear-pack-apply-btn"
                            onClick={() => handleApply(pack.id)}
                            disabled={applying}
                          >
                            {applying ? '...' : 'Apply'}
                          </button>
                        )}
                      </div>
                    </div>
                    <p className="gear-pack-card-description">{pack.description}</p>

                    {previewPackId === pack.id && (
                      <div className="gear-pack-preview">
                        {loadingDetail ? (
                          <p className="gear-packs-loading">Loading items...</p>
                        ) : packDetail ? (
                          <>
                            <div className="gear-pack-group-size">
                              <label className="gear-pack-group-label">Group size:</label>
                              <div className="gear-pack-group-stepper">
                                <button
                                  className="gear-qty-btn"
                                  onClick={() => setGroupSize(Math.max(1, groupSize - 1))}
                                  disabled={groupSize <= 1}
                                >
                                  −
                                </button>
                                <span className="gear-qty-value">{groupSize}</span>
                                <button
                                  className="gear-qty-btn"
                                  onClick={() => setGroupSize(groupSize + 1)}
                                >
                                  +
                                </button>
                              </div>
                            </div>
                            <div className="gear-pack-items-list">
                              {packDetail.items.map(item => {
                                const qty = computeQuantity(item.defaultQuantity, item.scalable);
                                return (
                                  <div key={item.id} className="gear-pack-item-row">
                                    <span className="gear-pack-item-name">{item.name}</span>
                                    <span className="gear-pack-item-qty">
                                      ×{qty}
                                      {item.scalable && (
                                        <span className="gear-pack-item-scaled" title="Scales with group size">↕</span>
                                      )}
                                    </span>
                                  </div>
                                );
                              })}
                            </div>
                          </>
                        ) : null}
                      </div>
                    )}
                  </div>
                );
              })}
            </>
          )}
        </div>
      )}

      {/* Delete gear pack confirmation */}
      <ConfirmModal
        isOpen={deleteConfirmPackId !== null}
        title="Delete Pack?"
        message="This will ungroup all items from plans that use this pack."
        confirmLabel="Delete"
        tone="danger"
        onConfirm={() => handleDeleteConfirm(deleteConfirmPackId!)}
        onCancel={() => setDeleteConfirmPackId(null)}
      />
    </div>
  );
}
