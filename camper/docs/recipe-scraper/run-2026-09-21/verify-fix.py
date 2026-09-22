#!/usr/bin/env python3
"""Independent check after fix-recipe-ingredients.py apply: reads each fixed recipe back from the API and
compares its lines (ingredient, quantity, unit, status) to the expected new-path result.
  CAMPER_API=http://localhost:8081/api python3 verify-fix.py     # default: prod
"""
import json, os, sys, urllib.request

BASE = os.path.dirname(os.path.abspath(__file__))
API = os.environ.get('CAMPER_API', 'https://www.canoecamp.life/api').rstrip('/')
H = {'X-User-Id': '00000000-0000-0000-0000-000000000000'}
get = lambda p: json.load(urllib.request.urlopen(urllib.request.Request(API + p, headers=H), timeout=30))

def junk(l):
    t = l['originalText'].strip()
    return l['matchedIngredientId'] is None and (not t or t.endswith(':') or t.startswith('*') or t.startswith('OR ') or 'tip' in t.lower() or 'and more' in t.lower())

log = json.load(open(f'{BASE}/fix.log.json'))
cat = {c['id']: c['name'] for c in get('/ingredients')}
bad = 0
for e in log:
    exp = [l for l in json.load(open(f"{BASE}/results/{e['case']}.json"))['ingredients'] if not junk(l)]
    got = get(f"/recipes/{e['recipeId']}")['ingredients']
    want = [((cat.get(l['matchedIngredientId']) or l['suggestedIngredientName']).lower(), float(l['quantity']), l['unit']) for l in exp]
    have = [(l['ingredient']['name'].lower(), float(l['quantity']), l['unit']) for l in got if l.get('ingredient')]
    statuses = {l['status'] for l in got}
    if want != have or statuses != {'approved'} or len(have) != len(got):
        bad += 1
        print(f"MISMATCH {e['case']}: expected {len(want)} lines, got {len(got)} (statuses {statuses})")
        for w, h in zip(want, have):
            if w != h: print(f"   want {w}  got {h}")
    else:
        print(f"ok {e['case']}: {len(have)} lines")
print(f"\n{len(log) - bad}/{len(log)} recipes match expected against {API}")
sys.exit(1 if bad else 0)
