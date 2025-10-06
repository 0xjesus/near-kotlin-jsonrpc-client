#!/usr/bin/env python3
import sys
from pathlib import Path
import re

target_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("near-jsonrpc-types/build/generated-fixed")
gen_dir = target_dir / "src/main/kotlin"

problem_types = set()

for kt_file in gen_dir.rglob("*.kt"):
    content = kt_file.read_text()
    for match in re.finditer(r'data\s+class\s+(\w+)\s*\(\s*\)', content):
        problem_types.add(match.group(1))
        content = content.replace(match.group(0), f'object {match.group(1)}')
    if ': kotlin.Any' in content or ': Any?' in content:
        problem_types.add(kt_file.stem)
        content = '\n'.join(line for line in content.split('\n') if '@Serializable' not in line and '@SerialName' not in line and '@Contextual' not in line or 'class ' in line)
    if 'RpcError.kt' in str(kt_file) and 'enum class Name' not in content and not content.rstrip().endswith('}'):
        problem_types.add('RpcError')
        content = content.rstrip() + '\n {\n    enum class Name { REQUEST_VALIDATION_ERROR, HANDLER_ERROR, INTERNAL_ERROR }\n}\n'
    kt_file.write_text(content)

problem_types.update(['NonDelegateAction', 'CatchupStatusView', 'RpcPeerInfo', 'RpcKnownProducer'])

for kt_file in gen_dir.rglob("*.kt"):
    content = kt_file.read_text()
    
    for ptype in problem_types:
        content = re.sub(
            rf'(\s*)@SerialName\([^)]+\)\n\s*val (\w+): [^<]*{ptype}',
            rf'\1@Contextual\n\1val \2: kotlin.collections.List<{ptype}>',
            content
        )
        content = re.sub(
            rf'(\s*)val (\w+): ([^<]*{ptype}[^,\n]*)',
            lambda m: f'{m.group(1)}@Contextual\n{m.group(1)}val {m.group(2)}: {m.group(3)}' if '@Contextual' not in content[max(0,m.start()-50):m.start()] else m.group(0),
            content
        )
    
    if '@Contextual' in content and 'import kotlinx.serialization.Contextual' not in content:
        content = content.replace('import kotlinx.serialization.Serializable', 'import kotlinx.serialization.Serializable\nimport kotlinx.serialization.Contextual')
    
    kt_file.write_text(content)

print(f"Fixed {len(problem_types)} types!")
