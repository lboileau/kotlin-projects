import type { AvatarResponse } from '../api/client';
import { SKIN_COLORS, HAIR_COLORS } from '../lib/avatarConstants';
import { AvatarHair } from './AvatarHair';

/**
 * Renders a circular avatar head with hair and facial features.
 * Uses the exact same SVG structure as MiniAvatar in AssignmentsModal.
 */
export function AvatarHead({ avatar, size = 28 }: { avatar?: AvatarResponse | null; size?: number }) {
  if (!avatar) {
    return (
      <svg width={size} height={size} viewBox="0 0 28 28">
        <defs><clipPath id="avatar-head-clip"><circle cx="14" cy="14" r="13" /></clipPath></defs>
        <circle cx="14" cy="14" r="13" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="1.5" />
        <g clipPath="url(#avatar-head-clip)">
          <circle cx="14" cy="11" r="5" fill="var(--parchment)" />
          <ellipse cx="14" cy="24" rx="8" ry="6" fill="var(--parchment)" />
        </g>
      </svg>
    );
  }

  const skin = SKIN_COLORS[avatar.skinColor] || '#D4A574';
  const hair = HAIR_COLORS[avatar.hairColor] || '#4A3020';

  return (
    <svg width={size} height={size} viewBox="0 0 28 28">
      {/* Face — same as MiniAvatar: cx=14, cy=16, r=11 */}
      <circle cx="14" cy="16" r="11" fill={skin} />
      {/* Hair — same transform as MiniAvatar */}
      <g transform="translate(-13.5, 1) scale(1.15)">
        <AvatarHair style={avatar.hairStyle} color={hair} />
      </g>
      {/* Eyes */}
      <ellipse cx="10" cy="16" rx="1.6" ry="1.3" fill="#2A2A2A" />
      <ellipse cx="18" cy="16" rx="1.6" ry="1.3" fill="#2A2A2A" />
      <circle cx="10.5" cy="15.6" r="0.5" fill="rgba(255,255,255,0.8)" />
      <circle cx="18.5" cy="15.6" r="0.5" fill="rgba(255,255,255,0.8)" />
      {/* Mouth */}
      <path d="M11,20 Q14,21.5 17,20" fill="none" stroke="#3A2A2A" strokeWidth="0.8" strokeLinecap="round" />
    </svg>
  );
}
