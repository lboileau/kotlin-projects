import { describe, expect, it } from 'vitest';
import { normaliseStep, normaliseSteps, parseRecipeTab, splitPastedSteps, stepsChanged } from './recipeSteps';

describe('parseRecipeTab', () => {
  it('reads the three tabs and falls back to ingredients', () => {
    expect(parseRecipeTab('instructions')).toBe('instructions');
    expect(parseRecipeTab('photos')).toBe('photos');
    expect(parseRecipeTab('ingredients')).toBe('ingredients');
    expect(parseRecipeTab(null)).toBe('ingredients');
    expect(parseRecipeTab('nope')).toBe('ingredients');
  });
});

describe('normaliseStep', () => {
  it('strips leading numbering and step labels', () => {
    expect(normaliseStep('1. Chop the onion.')).toBe('Chop the onion.');
    expect(normaliseStep('2) Fry it')).toBe('Fry it');
    expect(normaliseStep('Step 3: Simmer.')).toBe('Simmer.');
    expect(normaliseStep('STEP TWO - Serve')).toBe('Serve');
    expect(normaliseStep('10 – Rest the meat')).toBe('Rest the meat');
  });

  it('leaves a quantity at the start of a sentence alone', () => {
    expect(normaliseStep('2 cups of water go in first.')).toBe('2 cups of water go in first.');
    expect(normaliseStep('350°F for 20 minutes')).toBe('350°F for 20 minutes');
  });

  it('trims', () => {
    expect(normaliseStep('  Serve.  ')).toBe('Serve.');
  });
});

describe('normaliseSteps', () => {
  it('drops blanks and keeps order', () => {
    expect(normaliseSteps(['1. Chop', '', '   ', '2. Cook'])).toEqual(['Chop', 'Cook']);
  });
});

describe('stepsChanged', () => {
  it('is false for the same list, even with cosmetic differences', () => {
    expect(stepsChanged(['Chop', 'Cook'], [' 1. Chop', 'Cook '])).toBe(false);
    expect(stepsChanged([], [''])).toBe(false);
  });

  it('is true when a step is added, removed, edited or reordered', () => {
    expect(stepsChanged(['Chop'], ['Chop', 'Cook'])).toBe(true);
    expect(stepsChanged(['Chop', 'Cook'], ['Chop'])).toBe(true);
    expect(stepsChanged(['Chop'], ['Chop finely'])).toBe(true);
    expect(stepsChanged(['Chop', 'Cook'], ['Cook', 'Chop'])).toBe(true);
  });
});

describe('splitPastedSteps', () => {
  it('splits one step per line', () => {
    expect(splitPastedSteps('1. Chop\n2. Cook\n\n')).toEqual(['Chop', 'Cook']);
  });

  it('splits by paragraph when the text has blank lines, joining wrapped lines', () => {
    expect(splitPastedSteps('Chop the onion\nfinely.\n\nCook it\nslowly.')).toEqual(['Chop the onion finely.', 'Cook it slowly.']);
  });

  it('returns a single step for a single line', () => {
    expect(splitPastedSteps('Mix everything.')).toEqual(['Mix everything.']);
  });
});
