#!/usr/bin/env bash
set -euo pipefail
MODULE=near-jsonrpc-types
GEN_DIR="$MODULE/build/generated-fixed/src/main/kotlin/org/near/jsonrpc/types/models"
./gradlew :$MODULE:clean
rm -rf "$MODULE/build/generated-fixed" "$MODULE/build/generated-raw"
./gradlew :$MODULE:downloadOpenApiSpec :$MODULE:fixAndCopyGenerated
python3 scripts/reset_and_patch_types.py "$GEN_DIR"
./gradlew :$MODULE:compileKotlin
./gradlew :near-jsonrpc-client:generateKotlinClient
./gradlew clean build
