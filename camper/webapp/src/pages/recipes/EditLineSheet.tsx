import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function EditLineSheet() {
  const { recipeId, lineId } = useParams<{ recipeId: string; lineId: string }>();
  return (
    <PlaceholderSheet
      title={lineId ? `Edit ingredient ${lineId}` : 'Edit ingredient'}
      parentPath={`/recipes/${recipeId}`}
    />
  );
}
