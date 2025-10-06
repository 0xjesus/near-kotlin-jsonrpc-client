import sys, re, pathlib
models_dir = pathlib.Path(sys.argv[1] if len(sys.argv)>1 else "near-jsonrpc-types/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models")
if not models_dir.exists():
    print("Models dir not found:", models_dir); sys.exit(0)
pkg_rx = re.compile(r'^(package [^\n]+)\n', re.M)
def rd(p): return p.read_text(encoding="utf-8")
def wr(p,s): p.write_text(s, encoding="utf-8")
def ensure_import(s, imp):
    if re.search(rf'(?m)^import\s+{re.escape(imp)}\s*$', s): return s
    m = pkg_rx.search(s)
    return (s[:m.end()] + f"import {imp}\n" + s[m.end():]) if m else f"import {imp}\n{s}"
def dedup_imports(s):
    m = pkg_rx.search(s)
    head_end = m.end() if m else 0
    pre = s[:head_end]; rest = s[head_end:]
    imps = re.findall(r'(?m)^import\s+[^\n]+\n', rest)
    body = re.sub(r'(?m)^import\s+[^\n]+\n', '', rest)
    uniq=[]; seen=set()
    for i in imps:
        if i not in seen:
            seen.add(i); uniq.append(i)
    return pre + ''.join(uniq) + body
def dedup_file_suppress(s):
    return re.sub(r'(?s)\A(@file:Suppress\([^\)]*\)\s*\n)(?:\s*\1)+', r'\1', s)
def dedup_annot_blocks(s):
    lines=s.splitlines(True); out=[]; i=0
    while i<len(lines):
        if re.match(r'^\s*@', lines[i] or ''):
            blk=[]; 
            while i<len(lines) and re.match(r'^\s*@', lines[i] or ''):
                blk.append(lines[i]); i+=1
            seen=set(); clean=[]
            for a in blk:
                m=re.match(r'^\s*@([\w.:]+)', a)
                k=m.group(1) if m else a
                if k in seen: continue
                seen.add(k); clean.append(a)
            out.extend(clean)
        else:
            out.append(lines[i]); i+=1
    return ''.join(out)
def ensure_serializable(s):
    lines=s.splitlines(True); out=[]; i=0
    while i<len(lines):
        ln=lines[i]
        m=re.match(r'^(\s*)(?:sealed\s+)?(?:data\s+)?(?:enum\s+class|class)\s+[A-Z][A-Za-z0-9_]*\b', ln)
        if m:
            j=len(out)-1; has_ser=False
            while j>=0 and (out[j].strip()=='' or out[j].lstrip().startswith('@')):
                if '@Serializable' in out[j] or '@kotlinx.serialization.Serializable' in out[j]: has_ser=True
                j-=1
            if not has_ser:
                out.append(m.group(1)+'@Serializable\n')
            out.append(ln); i+=1; continue
        out.append(ln); i+=1
    s=''.join(out)
    if '@Serializable' in s: s=ensure_import(s,'kotlinx.serialization.Serializable')
    return s
def replace_any_with_json_element(s):
    s=re.sub(r'(<\s*)Any(\s*>)', r'\1kotlinx.serialization.json.JsonElement\2', s)
    s=re.sub(r'(<\s*List\s*<)\s*Any\??(\s*>)', r'\1kotlinx.serialization.json.JsonElement\2', s)
    s=re.sub(r'(<\s*Set\s*<)\s*Any\??(\s*>)', r'\1kotlinx.serialization.json.JsonElement\2', s)
    s=re.sub(r'(<\s*Map\s*<[^,>]+,\s*)Any\??(\s*>)', r'\1kotlinx.serialization.json.JsonElement\2', s)
    s=re.sub(r'(:\s*)Any(\?)?\b', r'\1kotlinx.serialization.json.JsonElement\2', s)
    s=ensure_import(s,'kotlinx.serialization.json.JsonElement')
    return s
for p in models_dir.rglob('*.kt'):
    if re.search(r'(OneOf|AnyOf|AllOf)', p.name):
        s=rd(p); m=pkg_rx.search(s)
        pkg=(m.group(1) if m else 'package org.near.jsonrpc.types.models')+'\n'
        body=pkg+'import kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement\n\n@Serializable\ndata class '+p.stem+'(val raw: JsonElement)\n'
        wr(p, body)
for p in models_dir.rglob('*.kt'):
    s=rd(p); orig=s
    s=dedup_file_suppress(s)
    s=dedup_annot_blocks(s)
    s=ensure_serializable(s)
    s=replace_any_with_json_element(s)
    s=dedup_annot_blocks(s)
    s=dedup_imports(s)
    if s!=orig: wr(p,s)
print("Patched:", models_dir)
