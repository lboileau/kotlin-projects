#!/usr/bin/env python3
"""Replace the ingredient lines of the recipes the old scraper got wrong, in place, with the new-path results.

  python3 fix-recipe-ingredients.py plan               # dry run: print exactly what would change, call nothing
  python3 fix-recipe-ingredients.py apply              # add new lines first, then delete old ones; logs to fix.log.json
  python3 fix-recipe-ingredients.py rollback           # undo using fix.log.json + prod-backup/
  add --include-moderate to also fix the 6 recipes with one or two bad lines

  CAMPER_API=http://localhost:8081/api python3 fix-recipe-ingredients.py apply   # against a local DB seeded from prod

Recipe ids are preserved, so favourites and meal-plan references are untouched. Unmatched ingredients are
created in the catalogue (POST /api/ingredients) from the scraper's suggested name/category/unit.
Inputs (all in this folder): recipes.json (case order), ingredients.json (prod catalogue at the time),
results/<case>.json (new-path output after Kotlin post-processing), prod-backup/recipe-<id>.json.
"""
import json, os, sys, urllib.request, urllib.error

BASE = os.path.dirname(os.path.abspath(__file__))
API = os.environ.get('CAMPER_API', 'https://www.canoecamp.life/api').rstrip('/')
WRONG = ['00','03','08','09','11','13','18','22','24','30','32','35','36','41','47','52','53','57','60','61','68','69']
MODERATE = ['28','31','44','51','55','56']
CASES = WRONG + (MODERATE if '--include-moderate' in sys.argv else [])
LOG = f'{BASE}/fix.log.json'

recipes = json.load(open(f'{BASE}/recipes.json'))
catalogue = json.load(open(f'{BASE}/ingredients.json'))
cat_by_id = {c['id']: c for c in catalogue}
cat_by_name = {c['name'].strip().lower(): c for c in catalogue}

def is_junk(line):
    t = line['originalText'].strip()
    return line['matchedIngredientId'] is None and (
        not t or t.endswith(':') or t.startswith('*') or t.startswith('OR ') or 'tip' in t.lower() or 'and more' in t.lower())

def build_plan():
    plan = []
    for case in CASES:
        r = recipes[int(case)]
        result = json.load(open(f'{BASE}/results/{case}.json'))
        backup = json.load(open(f'{BASE}/prod-backup/recipe-{r["id"]}.json'))
        adds, skipped = [], []
        for l in result['ingredients']:
            if is_junk(l):
                skipped.append(l['originalText']); continue
            if l['matchedIngredientId']:
                adds.append(dict(ingredientId=l['matchedIngredientId'], name=cat_by_id[l['matchedIngredientId']]['name'],
                                 quantity=l['quantity'], unit=l['unit'], confidence=l['confidence'], text=l['originalText']))
            else:
                name = (l['suggestedIngredientName'] or l['originalText']).strip().lower()
                existing = cat_by_name.get(name)
                adds.append(dict(ingredientId=existing['id'] if existing else None, name=name,
                                 newIngredient=None if existing else dict(name=name, category=l['suggestedCategory'] or 'other', defaultUnit=l['suggestedUnit'] or l['unit']),
                                 quantity=l['quantity'], unit=l['unit'], confidence='NEW', text=l['originalText']))
        plan.append(dict(case=case, recipeId=r['id'], name=r['name'], userId=backup['createdBy'],
                         oldLines=[dict(id=l['id'], ingredientId=(l.get('ingredient') or {}).get('id'), quantity=l['quantity'], unit=l['unit'],
                                        name=(l.get('ingredient') or {}).get('name')) for l in backup['ingredients']],
                         adds=adds, skipped=skipped))
    return plan

def show(plan):
    new_ings = {}
    for p in plan:
        print(f"\n## {p['case']} {p['name']}  ({p['recipeId']})")
        print(f"   delete {len(p['oldLines'])} lines: " + ', '.join(f"{o['quantity']:g} {o['unit']} {o['name']}" for o in p['oldLines']))
        print(f"   add {len(p['adds'])} lines:")
        for a in p['adds']:
            tag = '' if a['confidence'] == 'HIGH' else f"  [{a['confidence']}]"
            print(f"     {a['quantity']:>6g} {a['unit']:6} {a['name']:28}{tag}   ← {a['text'][:60]}")
            if a.get('newIngredient'): new_ings[a['name']] = a['newIngredient']
        for s in p['skipped']: print(f"     (skipping non-ingredient line: {s[:60]!r})")
    print(f"\n{len(plan)} recipes, {sum(len(p['oldLines']) for p in plan)} lines deleted, {sum(len(p['adds']) for p in plan)} lines added")
    print(f"{len(new_ings)} new catalogue ingredients would be created: " + ', '.join(f"{n} ({v['category']}, {v['defaultUnit']})" for n, v in sorted(new_ings.items())))
    low = [(p['name'], a) for p in plan for a in p['adds'] if a['confidence'] == 'LOW']
    print(f"{len(low)} LOW-confidence matches will be stored as approved (eyeball these): " + '; '.join(f"{a['text'][:30]!r}→{a['name']}" for _, a in low))
    print(f"\nAPI: {API}")

def call(method, path, user_id, body=None):
    req = urllib.request.Request(API + path, method=method, data=json.dumps(body).encode() if body is not None else None,
                                 headers={'X-User-Id': user_id, 'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]
    except urllib.error.URLError as e:
        return 0, str(e)

def apply(plan):
    print(f"applying against {API}")
    log = []
    created = {}
    for p in plan:
        uid = p['userId']
        status, detail = call('GET', f"/recipes/{p['recipeId']}", uid)
        if status != 200: print(f"!! {p['name']}: GET failed {status} {detail}"); continue
        old_ids = [l['id'] for l in detail['ingredients']]
        entry = dict(case=p['case'], recipeId=p['recipeId'], userId=uid, added=[], deleted=[], createdIngredients=[])
        ok = True
        for a in p['adds']:
            ing_id = a['ingredientId']
            if ing_id is None:
                ni = a['newIngredient']
                if ni['name'] in created: ing_id = created[ni['name']]
                else:
                    s, body = call('POST', '/ingredients', uid, ni)
                    if s != 201: print(f"!! create ingredient {ni['name']}: {s} {body}"); ok = False; break
                    ing_id = created[ni['name']] = body['id']; entry['createdIngredients'].append(body['id'])
            s, body = call('POST', f"/recipes/{p['recipeId']}/ingredients", uid, dict(ingredientId=ing_id, quantity=a['quantity'], unit=a['unit']))
            if s != 201: print(f"!! {p['name']}: add {a['name']} failed {s} {body}"); ok = False; break
            entry['added'].append(body['id'])
        if ok:
            for lid in old_ids:
                s, body = call('DELETE', f"/recipes/{p['recipeId']}/ingredients/{lid}", uid)
                if s != 204: print(f"!! {p['name']}: delete {lid} failed {s} {body}"); ok = False; break
                entry['deleted'].append(lid)
        log.append(entry)
        json.dump(log, open(LOG, 'w'), indent=1)
        print(f"{'ok ' if ok else 'PARTIAL'} {p['case']} {p['name']}: +{len(entry['added'])} -{len(entry['deleted'])}")

def rollback():
    log = json.load(open(LOG))
    for e in log:
        backup = json.load(open(f'{BASE}/prod-backup/recipe-{e["recipeId"]}.json'))
        uid = e['userId']
        for lid in e['added']:
            call('DELETE', f"/recipes/{e['recipeId']}/ingredients/{lid}", uid)
        restored = 0
        for l in backup['ingredients']:
            ing = (l.get('ingredient') or {}).get('id')
            if not ing: continue
            s, _ = call('POST', f"/recipes/{e['recipeId']}/ingredients", uid, dict(ingredientId=ing, quantity=l['quantity'], unit=l['unit']))
            restored += (s == 201)
        print(f"rolled back {e['case']}: removed {len(e['added'])}, restored {restored}/{len(backup['ingredients'])}")

if __name__ == '__main__':
    mode = next((a for a in sys.argv[1:] if not a.startswith('--')), 'plan')
    plan = build_plan()
    if mode == 'plan': show(plan)
    elif mode == 'apply': apply(plan)
    elif mode == 'rollback': rollback()
    else: sys.exit(__doc__)
