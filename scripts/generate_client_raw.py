import json, re, pathlib
from urllib.request import urlopen

def camel(s):
    parts = re.split(r'[^0-9A-Za-z]+', s)
    parts = [p for p in parts if p]
    if not parts: return "call"
    return parts[0].lower() + "".join(w[:1].upper()+w[1:] for w in parts[1:])

def to_kotlin_type(openapi_type):
    """Convert OpenAPI type name to actual Kotlin generated type name"""
    # Replace underscores with nothing and capitalize following letter
    # JsonRpcResponse_for_X_and_Y -> JsonRpcResponseForXAndY
    parts = openapi_type.split('_')
    return ''.join(p.capitalize() if p else '' for p in parts)

def load_spec():
    cands = [
        pathlib.Path("near-jsonrpc-types/build/openapi.json"),
        pathlib.Path("near-jsonrpc-client/build/openapi.json"),
        pathlib.Path("openapi.json"),
    ]
    for c in cands:
        if c.exists():
            return json.loads(c.read_text(encoding="utf-8"))
    url="https://raw.githubusercontent.com/near/nearcore/master/chain/jsonrpc/openapi/openapi.json"
    return json.loads(urlopen(url, timeout=30).read().decode("utf-8"))

spec = load_spec()
paths = spec.get("paths", {}) or {}

# Extract response schemas from paths
method_schemas = {}
for path, path_item in paths.items():
    m = path.strip("/")
    if not m: continue
    m = m.split("/")[-1]
    
    # Get POST operation (JSON-RPC uses POST)
    post_op = path_item.get("post", {})
    responses = post_op.get("responses", {})
    success_resp = responses.get("200", {})
    content = success_resp.get("content", {})
    json_content = content.get("application/json", {})
    schema = json_content.get("schema", {})
    
    # Extract reference to response type
    ref = schema.get("$ref", "")
    if ref:
        # Extract type name from #/components/schemas/TypeName
        type_name = ref.split("/")[-1]
        kotlin_type = to_kotlin_type(type_name)
        method_schemas[m] = f"org.near.jsonrpc.types.models.{kotlin_type}"

methods = []
seen = set()
for path in paths.keys():
    m = path.strip("/")
    if not m: continue
    m = m.split("/")[-1]
    if m and m not in seen:
        seen.add(m)
        methods.append(m)

if "status" not in seen:
    methods.append("status")

out = []
out.append("package org.near.jsonrpc.client")
out.append("")
out.append("import kotlinx.serialization.json.JsonElement")
out.append("")

for m in methods:
    fn = camel(m)
    if m in method_schemas:
        type_ref = method_schemas[m]
        out.append(f'suspend fun NearJsonRpcClient.{fn}(params: JsonElement? = null): {type_ref} = this.call("{m}", params, {type_ref}.serializer())')
    else:
        out.append(f'suspend fun NearJsonRpcClient.{fn}Raw(params: JsonElement? = null): JsonElement = this.call("{m}", params, JsonElement.serializer())')

dst = pathlib.Path("near-jsonrpc-client/build/generated-src/org/near/jsonrpc/client/NearJsonRpcClientGenerated.kt")
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text("\n".join(out) + "\n", encoding="utf-8")
print("RAW CLIENT GENERATED:", dst)