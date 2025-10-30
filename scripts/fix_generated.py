#!/usr/bin/env python3
import sys
from pathlib import Path
import re

target_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("near-jsonrpc-types/build/generated-fixed")
gen_dir = target_dir / "src/main/kotlin"

problem_types = set()

# First pass: identify problem types and fix basic issues
for kt_file in gen_dir.rglob("*.kt"):
    content = kt_file.read_text()

    # Find empty data classes
    for match in re.finditer(r'data\s+class\s+(\w+)\s*\(\s*\)', content):
        problem_types.add(match.group(1))
        content = content.replace(match.group(0), f'object {match.group(1)}')

    # Find classes with 'Any' type
    if ': kotlin.Any' in content or ': Any?' in content:
        problem_types.add(kt_file.stem)
        # Replace 'Any' with JsonElement
        content = re.sub(r': kotlin\.Any\?', ': kotlinx.serialization.json.JsonElement?', content)
        content = re.sub(r': kotlin\.Any\b', ': kotlinx.serialization.json.JsonElement', content)
        content = re.sub(r': Any\?', ': kotlinx.serialization.json.JsonElement?', content)
        content = re.sub(r': Any\b', ': kotlinx.serialization.json.JsonElement', content)

        # Add JsonElement import if not present
        if 'kotlinx.serialization.json.JsonElement' not in content:
            content = content.replace(
                'import kotlinx.serialization.Serializable',
                'import kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement'
            )

    # Handle OneOf/AnyOf/AllOf types - convert to JsonElement wrappers
    if re.search(r'(OneOf|AnyOf|AllOf)', kt_file.stem):
        m = re.search(r'^package\s+([^\n]+)', content, re.M)
        pkg = m.group(0) if m else "package org.near.jsonrpc.types.models"
        name = kt_file.stem
        content = f"""{pkg}
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class {name}(val raw: JsonElement)
"""
        problem_types.add(name)

    kt_file.write_text(content)

# Add known problem types
problem_types.update(['NonDelegateAction', 'CatchupStatusView', 'RpcPeerInfo', 'RpcKnownProducer', 'RpcError', 'RpcValidatorRequest'])

# Second pass: add @Contextual annotations
for kt_file in gen_dir.rglob("*.kt"):
    content = kt_file.read_text()
    original_content = content

    for ptype in problem_types:
        # Add @Contextual before properties of problem types
        content = re.sub(
            rf'(\s*)@SerialName\(([^\)]+)\)\n(\s*)val (\w+): ([^<\n]*{ptype}[^\n]*)',
            rf'\1@SerialName(\2)\n\1@Contextual\n\3val \4: \5',
            content
        )

        # Add @Contextual for properties without @SerialName
        lines = content.split('\n')
        new_lines = []
        i = 0
        while i < len(lines):
            line = lines[i]
            # Check if this line matches pattern and doesn't already have @Contextual or @SerialName before it
            if re.match(rf'(\s*)val (\w+): ([^<\n]*{ptype}[^\n]*)', line):
                # Check previous lines for @Contextual or @SerialName
                has_annotation = False
                j = i - 1
                while j >= 0 and (lines[j].strip() == '' or lines[j].strip().startswith('@')):
                    if '@Contextual' in lines[j] or '@SerialName' in lines[j]:
                        has_annotation = True
                        break
                    j -= 1

                if not has_annotation:
                    indent = re.match(r'(\s*)', line).group(1)
                    new_lines.append(f'{indent}@Contextual')

            new_lines.append(line)
            i += 1

        content = '\n'.join(new_lines)

    # Remove duplicate @Contextual annotations (on consecutive lines)
    lines = content.split('\n')
    filtered_lines = []
    last_was_contextual = False
    for line in lines:
        is_contextual = line.strip() == '@Contextual'
        if not (is_contextual and last_was_contextual):
            filtered_lines.append(line)
        last_was_contextual = is_contextual

    content = '\n'.join(filtered_lines)

    # Add import if needed
    if '@Contextual' in content and 'import kotlinx.serialization.Contextual' not in content:
        content = content.replace(
            'import kotlinx.serialization.Serializable',
            'import kotlinx.serialization.Serializable\nimport kotlinx.serialization.Contextual'
        )

    # Only write if changed
    if content != original_content:
        kt_file.write_text(content)

print(f"Fixed {len(problem_types)} types!")
