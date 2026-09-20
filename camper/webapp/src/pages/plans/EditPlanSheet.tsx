import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Separator, Spinner, Text, TextField } from '@radix-ui/themes';
import { CopyIcon, Share2Icon, TrashIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { QueryErrorState } from '../../components/QueryErrorState';
import { usePlan, useDeletePlan, useDuplicatePlan, useUpdatePlan } from '../../queries/plans';
import { buildMealPlanSummary } from '../../lib/mealPlanSummary';
import { clearSelectedPlanId, getSelectedPlanId } from '../../lib/selectedPlan';
import { toast } from '../../lib/toastStore';
import './EditPlanSheet.css';

export function EditPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  const sheet = useSheet(`/plans/${planId}`);

  const { data: plan, isError, refetch } = usePlan(planId);
  const updatePlan = useUpdatePlan(planId ?? '');
  const deletePlan = useDeletePlan();
  const duplicatePlan = useDuplicatePlan();

  // No effect needed to seed this from `plan`: until the field is
  // touched this session, the displayed value just falls through to the
  // server's — once touched, the override takes over.
  const [nameOverride, setNameOverride] = useState<string | null>(null);
  const name = nameOverride ?? plan?.name ?? '';

  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const dirty = name.trim().length > 0 && name.trim() !== plan?.name;

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
      { onSuccess: (newPlan) => sheet.close({ to: `/plans/${newPlan.id}` }) },
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
              />
              <Button type="submit" size="3" variant="solid" disabled={!dirty} loading={updatePlan.isPending}>
                Save
              </Button>
            </div>
          </form>

          <Separator size="4" />

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
            <Button variant="soft" color="red" size="3" onClick={() => setConfirmingDelete(true)}>
              <TrashIcon /> Delete plan
            </Button>
          </div>
        </div>
      )}
    </Sheet>
  );
}
