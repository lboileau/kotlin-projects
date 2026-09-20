import { useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Button, Separator, Spinner, Text, TextField } from '@radix-ui/themes';
import { CopyIcon, Share2Icon, TrashIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useCloseSheet } from '../../components/useCloseSheet';
import { ApiError } from '../../api/http';
import { duplicatePlan, plansKey, usePlan, useDeletePlan, useUpdatePlan } from '../../queries/plans';
import { flattenMealPlan } from '../../lib/flatPlan';
import { buildMealPlanSummary } from '../../lib/mealPlanSummary';
import { clearSelectedPlanId, getSelectedPlanId } from '../../lib/selectedPlan';
import { toast } from '../../lib/toastStore';
import './EditPlanSheet.css';

export function EditPlanSheet() {
  const { planId } = useParams<{ planId: string }>();
  const closeSheet = useCloseSheet(`/plans/${planId}`);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: plan } = usePlan(planId);
  const updatePlan = useUpdatePlan(planId ?? '');
  const deletePlan = useDeletePlan();

  // No effect needed to seed this from `plan`: until the field is
  // touched this session, the displayed value just falls through to the
  // server's — once touched, the override takes over.
  const [nameOverride, setNameOverride] = useState<string | null>(null);
  const name = nameOverride ?? plan?.name ?? '';

  const [duplicating, setDuplicating] = useState(false);
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const dirty = name.trim().length > 0 && name.trim() !== plan?.name;

  function handleRename(event: FormEvent) {
    event.preventDefault();
    if (!dirty) return;
    updatePlan.mutate({ name: name.trim() });
  }

  async function handleDuplicate() {
    if (!plan) return;
    setDuplicating(true);
    const recipes = flattenMealPlan(plan);
    setProgress({ done: 0, total: recipes.length });
    try {
      const { newPlanId, failedRecipeNames } = await duplicatePlan(plan, recipes, (done, total) =>
        setProgress({ done, total }),
      );
      void queryClient.invalidateQueries({ queryKey: plansKey });
      if (failedRecipeNames.length > 0) {
        toast.error(`Copied the plan, but couldn't add: ${failedRecipeNames.join(', ')}`);
      }
      navigate(`/plans/${newPlanId}`);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Could not duplicate the plan.';
      toast.error(message);
    } finally {
      setDuplicating(false);
      setProgress(null);
    }
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
        navigate('/plans', { replace: true });
      },
    });
  }

  return (
    <Sheet title="Edit plan" onClose={closeSheet}>
      {!plan ? (
        <div className="edit-plan-sheet__loading">
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
            <Button variant="soft" size="3" onClick={handleDuplicate} loading={duplicating} disabled={duplicating}>
              <CopyIcon /> Duplicate
            </Button>
            {progress && (
              <Text size="1" color="gray">
                Adding {progress.done} of {progress.total}…
              </Text>
            )}
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
