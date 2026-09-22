#!/usr/bin/env python3
"""Merge duplicate ingredient lines within each recipe, in place, via the API.

Recipe pages list ingredients per section, so the same ingredient appears several times. This mirrors
ScrapedIngredientMerger (clients/recipe-scraper-client): lines with the same ingredient and compatible
units are summed — same unit directly; volume↔volume / weight↔weight in the smallest unit, shown in the
largest unit if that reads cleanly (whole or .25/.5/.75); count units only when identical.

  python3 merge-duplicate-lines.py plan       # dry run
  python3 merge-duplicate-lines.py apply      # add merged line, then delete the originals; logs to merge.log.json
  python3 merge-duplicate-lines.py verify     # no recipe has a mergeable pair left; merged totals equal the originals
  python3 merge-duplicate-lines.py rollback   # delete merged lines, re-add the originals from merge.log.json
  CAMPER_API=http://localhost:8081/api MERGE_LOG=merge.log.local.json ...   # against the local clone
"""
import json, os, sys, time, urllib.request, urllib.error
from decimal import Decimal

BASE = os.path.dirname(os.path.abspath(__file__))
API = os.environ.get('CAMPER_API', 'https://www.canoecamp.life/api').rstrip('/')
LOG = os.environ.get('MERGE_LOG', f'{BASE}/merge.log.json')
UID = '00000000-0000-0000-0000-000000000000'   # reads; writes use each recipe's creator

VOLUME = {'tsp': Decimal('1'), 'tbsp': Decimal('3'), 'cup': Decimal('48'), 'ml': Decimal('0.202884'), 'l': Decimal('202.884')}
WEIGHT = {'g': Decimal('1'), 'kg': Decimal('1000'), 'oz': Decimal('28.3495'), 'lb': Decimal('453.592')}

def family(u): return VOLUME if u in VOLUME else WEIGHT if u in WEIGHT else None
def compatible(a, b): return a == b or (family(a) is not None and family(a) is family(b))
def clean(v): return (v % 1) in (Decimal('0'), Decimal('0.25'), Decimal('0.5'), Decimal('0.75'))

def total(lines):
    units = sorted({l['unit'] for l in lines}, key=lambda u: family(u)[u] if family(u) else 0)
    if len(units) == 1: return sum(Decimal(str(l['quantity'])) for l in lines), units[0]
    fam = family(units[0]); small, large = units[0], units[-1]
    t = sum(Decimal(str(l['quantity'])) * fam[l['unit']] / fam[small] for l in lines)
    in_large = (t * fam[small] / fam[large]).quantize(Decimal('0.01'))
    return (in_large, large) if clean(in_large) else (t.quantize(Decimal('0.01')), small)

def call(method, path, user_id, body=None):
    for attempt in range(6):
        req = urllib.request.Request(API + path, method=method, data=json.dumps(body).encode() if body is not None else None,
                                     headers={'X-User-Id': user_id, 'Content-Type': 'application/json'})
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                raw = resp.read(); return resp.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:
            if e.code < 500: return e.code, e.read().decode()[:300]
            status, detail = e.code, e.read().decode()[:300]
        except urllib.error.URLError as e:
            status, detail = 0, str(e)
        wait = 5 * (attempt + 1); print(f"   {method} {path}: {status} — retrying in {wait}s"); time.sleep(wait)
    return status, detail

def groups_for(recipe):
    """[(ingredient, [lines])] buckets of ≥2 lines that can be summed."""
    by_ing = {}
    for l in recipe['ingredients']:
        if l.get('ingredient'): by_ing.setdefault(l['ingredient']['id'], []).append(l)
    out = []
    for lines in by_ing.values():
        buckets = []
        for l in lines:
            b = next((b for b in buckets if compatible(b[0]['unit'], l['unit'])), None)
            if b is not None: b.append(l)
            else: buckets.append([l])
        out += [(b[0]['ingredient'], b) for b in buckets if len(b) > 1]
    return out

def build_plan():
    recipes = [r for r in json.load(open(f'{BASE}/recipes.json'))]
    plan = []
    for r in recipes:
        s, detail = call('GET', f"/recipes/{r['id']}", UID)
        if s != 200: print(f"!! {r['name']}: GET {s}"); continue
        merges = []
        for ing, lines in groups_for(detail):
            q, u = total(lines)
            merges.append(dict(ingredientId=ing['id'], name=ing['name'], quantity=float(q), unit=u,
                               originals=[dict(id=l['id'], quantity=l['quantity'], unit=l['unit']) for l in lines]))
        if merges: plan.append(dict(recipeId=r['id'], name=r['name'], userId=detail['createdBy'], merges=merges))
    return plan

def show(plan):
    for p in plan:
        print(f"\n## {p['name']}")
        for m in p['merges']:
            parts = ' + '.join('%g %s' % (o['quantity'], o['unit']) for o in m['originals'])
            print(f"   {parts}  →  {m['quantity']:g} {m['unit']} {m['name']}")
    print(f"\n{len(plan)} recipes, {sum(len(m['originals']) for p in plan for m in p['merges'])} lines → {sum(len(p['merges']) for p in plan)}   ({API})")

def finish_interrupted(log):
    """A merge is recorded as soon as its merged line is added. If a previous run died before deleting
    the originals, delete them now — otherwise a fresh plan would sum merged line + originals again."""
    for e in log:
        for m in e['merges']:
            pending = [o['id'] for o in m['originals'] if o['id'] not in m['deleted']]
            if not pending: continue
            s, detail = call('GET', f"/recipes/{e['recipeId']}", UID)
            live = {l['id'] for l in detail['ingredients']} if s == 200 else set()
            if m['addedId'] not in live: continue   # merged line never landed; nothing to finish
            for oid in pending:
                if oid in live and call('DELETE', f"/recipes/{e['recipeId']}/ingredients/{oid}", e['userId'])[0] == 204:
                    m['deleted'].append(oid); print(f"   finished interrupted merge in {e['name']}: deleted {oid}")

def apply():
    log = json.load(open(LOG)) if os.path.exists(LOG) else []
    finish_interrupted(log)
    plan = build_plan()
    by_recipe = {e['recipeId']: e for e in log}
    for p in plan:
        entry = by_recipe.setdefault(p['recipeId'], dict(recipeId=p['recipeId'], name=p['name'], userId=p['userId'], merges=[]))
        if entry not in log: log.append(entry)
        for m in p['merges']:
            s, body = call('POST', f"/recipes/{p['recipeId']}/ingredients", p['userId'], dict(ingredientId=m['ingredientId'], quantity=m['quantity'], unit=m['unit']))
            if s != 201: print(f"!! {p['name']}: add {m['name']} failed {s} {body}"); break
            rec = dict(m, addedId=body['id'], deleted=[])
            entry['merges'].append(rec); json.dump(log, open(LOG, 'w'), indent=1)   # record before deleting
            for o in m['originals']:
                s, body = call('DELETE', f"/recipes/{p['recipeId']}/ingredients/{o['id']}", p['userId'])
                if s != 204: print(f"!! {p['name']}: delete {o['id']} failed {s} {body}"); break
                rec['deleted'].append(o['id']); json.dump(log, open(LOG, 'w'), indent=1)
        print(f"ok {p['name']}: {sum(len(m['originals']) for m in p['merges'])} lines → {len(p['merges'])}")
    if not plan: print("nothing to merge")

def verify():
    log = json.load(open(LOG)); bad = 0
    for e in log:
        s, detail = call('GET', f"/recipes/{e['recipeId']}", UID)
        left = groups_for(detail)
        by_id = {l['id']: l for l in detail['ingredients']}
        for m in e['merges']:
            got = by_id.get(m['addedId'])
            exp_q, exp_u = total([dict(quantity=o['quantity'], unit=o['unit']) for o in m['originals']])
            if not got or Decimal(str(got['quantity'])) != exp_q or got['unit'] != exp_u or any(o['id'] in by_id for o in m['originals']):
                bad += 1; print(f"MISMATCH {e['name']} / {m['name']}: expected {exp_q} {exp_u}, got {got and (got['quantity'], got['unit'])}")
        if left: bad += 1; print(f"STILL MERGEABLE {e['name']}: {[(i['name'], len(l)) for i, l in left]}")
    print(f"{len(log) - bad if bad <= len(log) else 0}/{len(log)} recipes verified against {API}")
    sys.exit(1 if bad else 0)

def rollback():
    for e in json.load(open(LOG)):
        for m in e['merges']:
            call('DELETE', f"/recipes/{e['recipeId']}/ingredients/{m['addedId']}", e['userId'])
            for o in m['originals']:
                call('POST', f"/recipes/{e['recipeId']}/ingredients", e['userId'], dict(ingredientId=m['ingredientId'], quantity=o['quantity'], unit=o['unit']))
        print(f"rolled back {e['name']}")

if __name__ == '__main__':
    mode = sys.argv[1] if len(sys.argv) > 1 else 'plan'
    if mode == 'plan': show(build_plan())
    elif mode == 'apply': apply()
    elif mode == 'verify': verify()
    elif mode == 'rollback': rollback()
    else: sys.exit(__doc__)
