import { useState, type FormEvent } from 'react';
import { useLocation } from 'react-router-dom';
import { Button, Text, TextField } from '@radix-ui/themes';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { Stepper } from '../../components/Stepper';
import { useCreatePlan } from '../../queries/plans';
import { DEFAULT_PLAN_SERVINGS, todaysDateLabel } from '../../lib/planDefaults';
import './NewPlanSheet.css';

export function NewPlanSheet() {
  // Mounted under /plans (first plan) and under both one-plan screens, from
  // the header's plan dropdown (router.tsx): the parent is the page under it.
  const sheet = useSheet(useLocation().pathname.replace(/\/new\/?$/, ''));
  const createPlan = useCreatePlan();

  const [name, setName] = useState('');
  const [servings, setServings] = useState(DEFAULT_PLAN_SERVINGS);
  const defaultName = todaysDateLabel();

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (createPlan.isPending) return;

    // Shown as a placeholder, not a pre-filled value, so typing a name
    // never has to clear/replace anything — but an untouched, empty
    // field still creates successfully, using that same date as the name.
    createPlan.mutate(
      { name: name.trim() || defaultName, servings },
      {
        onSuccess: (plan) => sheet.close({ to: `/plans/${plan.id}`, replace: true }),
        // On error the global mutation error toast already surfaces it —
        // stay on the sheet so the user can retry.
      },
    );
  }

  return (
    <Sheet {...sheet.sheetProps} title="New plan">
      <form onSubmit={handleSubmit} className="new-plan-sheet__form">
        <label className="new-plan-sheet__field">
          <Text as="span" size="2" weight="medium">
            Name
          </Text>
          <TextField.Root
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder={defaultName}
            size="3"
            autoFocus
          />
        </label>

        <div className="new-plan-sheet__servings">
          <Text as="span" size="2" weight="medium">
            Servings per recipe
          </Text>
          <Stepper value={servings} onChange={setServings} min={1} ariaLabel="Servings per recipe" />
        </div>

        <Button
          type="submit"
          size="3"
          variant="solid"
          loading={createPlan.isPending}
          className="new-plan-sheet__submit"
        >
          Create plan
        </Button>
      </form>
    </Sheet>
  );
}
