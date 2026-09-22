# recipe-scraper-client

Turns a recipe web page into a `ScrapedRecipe` (name, servings, meal/theme, ingredient lines normalised
against the app's ingredient catalogue) using the Claude API. The service fetches the HTML; this client
does everything after that.

## Package
`com.acme.clients.recipescraperclient`

## How a scrape works (`internal/AnthropicRecipeScraperClient`)

1. **`RecipePageExtractor`** reduces the page to what describes the recipe. It prefers the
   schema.org `Recipe` node from the page's JSON-LD (`name`, `recipeYield`, `recipeIngredient`, …
   — ~1 KB, authoritative). Only when no JSON-LD recipe exists does it fall back to the page's visible
   text with scripts/styles/nav/header/footer stripped, capped at 60k chars.
   *Never send a raw prefix of the HTML*: recipe pages are 300–600 KB of ads and CSS and the ingredient
   list is routinely past the first 100 KB, which made the model reconstruct recipes from the
   instructions — wrong ingredients and wrong quantities.
2. The catalogue is sent as numbered lines (`ref: name`). The model returns `matchedIngredientRef`
   (an index), never a UUID — transcribed UUIDs were an import-failing error source.
3. The response is constrained with **structured outputs** (`OutputConfig.format` with the schema in
   `scrapedRecipeSchema()`), so units/categories/enums are guaranteed and no fence-stripping is needed.
4. **`ScrapedRecipeJson.toDomain(catalogue)`** resolves refs to ids and derives review flags
   deterministically. Lines the model labels `kind: HEADING` / `NOTE` (section labels, tips, "and more!"
   lists that sites put inside `recipeIngredient`) and blank lines are dropped here. The model only
   reports what it read; Kotlin decides what needs review:
   - `NEW_INGREDIENT` — no ref (or an out-of-range ref)
   - `INGREDIENT_MATCH_UNCERTAIN` — ref present, `matchConfidence` not `HIGH`
   - `UNIT_CONVERSION_NEEDED` — line unit ≠ matched ingredient's `defaultUnit` (computed here, not by the model)
   - `QUANTITY_ASSUMED` — source had no quantity ("to taste", "for garnish") or the value had to be corrected
   Any flag → the service stores the line as `pending_review`.
5. **`ScrapedIngredientMerger`** collapses the per-section repeats recipe pages produce ("For the chicken…
   For the rice…" each listing olive oil) into one line per ingredient: same unit summed; compatible units
   (volume↔volume, weight↔weight, via `meal-plan-calculator`'s `UnitConverter`) summed in the smallest unit
   and shown in the largest if that reads cleanly; count units only when identical. Every original line's
   text is kept, joined with "; ". `docs/recipe-scraper/run-2026-09-21/merge-duplicate-lines.py` applies the
   same rule to already-stored recipes through the API.

Model: `RECIPE_SCRAPER_MODEL` env var, default `claude-sonnet-5` with thinking disabled (a lookup-and-copy
task; thinking doubled the output bill for no accuracy gain). Measured on a 21-ingredient recipe with a
72-item catalogue: Sonnet 5 ≈ 3.6k in / 2.5k out (~$0.03), Haiku 4.5 ≈ 2.8k in / 1.6k out (~$0.01);
both got every quantity right, Sonnet was the more consistent judge on ambiguous matches.
Token usage is logged per scrape at INFO.

## Manual harness (`ScrapeHarnessTest`)

Skipped unless Gradle properties are passed; runs the real client against a saved page:

```bash
./gradlew :clients:recipe-scraper-client:test --tests "*ScrapeHarnessTest*" \
  -Pscrape.dir=/path/to/dir -Pscrape.input=raw -Pscrape.apiKey=$ANTHROPIC_API_KEY [-Pscrape.model=claude-haiku-4-5]
```

`dir` needs `page.html` (raw HTML, e.g. `curl -sL -A "Mozilla/5.0 (compatible; CamperBot/1.0)" <url> -o page.html`),
`ingredients.json` (`[{id,name,category,defaultUnit}]`, e.g. `select json_agg(json_build_object('id', id, 'name', name,
'category', category, 'defaultUnit', default_unit)) from ingredients`), and — for `-Pscrape.input=jsonld` — a
pre-extracted `recipe-jsonld.json`. Output goes to `dir/result-<input>-<model>.txt`.

## Offline harness (`ScrapeOfflineHarnessTest`) — no API credits

Runs everything except the model call, so prompts can be answered by a local agent (or a person) and fed back:

```bash
# 1. fetch + extract each URL in <dir>/urls.txt → <dir>/offline/<n>/{system.txt,user.txt,schema.json,meta.json}
./gradlew :clients:recipe-scraper-client:test --tests "*ScrapeOfflineHarnessTest*" -Pscrape.dir=<dir> -Pscrape.offline=prompt
# 2. answer each prompt however you like, writing <dir>/offline/<n>/response.json
# 3. run the real post-processing against <dir>/ingredients.json → <dir>/offline/<n>/result.txt
./gradlew :clients:recipe-scraper-client:test --tests "*ScrapeOfflineHarnessTest*" -Pscrape.dir=<dir> -Pscrape.offline=parse
```

`meta.json` records whether the page yielded `JsonLd` or fell back to `VisibleText`, and how many chars.
`docs/recipe-scraper/import-quality-report.md` is the Sep 2026 run over all 72 prod recipes (68 JSON-LD, 2 text,
1 empty redirect stub, 1 bot-blocked 403) comparing prod's stored lines, the new path, and a hand review.

## Testing
- `RecipePageExtractorTest` — JSON-LD graph/array/top-level detection, noise-field dropping, text fallback, cap
- `ScrapedRecipeJsonTest` — ref resolution, every flag derivation, enum/servings validation
- `FakeRecipeScraperClient` (testFixtures) for consumers; `NoOpRecipeScraperClient` returns a canned
  "Classic Guacamole" when `ANTHROPIC_API_KEY` is unset
