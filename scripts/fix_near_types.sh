#!/usr/bin/env bash
set -euo pipefail
MODULE=near-jsonrpc-types
BASE="$MODULE/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models"
./gradlew ":$MODULE:downloadOpenApiSpec" ":$MODULE:patchOpenApiSpec" ":$MODULE:openApiGenerate" ":$MODULE:fixAndCopyGenerated"
python3 - <<'PY'
import re, pathlib, sys
base=pathlib.Path("near-jsonrpc-types/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models")
if not base.exists():
    sys.exit(0)
pkg_rx=re.compile(r'^(package[^\n]*\n)', re.M)
cls_rx=re.compile(r'(^|\n)(\s*(?:@[^\n]*\n\s*)*)(?:sealed\s+)?(?:(?:data|enum)\s+)?(?:class|object)\s+([A-Z][A-Za-z0-9_]*)', re.M)
iface_rx=re.compile(r'(^|\n)(\s*(?:@[^\n]*\n\s*)*)interface\s+([A-Z][A-Za-z0-9_]*)', re.M)
ser_decl_rx=re.compile(r'@(?:kotlinx\.serialization\.)?Serializable\s*(?:\n\s*@[^\n]+\s*)*\n\s*(?:sealed\s+)?(?:(?:data|enum)\s+)?(?:class|object)\s+([A-Z][A-Za-z0-9_]*)', re.M)
decl_decl_rx=re.compile(r'(?:^|\n)\s*(?:@[^\n]*\n\s*)*(?:sealed\s+)?(?:(?:data|enum)\s+)?(?:class|object)\s+([A-Z][A-Za-z0-9_]*)', re.M)
prop_rx=re.compile(r'(^|[,(])(\s*)((?:@[\w.:]+(?:\([^()]*\))?\s*)*)(val|var)\s+([A-Za-z_]\w*)\s*:\s*([^=,\r\n)]+)', re.M)
ann_token_rx=re.compile(r'@\w[\w.:]*(?:\([^()]*\))?')
any_rx=re.compile(r'\bAny(\??)')
def read(p): return p.read_text(encoding="utf-8")
def write(p,s): p.write_text(s,encoding="utf-8")
def ensure_import(s,imp):
    if re.search(rf'(?m)^\s*import\s+{re.escape(imp)}\s*$',s): return s
    m=pkg_rx.search(s)
    return (s[:m.end()]+f"import {imp}\n"+s[m.end():]) if m else f"import {imp}\n{s}"
def dedup_imports(s):
    m=pkg_rx.search(s)
    if m:
        head=s[:m.end()]; body=s[m.end():]
        imps=re.findall(r'(?m)^\s*import\s+[^\n]+$',body)
        body=re.sub(r'(?m)^\s*import\s+[^\n]+$\n?','',body)
        uniq=[]
        seen=set()
        for i in imps:
            if i in seen: continue
            seen.add(i); uniq.append(i)
        return head+("\n".join(uniq)+("\n" if uniq else ""))+body
    imps=re.findall(r'(?m)^\s*import\s+[^\n]+$',s)
    s=re.sub(r'(?m)^\s*import\s+[^\n]+$\n?','',s)
    uniq=[]; seen=set()
    for i in imps:
        if i in seen: continue
        seen.add(i); uniq.append(i)
    return ("\n".join(uniq)+("\n" if uniq else ""))+s
def dedup_annot_lines(s):
    lines=s.splitlines(True); out=[]; i=0
    while i<len(lines):
        if re.match(r'^\s*@',lines[i] or ''):
            block=[]
            while i<len(lines) and re.match(r'^\s*@',lines[i] or ''): block.append(lines[i]); i+=1
            seen=set();clean=[]
            for a in block:
                m=re.match(r'^\s*@([\w.:]+)',a); key=m.group(1) if m else a
                if key in seen: continue
                seen.add(key); clean.append(a)
            out.extend(clean)
        else:
            out.append(lines[i]); i+=1
    return ''.join(out)
def make_oneof_stub(name, pkgline):
    return f"{pkgline}import kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement\n\n@Serializable\ndata class {name}(val raw: JsonElement)\n"
for p in base.rglob("*.kt"):
    s=read(p)
    if re.search(r'(OneOf|AnyOf|AllOf)', p.name):
        m=pkg_rx.search(s); pkg=m.group(1) if m else "package org.near.jsonrpc.types.models\n"
        write(p, make_oneof_stub(p.stem, pkg))
for p in base.rglob("*.kt"):
    s=read(p)
    if iface_rx.search(s): continue
    if ("@Serializable" not in s and "@kotlinx.serialization.Serializable" not in s) and re.search(r'\b(class|object)\b', s):
        s=re.sub(r'(^|\n)(\s*(?:@[^\n]*\n\s*)*)(?=(?:sealed\s+)?(?:(?:data|enum)\s+)?(?:class|object)\s+[A-Z])',
                 r'\1\2@kotlinx.serialization.Serializable\n', s, count=1)
        s=ensure_import(s,'kotlinx.serialization.Serializable')
        write(p,s)
decls={}
serializable=set()
for p in base.rglob("*.kt"):
    s=read(p)
    for n in decl_decl_rx.findall(s):
        decls.setdefault(n, []).append(p)
    for n in ser_decl_rx.findall(s):
        serializable.add(n)
for n,files in list(decls.items()):
    if len(files)>1:
        ranked=[]
        for f in files:
            s=read(f)
            score=((('@Serializable' in s) or ('@kotlinx.serialization.Serializable' in s)), s.count('\n'), -len(s))
            ranked.append((score,f))
        ranked.sort(reverse=True)
        keep=ranked[0][1]
        for _,f in ranked[1:]:
            try: f.unlink()
            except: pass
decls.clear(); serializable.clear()
for p in base.rglob("*.kt"):
    s=read(p)
    for n in decl_decl_rx.findall(s):
        decls.setdefault(n, []).append(p)
    for n in ser_decl_rx.findall(s):
        serializable.add(n)
all_types=set(decls.keys())
nonser=all_types-serializable
def should_contextual(tp:str)->bool:
    clean=re.sub(r'\b(kotlin(?:x)?\.[\w.]+|java\.[\w.]+)\b',' ',tp)
    clean=re.sub(r'[\[\]<>?,]|Mutable|Immutable',' ',clean)
    for n in re.findall(r'\b([A-Z][A-Za-z0-9_]*)\b', clean):
        if n in {"List","Set","Map","Array","Pair","Triple"}: continue
        if n in nonser: return True
    return False
for p in base.rglob("*.kt"):
    s=read(p)
    orig=s
    def repl(m):
        pre, spaces, ann, valvar, name, typ = m.groups()
        toks=ann_token_rx.findall(ann)
        seen=set();ann2=[]
        for t in toks:
            if t in seen: continue
            seen.add(t); ann2.append(t)
        ann2=(' '.join(ann2)+' ') if ann2 else ''
        typ2=any_rx.sub(lambda mm: 'kotlinx.serialization.json.JsonElement'+(mm.group(1) or ''), typ)
        if should_contextual(typ2) and '@Contextual' not in ann2:
            ann2='@Contextual '+ann2
        return f"{pre}{spaces}{ann2}{valvar} {name}: {typ2}"
    s=prop_rx.sub(repl,s)
    s=dedup_annot_lines(s)
    if '@Contextual' in s:
        s=ensure_import(s,'kotlinx.serialization.Contextual')
    if 'kotlinx.serialization.json.JsonElement' in s:
        s=ensure_import(s,'kotlinx.serialization.json.JsonElement')
    s=re.sub(r'(?s)\A(@file:Suppress\([^\)]*\)\s*\n)(?:\s*\1)+', r'\1', s)
    s=dedup_imports(s)
    if s!=orig: write(p,s)
missing=[]
for name in ['StateChangeKindView','StateChangeWithCauseView']:
    if name not in all_types:
        missing.append(name)
for name in missing:
    path = base / f"{name}.kt"
    if not path.exists():
        pkg = "package org.near.jsonrpc.types.models\n"
        path.write_text(make_oneof_stub(name, pkg), encoding="utf-8")
print("OK", base)
PY
