# Contributing to NEAR Kotlin JSON-RPC Client

Thank you for your interest in contributing to the NEAR Kotlin JSON-RPC Client! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Code Generation](#code-generation)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Project Structure](#project-structure)

## Code of Conduct

This project follows the NEAR Protocol community standards. Be respectful, inclusive, and collaborative. Harassment or discriminatory behavior will not be tolerated.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**Good bug reports include:**
- Clear, descriptive title
- Exact steps to reproduce the issue
- Expected vs actual behavior
- Kotlin version, client version, and OS
- Code samples or error messages
- Screenshots if applicable

**Create bug reports here:** [GitHub Issues](https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client/issues/new)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- Clear description of the proposed feature
- Explain why this enhancement would be useful
- Provide examples of how it would work
- List any alternative solutions you've considered

### Code Contributions

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add/update tests
5. Ensure all tests pass
6. Submit a pull request

## Development Setup

### Prerequisites

- **JDK 17 or higher**
- **Python 3.7+** (for code generation scripts)
- **Gradle 8.5+** (wrapper included)
- **Git**

### Clone and Build

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client.git
cd near-kotlin-jsonrpc-client

# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate coverage report
./gradlew :near-jsonrpc-client:jacocoTestReport
```

### IDE Setup

**IntelliJ IDEA (Recommended)**

1. Open the project: `File → Open → select root directory`
2. Import Gradle project when prompted
3. Set JDK 17: `File → Project Structure → Project SDK`
4. Enable Kotlin plugin if not already enabled

**VS Code**

1. Install extensions: `Kotlin Language` and `Gradle for Java`
2. Open the project folder
3. Run tasks from Gradle sidebar

## Code Generation

This project uses automated code generation from the NEAR OpenAPI specification.

### Regenerate All Code

```bash
# Download latest OpenAPI spec and regenerate everything
./gradlew :near-jsonrpc-types:downloadOpenApiSpec
./gradlew :near-jsonrpc-types:generateKotlinModels
./gradlew :near-jsonrpc-client:generateKotlinClient
./gradlew build
```

### Code Generation Pipeline

1. **Download OpenAPI Spec** - Fetches from nearcore repository
2. **Generate Types** - Creates Kotlin data classes from OpenAPI schemas
3. **Fix Type Names** - Converts snake_case to camelCase
4. **Generate Client** - Creates typed extension functions for RPC methods
5. **Patch Paths** - Ensures all requests use `/` endpoint

### Generation Scripts

- `tools/gen_types_from_openapi.py` - Generates type definitions
- `tools/gen_client_from_openapi.py` - Generates client methods
- `scripts/fix_near_types.sh` - Post-processing and fixes

**Do not manually edit generated files.** Changes will be overwritten. Instead, modify the generation scripts.

## Testing Guidelines

### Test Coverage Requirements

- Minimum **80% coverage** for core functionality
- All new features must include tests
- Bug fixes should include regression tests

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :near-jsonrpc-client:test

# Run with coverage
./gradlew :near-jsonrpc-client:test :near-jsonrpc-client:jacocoTestReport

# View coverage report
open near-jsonrpc-client/build/reports/jacoco/test/html/index.html
```

### Writing Tests

**Unit Tests**
- Test individual functions in isolation
- Use mocking for external dependencies
- Follow AAA pattern (Arrange, Act, Assert)

**Example:**
```kotlin
@Test
fun testBlockQuery() = runBlocking {
    val client = mockClient(json) { _ ->
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":{"header":{"height":123}}}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    
    val result = client.block()
    assertNotNull(result)
}
```

**Integration Tests**
- Test against mock RPC responses
- Verify serialization/deserialization
- Test error handling paths

### Test Naming Convention

- `test[MethodName][Scenario]` - e.g., `testBlockQuerySuccess`
- Be descriptive about what is being tested
- Use `@Test` annotation for all test methods

## Pull Request Process

### Before Submitting

1. **Update documentation** if needed
2. **Add tests** for new functionality
3. **Run all tests**: `./gradlew test`
4. **Check code style**: `./gradlew ktlintCheck`
5. **Update CHANGELOG.md** if applicable
6. **Ensure build passes**: `./gradlew build`

### PR Title Format

Use conventional commits format:

- `feat: add support for custom headers`
- `fix: resolve serialization issue with null values`
- `docs: update README with new examples`
- `test: add coverage for edge cases`
- `chore: update dependencies`
- `refactor: simplify error handling logic`

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] All tests pass
- [ ] Coverage meets threshold

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings generated
```

### Review Process

1. Maintainers will review within 3-5 business days
2. Address feedback and update PR
3. Once approved, a maintainer will merge
4. Squash merging is preferred for clean history

## Coding Standards

### Kotlin Style Guide

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Maximum 120 characters
- **Naming**:
  - Classes: `PascalCase`
  - Functions/Variables: `camelCase`
  - Constants: `SCREAMING_SNAKE_CASE`
- **Braces**: K&R style (opening brace on same line)

### Code Quality

**Required:**
- No compiler warnings
- All tests pass
- Code coverage ≥ 80%
- No unused imports
- Proper error handling

**Recommended:**
- Use `val` over `var` when possible
- Prefer immutability
- Use extension functions appropriately
- Document public APIs with KDoc
- Keep functions small and focused

### Example

```kotlin
/**
 * Queries block information from the NEAR network.
 *
 * @param params Block query parameters (block hash or height)
 * @return Block information wrapped in JSON-RPC response
 * @throws NearRpcException if the RPC call fails
 */
suspend fun NearJsonRpcClient.block(
    params: JsonElement? = null
): JsonRpcResponseForRpcBlockResponseAndRpcError {
    return this.call("block", params, JsonRpcResponseForRpcBlockResponseAndRpcError.serializer())
}
```

### Documentation

- Public APIs must have KDoc comments
- Explain "why" not just "what"
- Include examples for complex functionality
- Update README for user-facing changes

## Project Structure

```
near-kotlin-jsonrpc-client/
├── near-jsonrpc-types/          # Type definitions package
│   ├── src/main/kotlin/         # Generated types (DO NOT EDIT)
│   └── build.gradle.kts         # Package configuration
├── near-jsonrpc-client/         # Client implementation package  
│   ├── src/main/kotlin/
│   │   └── NearJsonRpcClient.kt # Core client (EDIT)
│   ├── src/test/kotlin/         # Tests (EDIT)
│   └── build.gradle.kts         # Package configuration
├── tools/                       # Code generation scripts (EDIT)
│   ├── gen_types_from_openapi.py
│   └── gen_client_from_openapi.py
├── scripts/                     # Build automation (EDIT)
└── .github/workflows/           # CI/CD (EDIT)
```

**Key Points:**
- Do NOT edit generated files in `near-jsonrpc-types/src`
- Do NOT edit `NearJsonRpcClientGenerated.kt`
- DO edit generation scripts to change output
- DO edit `NearJsonRpcClient.kt` for core changes

## Release Process

Releases are automated via GitHub Actions:

1. Changes merged to `main` trigger release-please
2. Release-please creates/updates release PR
3. Merging release PR triggers version bump and publish
4. Artifacts published to Maven Central automatically

**Manual Release** (if needed):
```bash
# Bump version in gradle.properties
# Create and push tag
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin v0.2.0
```

## Getting Help

- **Questions?** Open a [GitHub Discussion](https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client/discussions)
- **Bug?** File an [Issue](https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client/issues)
- **Chat?** Join [NEAR Discord](https://near.chat) #developers channel

## Recognition

Contributors will be recognized in:
- Release notes
- README contributors section
- GitHub contributors page

Thank you for contributing to the NEAR ecosystem!