import { toast } from './toastStore';

/** `{origin}/join/{token}` — the token is opaque, never parsed client-side. */
export function buildShareUrl(token: string): string {
  return `${window.location.origin}/join/${token}`;
}

export interface SharePlanLinkInput {
  url: string;
  planName: string;
}

export interface SharePlanLinkResult {
  /**
   * Set when neither the native share sheet nor the clipboard worked —
   * the caller shows this in a read-only, selectable field so the link
   * is still reachable (plain http://localhost, an old browser, a
   * clipboard permission denial).
   */
  fallbackUrl: string | null;
}

/**
 * Shares a plan's link: the native share sheet when available, else a
 * clipboard copy with a toast, else hands the URL back for the caller
 * to show in a selectable field.
 */
export async function sharePlanLink({ url, planName }: SharePlanLinkInput): Promise<SharePlanLinkResult> {
  if (navigator.share) {
    try {
      await navigator.share({ title: planName, url });
      return { fallbackUrl: null };
    } catch (error) {
      // A user-cancelled share sheet is not a failure — nothing else to do.
      if (error instanceof Error && error.name === 'AbortError') return { fallbackUrl: null };
      // Any other failure (no share target chosen, permission denied,
      // etc.) falls through to the clipboard below.
    }
  }

  try {
    await navigator.clipboard.writeText(url);
    toast.info('Link copied');
    return { fallbackUrl: null };
  } catch {
    return { fallbackUrl: url };
  }
}
