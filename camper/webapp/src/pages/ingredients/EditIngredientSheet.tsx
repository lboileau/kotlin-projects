import { useParams } from 'react-router-dom';
import { PlaceholderSheet } from '../../components/Placeholder';

export function EditIngredientSheet() {
  const { ingredientId } = useParams<{ ingredientId: string }>();
  return (
    <PlaceholderSheet
      title={ingredientId ? `Edit ${ingredientId}` : 'Edit ingredient'}
      parentPath="/ingredients"
    />
  );
}
