#!/usr/bin/env python3
import sys, re, pathlib

models_dir = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "near-jsonrpc-types/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models")

if not models_dir.exists():
    print(f"Models dir not found: {models_dir}")
    sys.exit(0)

# List of files that have compilation errors - convert these to JsonElement wrappers
problem_files = [
    'RpcBlockRequest', 'RpcClientConfigRequest', 'RpcClientConfigResponse', 'RpcError',
    'RpcHealthRequest', 'RpcHealthResponse', 'RpcNetworkInfoRequest', 'RpcProtocolConfigRequest',
    'RpcStateChangesInBlockRequest', 'RpcStatusRequest', 'StorageGetMode', 'SyncCheckpoint',
    'VMConfigView', 'RpcPeerInfo', 'RpcKnownProducer', 'RpcValidatorRequest', 'CatchupStatusView',
    'NonDelegateAction', 'DetailedDebugStatus', 'Direction', 'Finality', 'FunctionCallError',
    'GenesisConfigRequest', 'JsonRpcRequestForClientConfig', 'JsonRpcRequestForEXPERIMENTALGenesisConfig',
    'JsonRpcRequestForGenesisConfig', 'JsonRpcRequestForHealth', 'JsonRpcRequestForNetworkInfo',
    'JsonRpcRequestForStatus', 'LogSummaryStyle', 'MerklePathItem', 'MethodResolveError',
    'ProtocolVersionCheckConfig'
]

# Also convert all OneOf/AnyOf/AllOf
problem_patterns = ['OneOf', 'AnyOf', 'AllOf']

pkg_rx = re.compile(r'^(package [^\n]+)\n', re.M)

count = 0
for p in models_dir.rglob('*.kt'):
    should_convert = False

    # Check if it's in the problem list
    if p.stem in problem_files:
        should_convert = True

    # Check if it matches a problem pattern
    for pattern in problem_patterns:
        if pattern in p.stem:
            should_convert = True
            break

    if should_convert:
        s = p.read_text(encoding='utf-8')
        m = pkg_rx.search(s)
        pkg = m.group(1) if m else "package org.near.jsonrpc.types.models"
        name = p.stem
        body = f"""{pkg}
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class {name}(val raw: JsonElement)
"""
        p.write_text(body, encoding='utf-8')
        count += 1

print(f"Selectively patched {count} problem types!")
