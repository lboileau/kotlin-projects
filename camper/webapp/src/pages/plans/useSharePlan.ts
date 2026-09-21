import { useRef, useState } from 'react';
import { ApiError } from '../../api/http';
import { useShareLink } from '../../queries/plans';
import { buildShareUrl, sharePlanLink } from '../../lib/shareLink';
import { toast } from '../../lib/toastStore';

/**
 * Sharing a plan, for both the top-bar Share button and the edit sheet.
 * The link is fetched only when `share()` runs (the first fetch creates the
 * plan's backing trip). `fallbackUrl` is set when neither the native share
 * sheet nor the clipboard is available, so the caller can show the link in a
 * selectable field.
 */
export function useSharePlan(planId: string | undefined, planName: string | undefined) {
  const shareLink = useShareLink(planId);
  const [fallbackUrl, setFallbackUrl] = useState<string | null>(null);
  // A ref, not `isFetching`: that only flips on the next render, so a fast
  // double tap would otherwise run two shares (two native share sheets).
  const sharingRef = useRef(false);

  async function share() {
    if (!planId || !planName || sharingRef.current) return;
    sharingRef.current = true;
    try {
      setFallbackUrl(null);
      // Reuse a token already fetched this session. Safari only allows
      // navigator.share / clipboard writes while the tap's user activation is
      // still live, and a network round trip in between can let it lapse —
      // so only the first share of a plan pays for a fetch before sharing.
      let token = shareLink.data?.token;
      if (!token) {
        const result = await shareLink.refetch();
        if (!result.data) {
          // 409: the plan belongs to a camping trip, which cannot be link-shared.
          // The server's message says so; anything else gets the generic copy.
          const shareError = result.error;
          const conflict = shareError instanceof ApiError && shareError.status === 409;
          toast.error(conflict ? shareError.message : "Couldn't get the share link.");
          return;
        }
        token = result.data.token;
      }
      const outcome = await sharePlanLink({ url: buildShareUrl(token), planName });
      if (outcome.fallbackUrl) setFallbackUrl(outcome.fallbackUrl);
    } finally {
      sharingRef.current = false;
    }
  }

  return { share, isSharing: shareLink.isFetching, fallbackUrl };
}
