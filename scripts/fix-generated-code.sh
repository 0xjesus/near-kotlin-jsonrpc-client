#!/bin/bash
set -e

GENERATED_DIR="near-jsonrpc-types/build/generated/src/main/kotlin"

find "$GENERATED_DIR" -name "*.kt" -type f | while read file; do
    perl -i -pe 's/^data class (\w+)\s*\(\s*\)$/class $1/' "$file"
    
    if grep -q ': kotlin.Any' "$file" || grep -q ': Any\?' "$file"; then
        perl -i -pe 's/^@Serializable\s*$//' "$file"
        perl -i -pe 's/^\s*@SerialName.*$//' "$file"
    fi
done

if [ -f "$GENERATED_DIR/org/near/jsonrpc/types/model/RpcError.kt" ]; then
    if ! grep -q "enum class Name" "$GENERATED_DIR/org/near/jsonrpc/types/model/RpcError.kt"; then
        echo "" >> "$GENERATED_DIR/org/near/jsonrpc/types/model/RpcError.kt"
        echo " {" >> "$GENERATED_DIR/org/near/jsonrpc/types/model/RpcError.kt"
        echo "    enum class Name { REQUEST_VALIDATION_ERROR, HANDLER_ERROR, INTERNAL_ERROR }" >> "$GENERATED_DIR/org/near/jsonrpc/types/model/RpcError.kt"
        echo "}" >> "$GENERATED_DIR/org/near/jsonrpc/types/model/RpcError.kt"
    fi
fi

echo "Code fixed successfully"
