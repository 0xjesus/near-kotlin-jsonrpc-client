import sys, json, re, pathlib, os
from urllib.request import urlopen

out_dir = pathlib.Path(sys.argv[1]); out_dir.mkdir(parents=True, exist_ok=True)
repo_root = pathlib.Path(__file__).resolve().parents[1]
spec_file_env = os.environ.get("SPEC_FILE")
candidates=[]
if spec_file_env: candidates.append(pathlib.Path(spec_file_env))
candidates += [repo_root/"near-jsonrpc-types"/"build", repo_root/"build", repo_root]
spec_path=None
for base in candidates:
    if base.is_file() and base.name=="openapi.json": spec_path=base; break
    if base.is_dir():
        hit=next(base.rglob("openapi.json"), None)
        if hit: spec_path=hit; break
if spec_path and spec_path.exists():
    data=json.loads(spec_path.read_text(encoding="utf-8"))
else:
    url="https://raw.githubusercontent.com/near/nearcore/master/chain/jsonrpc/openapi/openapi.json"
    data=json.loads(urlopen(url, timeout=30).read().decode("utf-8"))

paths=data.get("paths",{})

def pascal_to_camel(name): 
    return name[:1].lower()+name[1:] if name else name

def to_kotlin_type(openapi_schema_name):
    """Convert OpenAPI schema name (with underscores) to Kotlin type name (camelCase)"""
    # JsonRpcResponse_for_RpcBlockResponse_and_RpcError -> JsonRpcResponseForRpcBlockResponseAndRpcError
    parts = openapi_schema_name.split('_')
    result = []
    for p in parts:
        if p:
            # Capitalizar SOLO la primera letra, mantener el resto como está
            result.append(p[0].upper() + p[1:] if len(p) > 1 else p.upper())
    return ''.join(result)
def pick_ret(schema_ref):
    """Extract just the response type name from wrapper, or return full type"""
    # Convert to Kotlin naming first
    kotlin_name = to_kotlin_type(schema_ref)
    
    # Now check if it's a wrapped response type
    if kotlin_name.startswith("JsonRpcResponseFor") and "AndRpcError" in kotlin_name:
        # Return the full Kotlin type name (it's already correct)
        return kotlin_name
    
    return kotlin_name

k = ["package org.near.jsonrpc.client","","import kotlinx.serialization.json.JsonElement",""]
k.append('suspend fun NearJsonRpcClient.statusRaw(params: JsonElement? = null): JsonElement = this.call("status", params, JsonElement.serializer())')

for path,ops in paths.items():
    op=ops.get("post") or ops.get("get") or {}
    op_id=op.get("operationId") or re.sub(r'[^a-zA-Z0-9]', '_', path).strip('_').title()
    resp=(op.get("responses") or {}).get("200") or {}
    content=(resp.get("content") or {}).get("application/json") or {}
    schema=content.get("schema") or {}
    ref=schema.get("$ref")
    ret="kotlinx.serialization.json.JsonElement"
    
    if isinstance(ref,str) and "#/components/schemas/" in ref:
        name=ref.split("#/components/schemas/")[-1]
        ret=pick_ret(name)
    
    method_name=path.strip("/").split("/")[-1] if path.strip("/") else "root"
    if method_name=="root": continue
    
    fname=pascal_to_camel(op_id)
    
    if ret=="kotlinx.serialization.json.JsonElement":
        k.append(f'suspend fun NearJsonRpcClient.{fname}(params: JsonElement? = null): JsonElement = this.call("{method_name}", params, JsonElement.serializer())')
    else:
        k.append(f'suspend fun NearJsonRpcClient.{fname}(params: JsonElement? = null): org.near.jsonrpc.types.models.{ret} = this.call("{method_name}", params, org.near.jsonrpc.types.models.{ret}.serializer())')

(out_dir/"NearJsonRpcClientGenerated.kt").write_text("\n".join(k)+"\n", encoding="utf-8")
print("Generated", out_dir/"NearJsonRpcClientGenerated.kt")