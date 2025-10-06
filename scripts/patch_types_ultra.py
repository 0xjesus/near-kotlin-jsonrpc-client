import sys, re, pathlib
MODELS = pathlib.Path(sys.argv[1] if len(sys.argv)>1 else "near-jsonrpc-types/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models")
if not MODELS.exists():
    print("skip, not found", MODELS); sys.exit(0)
pkg_rx = re.compile(r'^(package [^\n]+)', re.M)
for p in MODELS.rglob('*.kt'):
    s = p.read_text(encoding='utf-8')
    m = pkg_rx.search(s)
    pkg = m.group(1) if m else "package org.near.jsonrpc.types.models"
    name = p.stem
    body = pkg + "\nimport kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement\n\n@Serializable\ndata class " + name + "(val raw: JsonElement)\n"
    p.write_text(body, encoding='utf-8')
print("NUKE PATCHED:", MODELS)
