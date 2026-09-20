import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Text, TextField } from '@radix-ui/themes';
import { Sheet } from '../../components/Sheet';
import { useCloseSheet } from '../../components/useCloseSheet';
import { Stepper } from '../../components/Stepper';
import { useCreatePlan } from '../../queries/plans';
import './NewPlanSheet.css';

export function NewPlanSheet() {
  const closeSheet = useCloseSheet('/plans');
  const navigate = useNavigate();
  const createPlan = useCreatePlan();

  const [name, setName] = useState('');
  const [servings, setServings] = useState(2);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (createPlan.isPending || !name.trim()) return;

    createPlan.mutate(
      { name: name.trim(), servings },
      {
        onSuccess: (plan) => navigate(`/plans/${plan.id}`, { replace: true }),
        // On error the global mutation error toast already surfaces it —
        // stay on the sheet so the user can retry.
      },
    );
  }

  return (
    <Sheet title="New plan" onClose={closeSheet}>
      <form onSubmit={handleSubmit} className="new-plan-sheet__form">
        <label className="new-plan-sheet__field">
          <Text as="span" size="2" weight="medium">
            Name
          </Text>
          <TextField.Root
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Weeknight dinners"
            size="3"
            autoFocus
            required
          />
        </label>

        <div className="new-plan-sheet__servings">
          <Text as="span" size="2" weight="medium">
            Servings
          </Text>
          <Stepper value={servings} onChange={setServings} min={1} ariaLabel="Servings" />
        </div>

        <Button
          type="submit"
          size="3"
          variant="solid"
          loading={createPlan.isPending}
          disabled={!name.trim()}
          className="new-plan-sheet__submit"
        >
          Create plan
        </Button>
      </form>
    </Sheet>
  );
}
