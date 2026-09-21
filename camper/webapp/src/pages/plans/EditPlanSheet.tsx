import { useEffect, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Badge, Button, Separator, Skeleton, Spinner, Text, TextField } from '@radix-ui/themes';
import { CopyIcon, Cross2Icon, ExitIcon, Link2Icon, Share2Icon, TrashIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { QueryErrorState } from '../../components/QueryErrorState';
import {
  planKey,
  plansKey,
  shoppingKey,
  useDeletePlan,
  useDuplicatePlan,
  usePlan,
  usePlanMembers,
  useRemoveMember,
  useUpdatePlan,
} from '../../queries/plans';
import type { MealPlanResponse } from '../../api/mealPlans';
import { ApiError } from '../../api/http';
import { useAuth } from '../../auth/useAuth';
import { buildMealPlanSummary } from '../../lib/mealPlanSummary';
import { useSharePlan } from './useSharePlan';
import { clearSelectedPlanId, getSelectedPlanId } from '../../lib/selectedPlan';
import { toast } from '../../lib/toastStore';
import './EditPlanSheet.css';
import { RowActionButton } from '../../components/RowActionButton';

export function EditPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  const sheet = useSheet(`/plans/${planId}`);
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const { data: plan, isError, error, refetch } = usePlan(planId);
  const sharePlan = useSharePlan(planId, plan?.name);
  const members = usePlanMembers(planId);
  const updatePlan = useUpdatePlan(planId ?? '');
  const deletePlan = useDeletePlan();
  const duplicatePlan = useDuplicatePlan();
  const removeMember = useRemoveMember(planId ?? '');

  // No effect needed to seed this from `plan`: until the field is
  // touched this session, the displayed value just falls through to the
  // server's — once touched, the override takes over.
  const [nameOverride, setNameOverride] = useState<string | null>(null);
  const name = nameOverride ?? plan?.name ?? '';

  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [confirmingLeave, setConfirmingLeave] = useState(false);
  const [confirmingRemoveUserId, setConfirmingRemoveUserId] = useState<string | null>(null);

  const dirty = name.trim().length > 0 && name.trim() !== plan?.name;
  // Derived from the plan's own `role` field (never from comparing ids
  // client-side) — while `plan` hasn't loaded yet, the sheet is still in
  // its loading branch below, so owner-only controls never flash before
  // the role is actually known.
  const isOwner = plan?.role === 'owner';

  // Same derivation as PlanDetailPage/ShoppingPage. Needed here too: a
  // stale `plan` stays in cache once fetched, so without this the sheet
  // would keep showing full interactive controls (rename, remove a
  // member, etc.) over a page that's already switched to its "not
  // found"/"no access" state underneath — e.g. the owner removes this
  // user via the `members` event while they still have this sheet open.
  const notFound = isError && error instanceof ApiError && error.status === 404;
  const forbidden = isError && error instanceof ApiError && error.status === 403;

  useEffect(() => {
    if (notFound || forbidden) sheet.close();
    // Only reacts to the plan becoming inaccessible — `sheet.close` is a
    // plain function recreated every render, not a stable dependency.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [notFound, forbidden]);

  function handleRename(event: FormEvent) {
    event.preventDefault();
    if (!dirty) return;
    updatePlan.mutate({ name: name.trim() });
  }

  function handleDuplicate() {
    if (!planId) return;
    // On error the global mutation error toast already surfaces it — stay
    // on the sheet so the user can retry.
    duplicatePlan.mutate(
      { planId },
      // Replace, so Back from the copy doesn't reopen this sheet on the original.
      { onSuccess: (newPlan) => sheet.close({ to: `/plans/${newPlan.id}`, replace: true }) },
    );
  }

  async function handleCopySummary() {
    if (!plan) return;
    const summary = buildMealPlanSummary(plan);
    try {
      await navigator.clipboard.writeText(summary);
      toast.info('Copied');
    } catch {
      toast.error('Could not copy to the clipboard.');
    }
  }

  function handleConfirmDelete() {
    if (!planId) return;
    deletePlan.mutate(planId, {
      onSuccess: () => {
        if (getSelectedPlanId() === planId) clearSelectedPlanId();
        sheet.close({ to: '/plans', replace: true });
      },
    });
  }

  function handleRemoveMember(userId: string) {
    removeMember.mutate(
      { userId },
      {
        onSettled: () => setConfirmingRemoveUserId(null),
      },
    );
  }

  function handleLeave() {
    if (!planId || !plan || !user) return;
    removeMember.mutate(
      { userId: user.id, isSelf: true },
      {
        onSuccess: () => {
          if (getSelectedPlanId() === planId) clearSelectedPlanId();
          queryClient.setQueryData<MealPlanResponse[]>(plansKey, (current) =>
            current ? current.filter((entry) => entry.id !== planId) : current,
          );
          queryClient.removeQueries({ queryKey: planKey(planId) });
          queryClient.removeQueries({ queryKey: shoppingKey(planId) });
          toast.info(`You left ${plan.name}`);
          sheet.close({ to: '/plans', replace: true });
        },
        onSettled: () => setConfirmingLeave(false),
      },
    );
  }

  return (
    <Sheet {...sheet.sheetProps} title="Edit plan">
      {isError && !plan ? (
        <QueryErrorState message="Couldn't load this plan." onRetry={() => void refetch()} />
      ) : !plan ? (
        <div className="edit-plan-sheet__loading" aria-busy="true" aria-label="Loading plan">
          <Spinner size="3" />
        </div>
      ) : confirmingDelete ? (
        <div className="edit-plan-sheet__confirm-delete">
          <Text as="p" size="2">
            Delete &ldquo;{plan.name}&rdquo;? This can&apos;t be undone.
          </Text>
          <div className="edit-plan-sheet__confirm-delete-actions">
            <Button
              variant="soft"
              size="3"
              onClick={() => setConfirmingDelete(false)}
              disabled={deletePlan.isPending}
            >
              Cancel
            </Button>
            <Button variant="solid" color="red" size="3" loading={deletePlan.isPending} onClick={handleConfirmDelete}>
              Yes, delete
            </Button>
          </div>
        </div>
      ) : (
        <div className="edit-plan-sheet__sections">
          {isOwner && (
            <>
              <form onSubmit={handleRename} className="edit-plan-sheet__section">
                <Text as="span" size="2" weight="medium">
                  Name
                </Text>
                <div className="edit-plan-sheet__rename-row">
                  <TextField.Root
                    value={name}
                    onChange={(event) => setNameOverride(event.target.value)}
                    size="3"
                    className="edit-plan-sheet__rename-input"
                    aria-label="Plan name"
                    autoCapitalize="sentences"
                    autoComplete="off"
                    enterKeyHint="done"
                  />
                  <Button type="submit" size="3" variant="solid" disabled={!dirty} loading={updatePlan.isPending}>
                    Save
                  </Button>
                </div>
              </form>

              <Separator size="4" />
            </>
          )}

          <div className="edit-plan-sheet__section">
            <Button
              variant="soft"
              size="3"
              onClick={handleDuplicate}
              loading={duplicatePlan.isPending}
              disabled={duplicatePlan.isPending}
            >
              <CopyIcon /> Duplicate
            </Button>
          </div>

          <div className="edit-plan-sheet__section">
            <Button variant="soft" size="3" onClick={handleCopySummary}>
              <Share2Icon /> Copy summary
            </Button>
          </div>

          <Separator size="4" />

          <div className="edit-plan-sheet__section">
            <Text as="span" size="2" weight="medium">
              Sharing
            </Text>
            <Button variant="soft" size="3" onClick={() => void sharePlan.share()} loading={sharePlan.isSharing}>
              <Link2Icon /> Share plan
            </Button>
            {sharePlan.fallbackUrl && (
              <TextField.Root
                value={sharePlan.fallbackUrl}
                readOnly
                size="2"
                aria-label="Share link"
                onFocus={(event) => event.currentTarget.select()}
              />
            )}
          </div>

          <div className="edit-plan-sheet__section">
            <Text as="span" size="2" weight="medium">
              Members
            </Text>
            {members.isLoading && <Skeleton height="44px" aria-hidden="true" />}
            {members.isError && !members.data && (
              <QueryErrorState message="Couldn't load members." onRetry={() => void members.refetch()} />
            )}
            {members.data && (
              <div className="edit-plan-sheet__members">
                {members.data.map((member) => {
                  const isSelf = member.userId === user?.id;
                  const removable = isOwner && member.role !== 'owner';
                  return (
                    <div key={member.userId} className="edit-plan-sheet__member-row">
                      <span className="edit-plan-sheet__member-name">
                        {member.username}
                        {isSelf ? ' (you)' : ''}
                      </span>
                      {member.role === 'owner' ? (
                        <Badge variant="soft">Owner</Badge>
                      ) : removable ? (
                        confirmingRemoveUserId === member.userId ? (
                          <div className="edit-plan-sheet__member-confirm">
                            <Button size="2" variant="soft" onClick={() => setConfirmingRemoveUserId(null)}>
                              Cancel
                            </Button>
                            <Button
                              size="2"
                              variant="solid"
                              color="red"
                              loading={removeMember.isPending}
                              onClick={() => handleRemoveMember(member.userId)}
                            >
                              Remove
                            </Button>
                          </div>
                        ) : (
                          <RowActionButton
                            quiet
                            color="red"
                            aria-label={`Remove ${member.username}`}
                            onClick={() => setConfirmingRemoveUserId(member.userId)}
                          >
                            <Cross2Icon />
                          </RowActionButton>
                        )
                      ) : null}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {!isOwner && (
            <div className="edit-plan-sheet__section">
              {confirmingLeave ? (
                <div className="edit-plan-sheet__confirm-delete">
                  <Text as="p" size="2">
                    Leave &ldquo;{plan.name}&rdquo;?
                  </Text>
                  <div className="edit-plan-sheet__confirm-delete-actions">
                    <Button
                      variant="soft"
                      size="3"
                      onClick={() => setConfirmingLeave(false)}
                      disabled={removeMember.isPending}
                    >
                      Cancel
                    </Button>
                    <Button variant="solid" color="red" size="3" loading={removeMember.isPending} onClick={handleLeave}>
                      Yes, leave
                    </Button>
                  </div>
                </div>
              ) : (
                <Button variant="soft" color="red" size="3" onClick={() => setConfirmingLeave(true)}>
                  <ExitIcon /> Leave plan
                </Button>
              )}
            </div>
          )}

          {isOwner && (
            <>
              <Separator size="4" />
              <div className="edit-plan-sheet__section">
                <Button variant="soft" color="red" size="3" onClick={() => setConfirmingDelete(true)}>
                  <TrashIcon /> Delete plan
                </Button>
              </div>
            </>
          )}
        </div>
      )}
    </Sheet>
  );
}
