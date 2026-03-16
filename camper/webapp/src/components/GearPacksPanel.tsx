import { useState, useEffect, useCallback } from 'react';
import { api, type GearPackSummary, type GearPackDetail } from '../api/client';
import './GearPacksPanel.css';

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
}

export function GearPacksPanel({ planId, memberCount, canEdit, onItemsChanged }: GearPacksPanelProps) {
  const [expanded, setExpanded] = useState(false);
  const [packs, setPacks] = useState<GearPackSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [previewPackId, setPreviewPackId] = useState<string | null>(null);
  const [packDetail, setPackDetail] = useState<GearPackDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [groupSize, setGroupSize] = useState(memberCount || 1);
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);
  const [applySuccess, setApplySuccess] = useState<string | null>(null);

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
          {loading ? (
            <p className="gear-packs-loading">Loading gear packs...</p>
          ) : packs.length === 0 ? (
            <p className="gear-packs-empty">No gear packs available.</p>
          ) : (
            <>
              {applySuccess && (
                <div className="gear-packs-success">{applySuccess}</div>
              )}
              {applyError && (
                <div className="gear-packs-error">{applyError}</div>
              )}
              {packs.map(pack => (
                <div key={pack.id} className="gear-pack-card">
                  <div className="gear-pack-card-header">
                    <div className="gear-pack-card-info">
                      <PackIcon packName={pack.name} />
                      <span className="gear-pack-card-name">{pack.name}</span>
                      <span className="gear-pack-card-count">{pack.itemCount} items</span>
                    </div>
                    <div className="gear-pack-card-actions">
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
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}
