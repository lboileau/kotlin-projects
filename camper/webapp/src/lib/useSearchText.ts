import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * A search box whose text lives in the URL (`?q=`) without being driven by
 * it. The input used to be controlled straight from the search param, and
 * React Router applies URL updates asynchronously, so React kept resetting
 * the field to a stale value between keystrokes: fast typing dropped
 * characters ("zzzz" left "z").
 *
 * Here the input is controlled by local state, which is what filtering
 * should read too, and the URL follows it after a short pause (`replace`, so
 * typing adds no history entries). A change to the param from outside — Back
 * or Forward, a "Clear filters" button — flows back into the field.
 */
export function useSearchText(param = 'q', delayMs = 200): [string, (text: string) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const fromUrl = searchParams.get(param) ?? '';
  const [text, setText] = useState(fromUrl);
  // The last value this hook wrote to the URL, to tell its own writes (which
  // may already be behind what has been typed since) from outside ones.
  const [written, setWritten] = useState(fromUrl);
  const [seenUrl, setSeenUrl] = useState(fromUrl);

  // Derived during render rather than in an effect: the URL changed, and not
  // because of this hook, so the field follows it.
  if (fromUrl !== seenUrl) {
    setSeenUrl(fromUrl);
    if (fromUrl !== written) {
      setWritten(fromUrl);
      setText(fromUrl);
    }
  }

  useEffect(() => {
    if (text === written) return;
    const timer = window.setTimeout(() => {
      setWritten(text);
      setSearchParams(
        (previous) => {
          const next = new URLSearchParams(previous);
          if (text) next.set(param, text);
          else next.delete(param);
          return next;
        },
        { replace: true },
      );
    }, delayMs);
    return () => window.clearTimeout(timer);
  }, [text, written, param, delayMs, setSearchParams]);

  return [text, setText];
}
