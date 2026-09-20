import { Button, Text } from '@radix-ui/themes';
import './QueryErrorState.css';

interface QueryErrorStateProps {
  /** Deliberately generic — the actual error (however technical) never shows verbatim. */
  message?: string;
  onRetry: () => void;
}

/** Shared error state for a query-driven screen: no raw error strings, always a way to retry. */
export function QueryErrorState({ message = "Couldn't load this.", onRetry }: QueryErrorStateProps) {
  return (
    <div className="query-error-state">
      <Text color="gray" size="2">
        {message}
      </Text>
      <Button variant="soft" size="3" onClick={onRetry}>
        Retry
      </Button>
    </div>
  );
}
