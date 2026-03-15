import type { AvatarResponse } from '../api/client';
import { SKIN_COLORS, HAIR_COLORS, SHIRT_COLORS, PANTS_COLORS } from '../lib/avatarConstants';
import { AvatarHair } from './AvatarHair';

interface AvatarPreviewProps {
  avatar: AvatarResponse | null;
  size?: number;
}

export function AvatarPreview({ avatar, size = 80 }: AvatarPreviewProps) {
  if (!avatar) return <p style={{ fontSize: '0.85rem', fontStyle: 'italic', color: 'var(--charcoal-light)', opacity: 0.6 }}>No avatar generated yet.</p>;

  const skin = SKIN_COLORS[avatar.skinColor] || '#D4A574';
  const hood = HAIR_COLORS[avatar.hairColor] || '#4A3020';
  const body = SHIRT_COLORS[avatar.shirtColor] || '#3A5A8A';
  const accent = PANTS_COLORS[avatar.pantsColor] || '#2A3A5A';

  const height = size * (106 / 80);

  return (
    <svg width={size} height={height} viewBox="0 0 48 64">
      <circle cx="24" cy="14" r="11" fill={skin} />
      <AvatarHair style={avatar.hairStyle} color={hood} />
      <ellipse cx="20" cy="14" rx="1.8" ry="1.4" fill="#2A2A2A" />
      <ellipse cx="28" cy="14" rx="1.8" ry="1.4" fill="#2A2A2A" />
      <circle cx="20.6" cy="13.6" r="0.6" fill="rgba(255,255,255,0.8)" />
      <circle cx="28.6" cy="13.6" r="0.6" fill="rgba(255,255,255,0.8)" />
      <path d="M21,18 Q24,19.5 27,18" fill="none" stroke="#3A2A2A" strokeWidth="1" strokeLinecap="round" />
      <rect x="13" y="26" width="22" height="24" rx="4" fill={body} />
      <path d="M15,26 L20,30 L24,28 L28,30 L33,26" fill={accent} />
      <line x1="24" y1="30" x2="24" y2="50" stroke={hood} strokeWidth="1" opacity="0.4" />
      <rect x="7" y="28" width="9" height="16" rx="4.5" fill={body} />
      <rect x="32" y="28" width="9" height="16" rx="4.5" fill={body} />
      <ellipse cx="19" cy="52" rx="6" ry="5" fill={accent} />
      <ellipse cx="29" cy="52" rx="6" ry="5" fill={accent} />
      <rect x="12" y="54" width="13" height="7" rx="3" fill="#3A2A1A" />
      <rect x="23" y="54" width="13" height="7" rx="3" fill="#3A2A1A" />
    </svg>
  );
}
