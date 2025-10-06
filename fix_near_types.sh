set -e
MODULE=near-jsonrpc-types
MODDIR="$MODULE"
GEN_DIR="$MODDIR/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models"
./gradlew -p "$MODDIR" clean || true
rm -rf "$MODDIR/build/generated-fixed" "$MODDIR/build/generated-raw"
./gradlew -p "$MODDIR" downloadOpenApiSpec fixAndCopyGenerated
python3 - "$GEN_DIR" <<'PY'
import sys,re,pathlib
base=pathlib.Path(sys.argv[1])
if not base.exists(): 
    sys.exit(0)
pkg_line='package org.near.jsonrpc.types.models\n'
pkg_rx=re.compile(r'^(package[^\n]*\n)',re.M)
decl_rx=re.compile(r'^\s*(?:@[\w.:]+(?:\([^)]*\))?\s*)*(?:sealed\s+)?(?:data\s+)?(?:enum\s+class|class|object|interface)\s+([A-Z][A-Za-z0-9_]*)',re.M)
hdr_rx=re.compile(r'^(\s*)((?:@[\w.:]+(?:\([^)]*\))?\s*)*)((?:sealed\s+)?(?:data\s+)?)(enum\s+class|class|object|interface)\s+([A-Z][A-Za-z0-9_]*)',re.M)
prop_rx=re.compile(r'(^|[,(])(\s*)((?:@[\w.:]+(?:\([^()]*\))?\s*)*)(val|var)\s+([A-Za-z_]\w*)\s*:\s*([^=,\r\n)]+)',re.M)
any_rx=re.compile(r'\b(?:kotlin\.)?Any(\?)?\b')
ann_token_rx=re.compile(r'@[\w.:]+(?:\([^()]*\))?')
def read(p): 
    return p.read_text(encoding='utf-8')
def write(p,s): 
    p.write_text(s,encoding='utf-8')
def get_pkg(s):
    m=pkg_rx.search(s)
    return m.group(1) if m else pkg_line
def ensure_import(s,imp):
    if f"import {imp}\n" in s: 
        return s
    m=pkg_rx.search(s)
    return s[:m.end()]+f"import {imp}\n"+s[m.end():] if m else f"import {imp}\n{s}"
def dedup_imports(s):
    lines=s.splitlines(True)
    seen=set(); out=[]
    for ln in lines:
        if ln.startswith('import '):
            if ln in seen: 
                continue
            seen.add(ln)
        out.append(ln)
    return ''.join(out)
def dedup_annot_lines(s):
    lines=s.splitlines(True)
    out=[]; i=0
    while i<len(lines):
        if lines[i].lstrip().startswith('@'):
            block=[]
            while i<len(lines) and lines[i].lstrip().startswith('@'):
                block.append(lines[i]); i+=1
            seen=set(); keep=[]
            for a in block:
                m=re.match(r'^\s*@([\w.:]+)',a)
                k=m.group(1) if m else a
                if k in seen: 
                    continue
                seen.add(k); keep.append(a)
            out.extend(keep)
        else:
            out.append(lines[i]); i+=1
    return ''.join(out)
def dedup_inline_annotations(ann):
    toks=ann_token_rx.findall(ann or '')
    if not toks: 
        return ann or ''
    seen=set(); keep=[]
    for t in toks:
        k=re.match(r'@([\w.:]+)',t).group(1)
        if k in seen: 
            continue
        seen.add(k); keep.append(t)
    sep='\n' if '\n' in (ann or '') else ' '
    return (sep.join(keep)+sep) if keep else ''
for p in base.rglob("*.kt"):
    if re.search(r'(OneOf|AnyOf|AllOf)', p.stem):
        s=read(p)
        pkg=get_pkg(s)
        cls=p.stem
        new=pkg+"import kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement\n\n@Serializable\ndata class "+cls+"(val raw: JsonElement)\n"
        write(p,new)
decls={}
for p in base.rglob("*.kt"):
    s=read(p)
    for n in decl_rx.findall(s):
        decls.setdefault(n,[]).append(p)
def file_score(p):
    try: s=read(p)
    except: return -1
    score=0
    if 'val raw: JsonElement' in s: score+=100000
    if '@Serializable' in s or '@kotlinx.serialization.Serializable' in s: score+=1000
    score+=s.count('\n')
    return score
for n,files in list(decls.items()):
    if len(files)>1:
        files_sorted=sorted(files,key=file_score,reverse=True)
        keep=files_sorted[0]
        for f in files_sorted[1:]:
            try:
                if f.exists(): f.unlink()
            except: pass
for p in base.rglob("*.kt"):
    s=read(p)
    def insert_ser(m):
        ann=m.group(2) or ''
        kind=m.group(4)
        if kind=='interface': 
            return m.group(0)
        if re.search(r'@(?:kotlinx\.)?serialization\.Serializable\b',ann):
            return m.group(0)
        return m.group(1)+'@Serializable\n'+m.group(0)[len(m.group(1)):]
    t=hdr_rx.sub(insert_ser,s)
    t=re.sub(r'(?s)\A(@file:Suppress\([^\)]*\)\s*\n)(?:\s*\1)+', r'\1', t)
    t=dedup_annot_lines(t)
    if '@Serializable' in t: 
        t=ensure_import(t,'kotlinx.serialization.Serializable')
    if t!=s: 
        write(p,t)
serializable=set()
for p in base.rglob("*.kt"):
    s=read(p)
    for m in hdr_rx.finditer(s):
        ann=m.group(2) or ''
        kind=m.group(4)
        name=m.group(5)
        if kind!='interface' and re.search(r'@(?:kotlinx\.)?serialization\.Serializable\b',ann):
            serializable.add(name)
builtin={'String','Int','Long','Boolean','Double','Float','Short','Byte','Char','Unit','Nothing','Any','List','Map','Set','MutableList','MutableMap','MutableSet','Array','Pair','Triple','ULong','UInt','UShort','UByte','BigInteger','BigDecimal','JsonElement','Optional','Duration'}
def type_tokens(tp):
    t=re.sub(r'\b(?:kotlinx?|java)\.[\w.]*',' ',tp)
    t=re.sub(r'[\[\]<>?,:|&()]',' ',t)
    return [x for x in re.findall(r'\b([A-Z][A-Za-z0-9_]*)\b',t)]
def need_ctx(typ):
    for tok in type_tokens(typ):
        if tok in builtin: 
            continue
        if tok not in serializable:
            return True
    return False
for p in base.rglob("*.kt"):
    s=read(p)
    def prop_fix(m):
        pre,spaces,ann,valvar,name,typ=m.groups()
        ann2=dedup_inline_annotations(ann or '')
        typ2=any_rx.sub(lambda mm: 'JsonElement'+(mm.group(1) or ''), typ)
        add_ctx=need_ctx(typ2) and '@Contextual' not in ann2
        if add_ctx:
            sep='\n' if '\n' in (ann or '') else ' '
            ann2='@Contextual'+sep+ann2
        return f"{pre}{spaces}{ann2}{valvar} {name}: {typ2}"
    t=prop_rx.sub(prop_fix,s)
    t=dedup_annot_lines(t)
    if 'JsonElement' in t: 
        t=ensure_import(t,'kotlinx.serialization.json.JsonElement')
    if '@Contextual' in t: 
        t=ensure_import(t,'kotlinx.serialization.Contextual')
    t=dedup_imports(t)
    if t!=s: 
        write(p,t)
declared=set()
for p in base.rglob("*.kt"):
    s=read(p)
    for n in decl_rx.findall(s):
        declared.add(n)
referenced=set()
for p in base.rglob("*.kt"):
    s=read(p)
    for m in prop_rx.finditer(s):
        typ=m.group(6)
        for tok in type_tokens(typ):
            if tok not in builtin:
                referenced.add(tok)
missing=(referenced-declared)-builtin
force={'StateChangeKindView','StateChangeWithCauseView'}
missing |= force
for name in sorted(missing):
    fp=base/(name+'.kt')
    if fp.exists(): 
        continue
    content=pkg_line+"import kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement\n\n@Serializable\ndata class "+name+"(val raw: JsonElement)\n"
    write(fp,content)
print("OK",base)
PY
./gradlew -p "$MODDIR" -x downloadOpenApiSpec -x fixAndCopyGenerated -x openApiGenerate compileKotlin
