# Recipe import quality: prod (old path) vs new path

72 recipes with a source URL on canoecamp.life, 68 extracted via JSON-LD, 2 via text fallback, 2 unscrapable.

**Prod column** = what is stored on canoecamp.life today (old 100 KB raw-HTML path), scored against the source page's own ingredient list. "FABRICATED" = not one stored line exists in the source.

**Sonnet column** = the new pipeline (JSON-LD/text extraction → exact prod prompt + schema → Sonnet, simulated locally → Kotlin post-processing), as lines-produced/lines-in-source.

**My review** = line-by-line check of the Sonnet output against the source: every quantity, unit and catalogue match.

| # | Recipe | Site | Extract | Src lines | Prod lines | Prod vs source | Sonnet vs source | My review |
|---|---|---|---|---|---|---|---|---|
| 00 | 20 Minute Ground Turkey Bang Bang Rice B | lemonsandzest.com | JSON-LD | 19 | 14 | partial: 12 invented, 17 missing | 19/19 (1 non-ingredient) | trailing "…and more!" toppings line emitted as NEW (now dropped via kind=NOTE) |
| 01 | Asian Slaw with Sesame Ginger Dressing | jessicagavin.com | JSON-LD | 16 | 16 | partial: 1 qty | 16/16 | "¼ sliced green onions" has no unit in source; cup assumed — reasonable |
| 02 | Asparagus, Corn, Tomato & Feta Salad | blythesblog.com | JSON-LD | 11 | 10 | partial: 1 invented, 3 missing | 11/11 | all lines, quantities and matches correct |
| 03 | Authentic German Red Cabbage (Rotkohl) | dirndlkitchen.com | JSON-LD | 13 | 16 | FABRICATED | 13/13 | all lines, quantities and matches correct |
| 04 | Baby Meatballs for Baby Led Weaning (Hig | abbeyskitchen.com | JSON-LD | 8 | 6 | partial: 2 missing | 8/8 | all lines, quantities and matches correct |
| 05 | Baked Chicken & Veggie Meatballs for Bab | babyfoode.com | JSON-LD | 10 | 10 | ok | 10/10 | all lines, quantities and matches correct |
| 06 | Baked Pork Tenderloin with Apples | inspiredtaste.net | JSON-LD | 10 | 9 | partial: 1 invented, 2 missing, 1 qty | 10/10 | all lines, quantities and matches correct |
| 07 | BBQ Meatloaf Muffins {Beef and Quinoa} | hauteandhealthyliving.com | JSON-LD | 11 | 11 | ok | 11/11 | all lines, quantities and matches correct |
| 08 | Beef and Zucchini "Lasagna" | hellofresh.ca | JSON-LD | 10 | 8 | FABRICATED | 10/10 | all lines, quantities and matches correct |
| 09 | Beef Stew | spendwithpennies.com | JSON-LD | 17 | 14 | FABRICATED | 17/17 | all lines, quantities and matches correct |
| 10 | Best Easy Healthy Baked Salmon | lecremedelacrumb.com | JSON-LD | 7 | 7 | ok | 7/7 | all lines, quantities and matches correct |
| 11 | Best Fluffy Pancakes | cafedelites.com | JSON-LD | 9 | 10 | partial: 1 invented, 1 qty | 9/9 | all lines, quantities and matches correct |
| 12 | Best Pulled Pork | delish.com | JSON-LD | 16 | 16 | partial: 4 qty | 16/16 | all lines, quantities and matches correct |
| 13 | Best Ratatouille Recipe | cookieandkate.com | JSON-LD | 13 | 15 | partial: 14 invented, 12 missing | 13/13 | all lines, quantities and matches correct |
| 14 | Black Bean and Corn Salad | loveandlemons.com | JSON-LD | 15 | 14 | partial: 1 missing, 1 qty | 15/15 | all lines, quantities and matches correct |
| 15 | Broccoli Salad | tastesbetterfromscratch.com | JSON-LD | 8 | 8 | partial: 1 qty | 8/8 | all lines, quantities and matches correct |
| 16 | Cheesy Baked Broccoli Bites | hungryhealthyhappy.com | JSON-LD | 9 | 9 | partial: 1 invented, 1 missing | 9/9 | all lines, quantities and matches correct |
| 17 | Chewy No Sugar Greek Yogurt Brownies | moanaskitchen.com | EMPTY | 0 | 10 | n/a | — | page is a JS redirect stub with no body; new client now refuses instead of guessing |
| 18 | Chicken Biryani | indianhealthyrecipes.com | JSON-LD | 27 | 26 | partial: 25 invented, 26 missing | 27/27 | all lines, quantities and matches correct |
| 19 | Chicken Breast in Creamy Mushroom Sauce | recipetineats.com | JSON-LD | 13 | 13 | partial: 5 invented, 5 missing, 3 qty | 13/13 | all lines, quantities and matches correct |
| 20 | Chicken Noodle Soup | budgetbytes.com | JSON-LD | 14 | 14 | partial: 2 qty | 14/14 | all lines, quantities and matches correct |
| 21 | Chicken Paprikash with Spaetzle | pathculture.com | text | 0 | 17 | n/a | 17 lines (text) | no JSON-LD; found all 17 lines in prose |
| 22 | Creamy Sweet Potato Coconut Soup | natashaskitchen.com | JSON-LD | 11 | 15 | FABRICATED | 11/11 | all lines, quantities and matches correct |
| 23 | Cucumber Edamame Salad with Sesame-Ginge | dishingouthealth.com | JSON-LD | 13 | 13 | partial: 5 invented, 5 missing, 1 qty | 13/13 | all lines, quantities and matches correct |
| 24 | Cá Kho Tộ: Vietnamese Caramelized Braise | panningtheglobe.com | JSON-LD | 9 | 13 | FABRICATED | 9/9 | all lines, quantities and matches correct |
| 25 | Easy Chicken Piccata | saltandlavender.com | JSON-LD | 11 | 11 | ok | 11/11 | all lines, quantities and matches correct |
| 26 | Easy Maple Roasted Carrots | vancouverwithlove.com | JSON-LD | 7 | 6 | partial: 1 invented, 2 missing | 7/7 | all lines, quantities and matches correct |
| 27 | Easy Vegetable Pasta Bake | inspiredtaste.net | JSON-LD | 11 | 11 | ok | 11/11 | all lines, quantities and matches correct |
| 28 | Filipino Chicken Adobo (Flavour Kapow!) | recipetineats.com | JSON-LD | 12 | 11 | partial: 5 invented, 6 missing, 1 qty | 12/12 | "1/3 cup + 2 tbsp" → 0.33 cup, should be ~0.46 (prompt now says to sum) |
| 29 | Fish Stew with Ginger and Tomatoes | simplyrecipes.com | JSON-LD | 12 | 12 | FABRICATED | 12/12 | all lines, quantities and matches correct |
| 30 | French Lentil & Beet Salad | kaynutrition.com | JSON-LD | 14 | 10 | partial: 9 invented, 13 missing | 14/14 | all lines, quantities and matches correct |
| 31 | Fried Rice | gimmesomeoven.com | JSON-LD | 12 | 12 | partial: 2 qty | 12/12 | all lines, quantities and matches correct |
| 32 | Frikadellen (German Hamburgers) | dirndlkitchen.com | JSON-LD | 12 | 13 | FABRICATED | 12/12 | all lines, quantities and matches correct |
| 33 | Garlic Herb Roasted Potatoes Carrots and | eatwell101.com | JSON-LD | 8 | 7 | partial: 1 missing | 8/8 | all lines, quantities and matches correct |
| 34 | Gluten-free Lemon Poppy Seed Loaf | tasty.co | JSON-LD | 9 | 9 | ok | 9/9 | all lines, quantities and matches correct |
| 35 | Gordon Ramsay Fennel Salad Recipe | cheframsayrecipes.com | JSON-LD | 11 | 12 | partial: 11 invented, 10 missing | 11/11 | all lines, quantities and matches correct |
| 36 | Greek Chicken and Lemon Rice (30 Minutes | juliasalbum.com | JSON-LD | 21 | 17 | FABRICATED | 21/21 | all lines, quantities and matches correct |
| 37 | Greek Chicken Burgers | downshiftology.com | JSON-LD | 16 | 16 | partial: 1 qty | 16/16 | all lines, quantities and matches correct |
| 38 | Greek Chicken Souvlaki Bowls with Roaste | littlespicejar.com | JSON-LD | 15 | 20 | partial: 9 invented, 4 missing | 15/15 | all lines, quantities and matches correct |
| 39 | Greek Salad | loveandlemons.com | JSON-LD | 14 | 13 | partial: 1 missing, 1 qty | 14/14 | all lines, quantities and matches correct |
| 40 | Greek turkey meatballs with lemon herb r | crisptastes.com | JSON-LD | 16 | 16 | ok | 16/16 | all lines, quantities and matches correct |
| 41 | Japchae glass noodles stir fried | maangchi.com | JSON-LD | 10 | 14 | FABRICATED | 10/10 | all lines, quantities and matches correct |
| 42 | Jerk Chicken with Rice and Peas | whatdadcooked.com | text | 0 | 19 | n/a | 19 lines (text) | no JSON-LD; found all 19 lines in a noisy blog page |
| 43 | Korean Beef Bowl | damndelicious.net | HTTP 403 | 0 | 10 | n/a | — | site returns 403 to the CamperBot user-agent — nothing to scrape |
| 44 | Korean Beef Bulgogi Rice Bowls - the eas | recipetineats.com | JSON-LD | 23 | 23 | partial: 1 missing, 2 qty | 23/23 | all lines, quantities and matches correct |
| 45 | Lamb Koftas with Yoghurt Dressing | recipetineats.com | JSON-LD | 22 | 20 | partial: 2 invented, 4 missing, 2 qty | 22/22 (1 non-ingredient) | "1/2 black pepper" no unit in source; tsp assumed — reasonable; two "OR…" serving lines emitted as NEW (now kind=NOTE) |
| 46 | Mason Jar Cobb Salad Recipe | joyfulhealthyeats.com | JSON-LD | 14 | 14 | ok | 14/14 | all lines, quantities and matches correct |
| 47 | Mayo Parmesan Chicken Bake | lilluna.com | JSON-LD | 6 | 7 | FABRICATED | 6/6 | all lines, quantities and matches correct |
| 48 | Old-Fashioned Rice Pudding | spicysouthernkitchen.com | JSON-LD | 6 | 6 | partial: 1 invented, 1 missing | 6/6 | all lines, quantities and matches correct |
| 49 | One Skillet Salmon with Lemon Orzo | servingdumplings.com | JSON-LD | 17 | 17 | partial: 1 qty | 17/17 | all lines, quantities and matches correct |
| 50 | One-Pot Chicken with Caramelized Lemon a | alisoneroman.com | JSON-LD | 8 | 9 | partial: 1 invented, 1 missing, 3 qty | 8/8 | all lines, quantities and matches correct |
| 51 | Oven-Roasted Chicken Shawarma (NYTimes) | recipezazz.com | JSON-LD | 15 | 14 | partial: 1 missing | 15/15 (1 non-ingredient) | "*FOR MARINADE*" heading emitted as NEW (now kind=HEADING) |
| 52 | Peruvian Chicken with Green Sauce (Aji V | cookingclassy.com | JSON-LD | 23 | 22 | partial: 1 missing, 2 qty | 23/23 | all lines, quantities and matches correct |
| 53 | Radicchio Arugula Apple Salad with Parme | nataliecooks.com | JSON-LD | 14 | 8 | FABRICATED | 14/14 (3 non-ingredient) | 3 "Pro tip"/substitution lines emitted as NEW (now kind=NOTE) |
| 54 | Roasted Brussels Sprouts | loveandlemons.com | JSON-LD | 8 | 7 | partial: 1 missing | 8/8 | all lines, quantities and matches correct |
| 55 | Roasted Cauliflower with Tahini and Fres | abeautifulplate.com | JSON-LD | 12 | 11 | partial: 1 invented, 2 missing, 2 qty | 12/12 | all lines, quantities and matches correct |
| 56 | Roasted Root Vegetables | thekitchn.com | JSON-LD | 6 | 9 | partial: 4 invented, 1 missing | 6/6 | all lines, quantities and matches correct |
| 57 | Shrimp Cakes | simplyrecipes.com | JSON-LD | 14 | 11 | FABRICATED | 14/14 | all lines, quantities and matches correct |
| 58 | Sour Cream and Onion Chicken | saltandlavender.com | JSON-LD | 11 | 11 | ok | 11/11 | all lines, quantities and matches correct |
| 59 | Spicy Turkey Sausage & Kale Chili | delish.com | JSON-LD | 15 | 15 | partial: 1 qty | 15/15 | all lines, quantities and matches correct |
| 60 | Sun-dried Tomato & Artichoke Pasta | shortgirltallorder.com | JSON-LD | 13 | 11 | FABRICATED | 13/13 | all lines, quantities and matches correct |
| 61 | Sweet Potato Black Bean Salad | theroastedroot.net | JSON-LD | 15 | 14 | FABRICATED | 15/15 | all lines, quantities and matches correct |
| 62 | Tabouli Salad Recipe | themediterraneandish.com | JSON-LD | 10 | 10 | partial: 3 qty | 10/10 | all lines, quantities and matches correct |
| 63 | Thai Basil Beef (Pad Gra Prow) | thewoksoflife.com | JSON-LD | 12 | 13 | partial: 2 invented, 1 missing | 12/12 | all lines, quantities and matches correct |
| 64 | Thai Glass Noodle Salad (Yum Woon Sen) | hot-thai-kitchen.com | JSON-LD | 15 | 15 | partial: 5 invented, 5 missing, 1 qty | 15/15 | all lines, quantities and matches correct |
| 65 | The Best Chili Recipe | spendwithpennies.com | JSON-LD | 15 | 15 | partial: 5 invented, 5 missing | 15/15 | all lines, quantities and matches correct |
| 66 | The Best Classic Shepherd's Pie | thewholesomedish.com | JSON-LD | 22 | 22 | partial: 1 invented, 2 missing, 2 qty | 22/22 | all lines, quantities and matches correct |
| 67 | Turkish Red Cabbage Salad | littlespicejar.com | JSON-LD | 11 | 11 | partial: 1 invented, 1 missing, 2 qty | 11/11 | all lines, quantities and matches correct |
| 68 | Tuscan Artichoke Tomato Salad | shortgirltallorder.com | JSON-LD | 13 | 11 | FABRICATED | 13/13 | all lines, quantities and matches correct |
| 69 | Vietnamese Lemongrass Chicken Rice Bowl | marionskitchen.com | JSON-LD | 17 | 17 | FABRICATED | 17/17 (2 non-ingredient) | blank line + "Nuoc cham sauce:" heading emitted as NEW (now dropped) |
| 70 | Vinegar Coleslaw Recipe (No Mayo Colesla | downshiftology.com | JSON-LD | 10 | 10 | ok | 10/10 | all lines, quantities and matches correct |
| 71 | White Bean and Kale Soup | primaverakitchen.com | JSON-LD | 12 | 12 | ok | 12/12 | all lines, quantities and matches correct |

## Totals (JSON-LD recipes, 68)

| | source lines | lines matching source | invented | missing | qty/unit wrong (parser) | qty/unit wrong (hand-checked) |
|---|---|---|---|---|---|---|
| prod (old path) | 879 | 538 | 324 | 345 | 41 | not reviewed |
| Sonnet (new path) | 879 | 879 | 0 | 0 (the 8 parser "missing" are duplicate lines it did emit) | 38 (all parser false positives) | 1 (#28) |

Prod verdicts: {'prod:partial': 41, 'prod:FABRICATED': 16, 'prod:ok': 11, 'prod:n/a': 4}

## Content-level verdict on prod (hand-reviewed, ingredient identity + quantity)

The line-text scoring above over- and under-states in places (Fish Stew has no stored originalText but correct content; Pancakes matches 9/9 lines by text yet has an invented cup of maple syrup). Reading the stored ingredients against the source recipe for all 68 JSON-LD recipes:

- **Materially wrong shopping list — re-import: 22 recipes**

  - #00: 8 missing (honey, ginger, sesame oil, broth, cornstarch, smoked paprika, salt, sriracha); soy sauce 1 tbsp vs 3; invented lime, olive oil
  - #03: invented ground clove/nutmeg/allspice/cinnamon stick + ½ cup red wine (source: 4 whole cloves, 2 tbsp red wine vinegar); apples 2 vs 3; jam 1 vs 2 tbsp; missing cornstarch, water
  - #08: every quantity ≈2× the source (zucchini 800 g vs 400, beef 500 g vs 250, tomatoes 750 ml vs 370, mozzarella 1.5 cup vs ¾)
  - #09: flour ⅓ cup vs 3 tbsp; broth 3 cups vs 6; tomato paste 1 vs 3 tbsp; invented garlic, Worcestershire, thyme, bay leaf; missing red wine, peas, rosemary, cornstarch, salt, pepper
  - #11: invented 1 cup maple syrup; butter ½ cup vs ¼
  - #13: zucchini 2 vs 1; bell pepper 2 vs 1; invented thyme, red wine vinegar, parsley; missing yellow squash, red pepper flakes
  - #18: rice 300 g vs 2 cups; yogurt ¾ cup vs 3 tbsp + ¼ cup; ghee 1 vs 2 tbsp; green chili 3 vs 1; invented tomato, cilantro, olive oil; missing cloves, cinnamon, garam masala, water, salt, fried onions, chilli powder
  - #22: 7 invented (cumin, cinnamon, lime, cilantro, pumpkin seeds, ginger, olive oil); missing bacon, celery, thyme, parsley; broth 2 cups vs 4 — effectively a different soup
  - #24: fish sauce 3 tbsp vs ¼ cup; sugar 3 tbsp vs ¼ cup; shallots 2 vs ¾ cup; invented garlic, canola oil, soy sauce, scallions, thai chili
  - #30: lentils 1 cup vs ½; beets 3 vs 4; goat cheese → feta; invented arugula, walnuts; missing water, bay leaf, red onion, parsley, chives, pepper
  - #32: 1 lb beef + 1 lb pork vs 500 g mixed total; eggs 2 vs 1; invented breadcrumbs, milk, pepper, oil; missing hard roll, marjoram, chili powder
  - #35: invented parmesan, pine nuts, parsley; missing blood oranges, radicchio, white balsamic, dill; fennel 2 vs 1; oil 2 vs 4 tbsp — a different salad
  - #36: invented onion, 1.5 cup chicken broth, whole lemon; missing fresh oregano ×2, second olive oil ×2, second lemon juice, spinach kept
  - #41: invented beef, garlic, sugar, soy sauce, sesame seeds, egg; missing onion, oil, salt, pepper
  - #47: parmesan 1 cup vs ⅓; garlic powder 1 tsp vs ½; lemon pepper → black pepper; invented parsley
  - #52: missing 2 cups cilantro (it is an aji verde), lime zest, cotija, second lime juice; lime 1 tsp vs 3 tbsp
  - #53: olive oil 3 tbsp vs ½ cup; dijon 1 tsp vs 2 tbsp; parmesan ½ cup vs 1; arugula 2 vs 3 cups; invented lemon, honey; missing walnuts, balsamic, cider vinegar, salt
  - #57: missing ⅔ cup cilantro, 4 garlic cloves, sugar, red onion; jalapeños 2 tsp vs 2 tbsp
  - #60: missing shallot, green olives, capers, chili flakes, parsley; invented oregano, basil
  - #61: mayonnaise 1 cup vs 2 tbsp; sweet potato 2 vs 1 large; missing salt, ½ cup red onion
  - #68: missing chickpeas (15 oz can), chives, capers, parsley; invented feta, oregano; olive oil 3 tbsp vs ¼ cup; tomatoes 1 cup vs 10 oz
  - #69: invented turmeric, vegetable oil, scallions, thai chili ×2, soy sauce, sesame oil; fish sauce 2 tsp vs 2 tbsp; garlic 3 vs 1 clove

- **A line or two wrong — fix in place: 6 recipes**

  - #28: missing 1½ cups water, second garlic, green onion garnish
  - #31: rice 2 cups vs 4
  - #44: missing sesame oil, second soy sauce, spinach, green onion; soy 2 vs 2½ tbsp
  - #51: olive oil 1 tbsp vs ½ cup (marinade)
  - #55: olive oil 1 tsp vs 2 tbsp; missing second oil, salt
  - #56: "3 lb root vegetables such as…" expanded to carrot/parsnip/beet/sweet potato at 1 lb each (4 lb)

- **Fine (~40)**: correct ingredients and quantities; at most a missing "salt to taste"/garnish line or a range stored as its midpoint.

Raw content diff per recipe (fuzzy, includes matcher noise — the verdicts above are the hand-checked reading of it): `docs/recipe-scraper/prod-content-diff.txt`.
