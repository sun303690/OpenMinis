#!/usr/bin/env python3
import json, os, re, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
with open(os.path.join(ROOT, 'kb.json'), encoding='utf-8') as f:
    D = json.load(f)
KB, LABELS = D['kb'], D['labels']
def entries():
    for c, items in KB.items():
        for i, e in items.items():
            yield c, i, e
def norm(s): return (s or '').lower()
def toks(s):
    t = norm(s); out = re.findall(r'[a-z0-9][a-z0-9._-]*', t)
    for run in re.findall(r'[一-鿿]+', t):
        if len(run) == 1: out.append(run)
        else: out += [run[j:j+2] for j in range(len(run)-1)]
    return out
def score(e, q):
    title, body, cve = norm(e.get('t')), norm(e.get('b')), norm(e.get('c'))
    hay = title + ' ' + cve + ' ' + body
    s = 0
    for w in toks(q):
        s += 6*title.count(w) + 4*cve.count(w) + min(body.count(w), 10)
    return s
def short(i, c, e):
    return f'{c}/{i}  [{e.get("s","?")}]  {e.get("t","")}  ({e.get("c","")})'
cmd = sys.argv[1] if len(sys.argv) > 1 else 'list'
if cmd == 'search':
    q = ' '.join(sys.argv[2:]) or sys.stdin.read()
    hits = sorted(((score(e,q), c, i, e) for c,i,e in entries()), key=lambda x:-x[0])
    hits = [h for h in hits if h[0] > 0][:12]
    if not hits: print('no hits'); sys.exit(0)
    for s,c,i,e in hits: print(f'{s:4d}  {short(i,c,e)}')
elif cmd == 'read':
    q = ' '.join(sys.argv[2:])
    for c,i,e in entries():
        if i == q or f'{c}/{i}' == q:
            print(f'# {e.get("t")}\n\n- id: {c}/{i}\n- severity: {e.get("s")}\n- cve: {e.get("c")}\n\n{e.get("b")}')
            sys.exit(0)
    hits = sorted(((score(e,q), c, i, e) for c,i,e in entries()), key=lambda x:-x[0])
    if hits and hits[0][0] > 0:
        s,c,i,e = hits[0]
        print(f'# {e.get("t")}   (matched {c}/{i}, score {s})\n\n{e.get("b")}')
    else: print('no match')
elif cmd == 'list':
    only = sys.argv[2] if len(sys.argv) > 2 else None
    for c,i,e in entries():
        if only and c != only: continue
        print(short(i,c,e))
elif cmd == 'cats':
    for c in KB: print(f'{c:18s} {LABELS.get(c,c):20s} {len(KB[c])} entries')
else:
    print('usage: kb.py search <q> | read <id> | list [cat] | cats')
