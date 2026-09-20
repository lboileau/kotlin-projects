import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Spinner } from '@radix-ui/themes';
import { PageLoader, type LoaderArea } from './PageLoader';
import './RouteFallback.css';

const SHOPPING_UNDER_PLAN = /^\/plans\/[^/]+\/shopping/;

function areaFor(pathname: string): LoaderArea | null {
  if (pathname.startsWith('/recipes') || pathname.startsWith('/ingredients')) return 'recipes';
  if (pathname.startsWith('/shopping') || SHOPPING_UNDER_PLAN.test(pathname)) return 'shopping';
  if (pathname.startsWith('/plans') || pathname.startsWith('/join')) return 'plans';
  return null;
}

/**
 * Suspense fallback for lazy routes. Delayed by ~150ms so a fast (cached
 * or small) chunk load never flashes anything — only a load that's
 * actually slow enough to notice shows the area's loading icon (or a plain
 * spinner outside the three tab areas).
 */
export function RouteFallback() {
  const [visible, setVisible] = useState(false);
  const { pathname } = useLocation();

  useEffect(() => {
    const timer = window.setTimeout(() => setVisible(true), 150);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) return null;

  const area = areaFor(pathname);
  if (area) return <PageLoader area={area} label="Loading" />;

  return (
    <div className="route-fallback" role="status" aria-label="Loading">
      <Spinner size="3" />
    </div>
  );
}
