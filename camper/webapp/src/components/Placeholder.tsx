import type { ReactNode } from 'react';
import { Text } from '@radix-ui/themes';
import { PageHeader } from './PageHeader';
import { Sheet } from './Sheet';
import { useCloseSheet } from './useCloseSheet';
import './Placeholder.css';

interface PlaceholderPageProps {
  title: string;
  backTo?: string;
  actions?: ReactNode;
}

/** Stand-in for a page not built yet, so navigation, the tab bar, and deep links can all be exercised. */
export function PlaceholderPage({ title, backTo, actions }: PlaceholderPageProps) {
  return (
    <div className="placeholder-page">
      <PageHeader title={title} backTo={backTo} actions={actions} />
      <div className="placeholder-page__body">
        <Text color="gray" size="2">
          Coming soon.
        </Text>
      </div>
    </div>
  );
}

interface PlaceholderSheetProps {
  title: string;
  parentPath: string;
  fullHeight?: boolean;
}

/** Stand-in for a sheet not built yet. */
export function PlaceholderSheet({ title, parentPath, fullHeight }: PlaceholderSheetProps) {
  const closeSheet = useCloseSheet(parentPath);
  return (
    <Sheet title={title} onClose={closeSheet} fullHeight={fullHeight}>
      <Text color="gray" size="2">
        Coming soon.
      </Text>
    </Sheet>
  );
}
