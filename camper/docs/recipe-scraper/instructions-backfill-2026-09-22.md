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
