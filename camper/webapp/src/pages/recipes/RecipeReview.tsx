import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { AlertDialog, Button, Callout, Heading, Text } from '@radix-ui/themes';
import { CheckIcon, ExclamationTriangleIcon, ReloadIcon } from '@radix-ui/react-icons';
import { SheetLink } from '../../components/SheetLink';
import { ApiError } from '../../api/http';
import { resolveRecipeIngredient, type RecipeDetailResponse, type RecipeIngredientResponse } from '../../api/recipes';
import { getIngredients, type IngredientResponse } from '../../api/ingredients';
import { createOrFindIngredient, createOrFindIngredientIn, ingredientsKey, useIngredients } from '../../queries/ingredients';
import { recipeKey, recipesKey, useResolveDuplicate } from '../../queries/recipes';
import { capitalize, normalizeCategory, normalizeUnit } from '../../lib/ingredientConstants';
import { formatQuantity } from '../../lib/formatQuantity';
import { asyncPool } from '../../lib/asyncPool';
import { toast } from '../../lib/toastStore';
import { IngredientLineRow } from './RecipeDetailPage';
import './RecipeReview.css';

const KNOWN_REVIEW_FLAGS: Record<string, string> = {
  ingredient_deleted: 'Its ingredient was deleted',
};

function humanizeFlag(flag: string): string {
  if (KNOWN_REVIEW_FLAGS[flag]) return KNOWN_REVIEW_FLAGS[flag];
  return flag
    .split('_')
    .filter(Boolean)
    .map(capitalize)
    .join(' ');
}

type ProposalKind = 'confirm' | 'select-existing' | 'create' | 'none';

interface Proposal {
  line: RecipeIngredientResponse;
  kind: ProposalKind;
  /** The ingredient id to resolve with, when already known (confirm / select-existing). */
  ingredientId?: string;
  /** Display name — the matched/existing ingredient's name, or the suggested new name for `create`. */
  name?: string;
}

function classifyProposal(line: RecipeIngredientResponse, ingredients: IngredientResponse[]): Proposal {
  if (line.matchedIngredient) {
    return { line, kind: 'confirm', ingredientId: line.matchedIngredient.id, name: line.matchedIngredient.name };
  }
  const suggested = line.suggestedIngredientName?.trim();
  if (!suggested) return { line, kind: 'none' };
  const existing = ingredients.find((i) => i.name.toLowerCase() === suggested.toLowerCase());
  if (existing) return { line, kind: 'select-existing', ingredientId: existing.id, name: existing.name };
  return { line, kind: 'create', name: suggested };
}

function proposalLabel(proposal: Proposal): string {
  switch (proposal.kind) {
    case 'confirm':
    case 'select-existing':
      return `→ ${proposal.name}`;
    case 'create':
      return `→ Create "${proposal.name}" (${capitalize(normalizeCategory(proposal.line.suggestedCategory))}, ${normalizeUnit(
        proposal.line.suggestedUnit,
      )})`;
    default:
      return 'No suggestion';
  }
}

interface LineState {
  status: 'pending' | 'error';
  error?: string;
}

export function RecipeReview({ recipe }: { recipe: RecipeDetailResponse }) {
  const queryClient = useQueryClient();
  const { data: ingredientsList } = useIngredients();
  const [lineStates, setLineStates] = useState<Record<string, LineState>>({});
  const [acceptingAll, setAcceptingAll] = useState(false);

  const pendingLines = recipe.ingredients.filter((line) => line.status === 'pending_review');
  const matchedLines = recipe.ingredients.filter((line) => line.status === 'approved');
  const proposals = pendingLines.map((line) => classifyProposal(line, ingredientsList ?? []));
  const acceptableCount = proposals.filter((p) => p.kind !== 'none').length;

  async function acceptOne(proposal: Proposal) {
    if (proposal.kind === 'none') return;
    const { line } = proposal;
    setLineStates((prev) => ({ ...prev, [line.id]: { status: 'pending' } }));
    try {
      let ingredientId = proposal.ingredientId;
      if (proposal.kind === 'create' && proposal.name) {
        const created = await createOrFindIngredient(queryClient, {
          name: proposal.name,
          category: normalizeCategory(line.suggestedCategory),
          defaultUnit: normalizeUnit(line.suggestedUnit),
        });
        ingredientId = created.id;
      }
      if (!ingredientId) throw new Error('Missing ingredient id.');
      await resolveRecipeIngredient(recipe.id, line.id, {
        action: proposal.kind === 'confirm' ? 'CONFIRM_MATCH' : 'SELECT_EXISTING',
        ingredientId,
        quantity: line.quantity,
        // The scraped line's own unit is also free text — normalise it the
        // same way, since the resolve endpoint's `unit` override goes
        // straight onto the same CHECK-constrained column.
        unit: normalizeUnit(line.unit),
      });
      setLineStates((prev) => {
        const next = { ...prev };
        delete next[line.id];
        return next;
      });
      queryClient.invalidateQueries({ queryKey: recipeKey(recipe.id) });
      queryClient.invalidateQueries({ queryKey: recipesKey });
      queryClient.invalidateQueries({ queryKey: ingredientsKey });
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Could not resolve this line.';
      setLineStates((prev) => ({ ...prev, [line.id]: { status: 'error', error: message } }));
    }
  }

  async function acceptAll() {
    const eligible = proposals.filter((p) => p.kind !== 'none');
    if (eligible.length === 0) return;

    setAcceptingAll(true);
    setLineStates((prev) => {
      const next = { ...prev };
      for (const p of eligible) next[p.line.id] = { status: 'pending' };
      return next;
    });

    try {
      // ONE fresh, case-insensitive-checked fetch shared by every create in
      // this batch — createOrFindIngredientIn checks it locally instead of
      // each concurrent create forcing its own network round trip.
      const freshList = await queryClient.fetchQuery({ queryKey: ingredientsKey, queryFn: getIngredients, staleTime: 0 });

      // De-duplicate CREATE proposals by lowercased name — several lines can propose the same new ingredient.
      const createGroups = new Map<string, { name: string; category: string; unit: string }>();
      for (const p of eligible) {
        if (p.kind !== 'create' || !p.name) continue;
        const key = p.name.toLowerCase();
        if (!createGroups.has(key)) {
          createGroups.set(key, {
            name: p.name,
            category: normalizeCategory(p.line.suggestedCategory),
            unit: normalizeUnit(p.line.suggestedUnit),
          });
        }
      }
      const groupEntries = [...createGroups.entries()];

      const createResults = await asyncPool(3, groupEntries, async ([, group]) =>
        createOrFindIngredientIn(queryClient, freshList, { name: group.name, category: group.category, defaultUnit: group.unit }),
      );

      const createdIngredientIds = new Map<string, string>();
      const failedGroupKeys = new Set<string>();
      createResults.forEach((result, i) => {
        const [key] = groupEntries[i];
        if (result.status === 'fulfilled' && result.value) {
          createdIngredientIds.set(key, result.value.id);
        } else {
          failedGroupKeys.add(key);
        }
      });

      const resolvable: Proposal[] = [];
      const erroredNow = new Map<string, string>();
      for (const p of eligible) {
        if (p.kind === 'create' && p.name && failedGroupKeys.has(p.name.toLowerCase())) {
          erroredNow.set(p.line.id, 'Could not create the ingredient.');
        } else {
          resolvable.push(p);
        }
      }
      if (erroredNow.size > 0) {
        setLineStates((prev) => {
          const next = { ...prev };
          for (const [lineId, message] of erroredNow) next[lineId] = { status: 'error', error: message };
          return next;
        });
      }

      await asyncPool(
        4,
        resolvable,
        async (p) => {
          const ingredientId = p.kind === 'create' && p.name ? createdIngredientIds.get(p.name.toLowerCase()) : p.ingredientId;
          if (!ingredientId) throw new Error('Missing ingredient id.');
          await resolveRecipeIngredient(recipe.id, p.line.id, {
            action: p.kind === 'confirm' ? 'CONFIRM_MATCH' : 'SELECT_EXISTING',
            ingredientId,
            quantity: p.line.quantity,
            unit: normalizeUnit(p.line.unit),
          });
          return p.line.id;
        },
        (result) => {
          setLineStates((prev) => {
            const next = { ...prev };
            if (result.status === 'fulfilled' && result.value) {
              delete next[result.value];
            } else {
              const failed = resolvable[result.index];
              const message = result.reason instanceof ApiError ? result.reason.message : 'Could not save this line.';
              next[failed.line.id] = { status: 'error', error: message };
            }
            return next;
          });
        },
      );
    } finally {
      queryClient.invalidateQueries({ queryKey: recipeKey(recipe.id) });
      queryClient.invalidateQueries({ queryKey: recipesKey });
      queryClient.invalidateQueries({ queryKey: ingredientsKey });
      setAcceptingAll(false);
    }
  }

  return (
    <div className="recipe-review">
      {recipe.duplicateOf && <DuplicateBanner recipe={recipe} />}

      {pendingLines.length > 0 && (
        <div className="recipe-review__section">
          <div className="recipe-review__section-header">
            <Heading size="2" color="gray">
              Needs review ({pendingLines.length})
            </Heading>
            {acceptableCount >= 2 && (
              <Button size="1" variant="soft" onClick={acceptAll} loading={acceptingAll} disabled={acceptingAll}>
                Accept all ({acceptableCount})
              </Button>
            )}
          </div>
          <ul className="recipe-review__list">
            {proposals.map((proposal) => (
              <PendingLineRow
                key={proposal.line.id}
                recipeId={recipe.id}
                proposal={proposal}
                lineState={lineStates[proposal.line.id]}
                onAccept={() => acceptOne(proposal)}
              />
            ))}
          </ul>
        </div>
      )}

      {matchedLines.length > 0 && (
        <div className="recipe-review__section">
          <Heading size="2" color="gray">
            Matched ({matchedLines.length})
          </Heading>
          <ul className="recipe-detail-page__lines">
            {matchedLines.map((line) => (
              <IngredientLineRow key={line.id} line={line} recipeId={recipe.id} clickable />
            ))}
          </ul>
        </div>
      )}

      {recipe.ingredients.length === 0 && (
        <Text as="p" size="2" color="gray">
          No ingredients yet.
        </Text>
      )}
    </div>
  );
}

function DuplicateBanner({ recipe }: { recipe: RecipeDetailResponse }) {
  const navigate = useNavigate();
  const resolveDuplicate = useResolveDuplicate(recipe.id);
  const [pendingAction, setPendingAction] = useState<'NOT_DUPLICATE' | 'USE_EXISTING' | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  if (!recipe.duplicateOf) return null;
  const duplicateOf = recipe.duplicateOf;

  async function markNotDuplicate() {
    setPendingAction('NOT_DUPLICATE');
    try {
      await resolveDuplicate.mutateAsync('NOT_DUPLICATE');
      toast.info('Marked as not a duplicate.');
    } finally {
      setPendingAction(null);
    }
  }

  async function useExisting() {
    setPendingAction('USE_EXISTING');
    try {
      await resolveDuplicate.mutateAsync('USE_EXISTING');
      toast.info('Kept the existing recipe.');
      navigate(`/recipes/${duplicateOf.id}`, { replace: true });
    } finally {
      setPendingAction(null);
    }
  }

  return (
    <Callout.Root color="amber" variant="surface" className="recipe-review__banner">
      <Callout.Icon>
        <ExclamationTriangleIcon />
      </Callout.Icon>
      <Callout.Text>
        Possible duplicate of <Link to={`/recipes/${duplicateOf.id}`}>{duplicateOf.name}</Link>.
      </Callout.Text>
      <div className="recipe-review__banner-actions">
        <Button
          size="1"
          variant="soft"
          disabled={resolveDuplicate.isPending}
          loading={pendingAction === 'NOT_DUPLICATE'}
          onClick={markNotDuplicate}
        >
          Not a duplicate
        </Button>
        <AlertDialog.Root open={confirmOpen} onOpenChange={setConfirmOpen}>
          <AlertDialog.Trigger>
            <Button size="1" variant="soft" color="red" disabled={resolveDuplicate.isPending}>
              Use existing
            </Button>
          </AlertDialog.Trigger>
          <AlertDialog.Content maxWidth="380px">
            <AlertDialog.Title>Use the existing recipe?</AlertDialog.Title>
            <AlertDialog.Description>
              This deletes this draft (&ldquo;{recipe.name}&rdquo;) and keeps &ldquo;{duplicateOf.name}&rdquo; instead. This
              can&apos;t be undone.
            </AlertDialog.Description>
            <div className="recipe-review__dialog-actions">
              <AlertDialog.Cancel>
                <Button variant="soft">Cancel</Button>
              </AlertDialog.Cancel>
              <AlertDialog.Action>
                <Button color="red" loading={pendingAction === 'USE_EXISTING'} onClick={useExisting}>
                  Use existing
                </Button>
              </AlertDialog.Action>
            </div>
          </AlertDialog.Content>
        </AlertDialog.Root>
      </div>
    </Callout.Root>
  );
}

function PendingLineRow({
  recipeId,
  proposal,
  lineState,
  onAccept,
}: {
  recipeId: string;
  proposal: Proposal;
  lineState: LineState | undefined;
  onAccept: () => void;
}) {
  const { line } = proposal;
  const isPending = lineState?.status === 'pending';
  const isError = lineState?.status === 'error';

  return (
    <li className={`recipe-review__row${proposal.kind === 'none' ? ' recipe-review__row--needs-attention' : ''}`}>
      <SheetLink to={`/recipes/${recipeId}/lines/${line.id}`} className="recipe-review__row-main">
        <Text as="span" size="1" color="gray" className="recipe-review__original">
          &ldquo;{line.originalText ?? 'No scraped text'}&rdquo;
        </Text>
        <Text as="span" size="2">
          {formatQuantity(line.quantity)} {line.unit}
        </Text>
        <Text as="span" size="2" weight="medium" color={proposal.kind === 'none' ? 'red' : undefined}>
          {proposalLabel(proposal)}
        </Text>
        {line.reviewFlags.length > 0 && (
          <div className="recipe-review__flags">
            {line.reviewFlags.map((flag) => (
              <Text as="span" key={flag} size="1" color="gray">
                {humanizeFlag(flag)}
              </Text>
            ))}
          </div>
        )}
        {isError && (
          <Text as="span" size="1" color="red">
            {lineState?.error}
          </Text>
        )}
      </SheetLink>
      {proposal.kind !== 'none' && (
        <Button
          size="3"
          variant="soft"
          color={isError ? 'red' : undefined}
          loading={isPending}
          onClick={onAccept}
          className="recipe-review__accept-button"
        >
          {isError ? (
            <>
              <ReloadIcon /> Retry
            </>
          ) : (
            <>
              <CheckIcon /> Accept
            </>
          )}
        </Button>
      )}
    </li>
  );
}
