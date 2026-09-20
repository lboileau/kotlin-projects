import { useState } from 'react';
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

  async function share() {
    if (!planId || !planName) return;
    setFallbackUrl(null);
    const result = await shareLink.refetch();
    if (!result.data) {
      // 409: the plan belongs to a camping trip, which cannot be link-shared.
      // The server's message says so; anything else gets the generic copy.
      const shareError = result.error;
      const conflict = shareError instanceof ApiError && shareError.status === 409;
      toast.error(conflict ? shareError.message : "Couldn't get the share link.");
      return;
    }
    const outcome = await sharePlanLink({ url: buildShareUrl(result.data.token), planName });
    if (outcome.fallbackUrl) setFallbackUrl(outcome.fallbackUrl);
  }

  return { share, isSharing: shareLink.isFetching, fallbackUrl };
}
