# Phase 00 — KMP Monorepo Foundation

## Why

The platform needs a deterministic monorepo and dependency graph before backend or VPN platform work begins. This phase establishes build, module, testing, and CI contracts so every later phase can evolve safely.

## What Changes

- Create the Kotlin Multiplatform monorepo and module boundaries.
- Establish shared API/domain modules that compile for JVM, Android, and Apple targets.
- Establish native OpenVPN bridge boundaries without implementing tunnel behavior yet.
- Add reproducible CI, formatting, static analysis, unit-test, and artifact checks.
- Define versioning and compatibility conventions for APIs, manifests, and clients.

## Capabilities

### New Capabilities

- `repository-foundation`
- `shared-contracts`
- `build-quality-gates`

## Impact

- Creates the repository skeleton used by every subsequent phase.
- No end-user VPN behavior is introduced.
- No production secrets are required.
