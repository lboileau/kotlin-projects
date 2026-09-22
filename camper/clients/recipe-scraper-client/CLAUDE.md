# recipe-scraper-client

Turns a recipe web page — or up to three photos of a recipe — into a `ScrapedRecipe` (name, servings,
meal/theme, ingredient lines normalised against the app's ingredient catalogue) using the Claude API.
The service fetches the HTML; this client does everything after that. `scrape(ScrapeRecipeParam)` is
the page path, `scrapeImages(ScrapeRecipeImagesParam)` the photo path; both share the system prompt,
the catalogue block, the schema and all post-processing (`AnthropicRecipeScraperClient.complete`).

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

## Photos (`scrapeImages`)

The images go first in the user turn as `ImageBlockParam` / `Base64ImageSource` content blocks (the
documented placement), each preceded by a `Photo N:` text label when there are several (also per the
docs), then one text block: the same catalogue lines and a short instruction
(`ScrapePromptBuilder.buildForImages`) — no source URL, no content label. The caller (the service)
has already checked count (`RecipeImage.MAX_IMAGES` = 3), media type and size; the webapp sizes photos
down to 1568px on the long edge before upload, so a page photo is ~300–500 KB of JPEG. Images are
billed in 28×28-px patches (`⌈w/28⌉ × ⌈h/28⌉`): a 1176×1568 photo is **~2,350 input tokens**, under
Sonnet 5's 2576px / 4,784-token cap so nothing is resized server-side. Measured on a meal-kit card
with a 286-item catalogue: ~4.8k in (800 system + 1.6k catalogue + 2.35k photo) / ~1.1k out for 9
lines ≈ $0.02 on Sonnet 5; three photos ≈ 9.5k in ≈ $0.03–0.04. The catalogue, not the photo, is the
text cost that grows.

Two things the first real photo (a meal-kit card, title out of frame, "2 Person" / "4 Person" columns)
taught the prompt and the code: the system prompt tells the model to *write a descriptive name when no
title is visible* and to *use the largest serving column and set baseServings to it*; and
`ScrapedRecipeJson.toDomain` turns a blank name into `"Untitled recipe"` rather than failing —
Sonnet 5 returned `""` for that card, and the import must not depend on the prompt line being obeyed.
**Only an empty ingredient list means "no recipe here"**: the schema has no "nothing found" shape, so the
prompt asks for an empty recipe when the photos are unreadable and the client turns that into a failure
("Couldn't read a recipe from the photo…").

## Relay client (`RelayRecipeScraperClient`) — the real app flow, no API key

`RECIPE_SCRAPER_RELAY_DIR=<dir>` (checked before `ANTHROPIC_API_KEY`) makes every import write its prompt
to `<dir>/<timestamp>-<n>-{page,photos}/` — `system.txt`, `user.txt`, `schema.json`, `meta.json`, plus
`image-1.jpg …` or `page.html` — and then **wait up to 4 minutes** for `response.json` to appear there,
which it parses exactly as a model answer. Whoever is watching the folder (a person, or a coding agent
that can read images — a Sonnet subagent pointed at the folder is a fair stand-in for the real model)
plays the model, and the picker → upload → draft → review flow runs for real in the browser. The
folder is gitignored (`camper/.relay/`). This replaced "put a photo in a directory and run the harness"
as the way to test without credits:

```bash
RECIPE_SCRAPER_RELAY_DIR=$PWD/.relay ./run-dev.sh --keep-db
```

Model: `RECIPE_SCRAPER_MODEL` env var, default `claude-sonnet-5` with thinking disabled (a lookup-and-copy
task; thinking doubled the output bill for no accuracy gain). Measured on a 21-ingredient recipe with a
72-item catalogue: Sonnet 5 ≈ 3.6k in / 2.5k out (~$0.03), Haiku 4.5 ≈ 2.8k in / 1.6k out (~$0.01);
both got every quantity right, Sonnet was the more consistent judge on ambiguous matches. A photo costs
more input than 1 KB of JSON-LD (a 1568px image is on the order of 1.5k tokens) — still a few cents.
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

`-Pscrape.offline=prompt-images` does the same for the image files in `<dir>/images/` (one recipe, sorted by
name, at most three) into `<dir>/offline/images/`; `parse` then handles that folder like any other.
`meta.json` records whether the page yielded `JsonLd` or fell back to `VisibleText`, and how many chars.
`docs/recipe-scraper/import-quality-report.md` is the Sep 2026 run over all 72 prod recipes (68 JSON-LD, 2 text,
1 empty redirect stub, 1 bot-blocked 403) comparing prod's stored lines, the new path, and a hand review.

## Testing
- `RecipePageExtractorTest` — JSON-LD graph/array/top-level detection, noise-field dropping, text fallback, cap
- `ScrapedRecipeJsonTest` — ref resolution, every flag derivation, enum/servings validation, blank-name placeholder
- `ScrapePromptBuilderTest` — page vs photo user messages, the sentences the system prompt must keep
- `FakeRecipeScraperClient` (testFixtures) for consumers (records `lastParam` / `lastImagesParam`);
  `NoOpRecipeScraperClient` returns a canned "Classic Guacamole" for both paths when `ANTHROPIC_API_KEY` is unset
