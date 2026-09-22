# Instructions backfill — 2026-09-22 (local DB)

Recipe steps were added with the instructions feature (`docs/recipe-instructions-photos/plan.md`);
existing recipes had none. This run filled them for every local recipe with a source URL, **without an
API call**: the offline harness (`ScrapeOfflineHarnessTest`, `prompt` mode) fetched and extracted each
page, Claude Fable 5.1 (the session running Claude Code) answered each prompt as the model would, the
harness's `parse` mode ran the real post-processing, and the resulting steps were written through
`PUT /api/recipes/{id}/steps`. Ingredient lines were not touched.

| | count |
|---|---|
| Recipes with a source URL | 72 |
| Filled | **66** |
| Blocked — HTTP 403 (bot-blocked, same as the Sep import report) | 5: simplyrecipes ×2, maangchi, damndelicious, marionskitchen |
| Empty page (154-byte redirect stub) | 1: moanaskitchen |

Of the 66: **64 had JSON-LD `recipeInstructions`** (every one — the extractor's flattening covered
HowToSection / HowToStep / string shapes) and the model answer is the flattened list as written, leading
numbering stripped. Two needed a judgement call: *Oven-Roasted Chicken Shawarma* had the whole method as
one string and was split at its natural step boundaries (6 steps); *Roasted Root Vegetables* had a
"Storage: …" note as its last step, which was dropped. **2 were visible-text pages** with no JSON-LD
(*Chicken Paprikash with Spaetzle*, *Jerk Chicken with Rice and Peas*) whose method was read from the
page text and written as steps, section headings skipped.

Steps per recipe: min 2, median 5, max 21 (Chicken Biryani).

A Sonnet 5 subagent answered three of these prompts independently beforehand (recipetineats,
spendwithpennies, cookieandkate): its steps were word-for-word identical to Fable's on all 28, and all
42 ingredient lines matched too. Adding instructions costs roughly +230–720 input and +230–720 output
tokens per URL import (~4 chars/token; output because steps are echoed back), ≈ $0.035–0.04 on Sonnet 5.

The six unfilled recipes can be given steps by hand on the edit page, or by the photo import if a printed
copy is to hand.

## Prod run (same day, after deploying PR #324 + #325)

75 prod recipes had a source URL. 66 answers were reused from the local run above (same URLs); the
harness ran on the 9 others — 4 usable (marionskitchen let us in this time), 5 the same blocked/dead
pages. `run-2026-09-22-steps/apply-steps.py` then applied `steps-plan.json` to prod (owner-run):
**69 applied, 1 skipped** (the tandoori chicken had just been imported live, steps included),
**0 failed**. The script GETs each recipe first and only writes steps to one that has none, so it is
safe to re-run; `--clear` is the rollback, `--dry-run` shows the plan.

Still without steps on prod: Chewy No Sugar Greek Yogurt Brownies (dead page), Fish Stew with Ginger
and Tomatoes, Shrimp Cakes (simplyrecipes 403), Japchae (maangchi 403), Korean Beef Bowl
(damndelicious 403).

Real-key cost seen in the same session: a URL import with steps was 6,820 in / 3,028 out tokens
(≈ $0.044 on Sonnet 5); a two-photo import 10,275 in / 3,092 out (≈ $0.052).
