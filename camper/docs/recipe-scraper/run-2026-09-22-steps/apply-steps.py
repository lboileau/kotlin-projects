#!/usr/bin/env python3
"""Apply steps-plan.json to prod: for each recipe, GET it and PUT the steps only if it has none.
   --dry-run prints what would change; --clear replaces every planned recipe's steps with [] (rollback)."""
import json, sys, urllib.request, os
API = os.environ.get('CAMPER_API', 'https://camper-service-production.up.railway.app')
UID = os.environ.get('CAMPER_USER', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44')
dry = '--dry-run' in sys.argv; clear = '--clear' in sys.argv
plan = json.load(open(os.path.join(os.path.dirname(__file__), 'steps-plan.json')))
def call(method, path, body=None):
    req = urllib.request.Request(API + path, data=json.dumps(body).encode() if body is not None else None,
        headers={'X-User-Id': UID, 'Content-Type': 'application/json'}, method=method)
    with urllib.request.urlopen(req) as r: return json.load(r)
applied = skipped = failed = 0
for item in plan:
    try:
        current = call('GET', f"/api/recipes/{item['id']}")
    except Exception as e:
        print(f"  FAIL get {item['name'][:40]}: {e}"); failed += 1; continue
    if not clear and current.get('steps'):
        skipped += 1; continue
    steps = [] if clear else item['steps']
    if dry:
        print(f"  would {'clear' if clear else 'set ' + str(len(steps)) + ' steps on'} {item['name'][:50]}"); applied += 1; continue
    try:
        call('PUT', f"/api/recipes/{item['id']}/steps", {'steps': steps}); applied += 1
        print(f"  ok  {len(steps):2d}  {item['name'][:50]}")
    except Exception as e:
        print(f"  FAIL put {item['name'][:40]}: {e}"); failed += 1
print(f"{'dry run: ' if dry else ''}{applied} applied, {skipped} skipped (already had steps), {failed} failed")
