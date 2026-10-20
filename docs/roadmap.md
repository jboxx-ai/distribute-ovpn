# Roadmap and Dependency Gates

| Phase | Depends on | Exit gate |
|---|---|---|
| 00 Foundation | none | monorepo builds on CI; shared test app compiles for JVM/Android/Apple |
| 01 Profile Registry | 00 | versioned `.ovpn` upload/fetch works with immutable history |
| 02 Device Trust | 01 | enrolled device can authenticate and verify signed profile |
| 03 Android Client | 02 | Android completes the state-driven enrollment journey, syncs a verified profile, handles VPN permission, and demonstrates real connect/disconnect evidence |
| 04 macOS Client | 02 | macOS can sync, install NetworkExtension config, connect, disconnect |
| 05 Safe Sync & Rollout | 03,04 | atomic update + rollback + channels demonstrated on both platforms |
| 06 Operations & Admin | 05 | CLI, audit, metrics, backups and restore drill pass |
| 07 Production Hardening | 06 | security, upgrade, disaster-recovery, release gates pass |

## Branching model

Recommended:
- `main` is releasable.
- Each OpenSpec phase change is implemented on a short-lived branch.
- A phase merges only after its acceptance scenarios pass.
- Later phases may start design work early, but code must not depend on an unmerged phase contract.

## Release model

- backend: semantic versions
- Android: semantic app version + monotonically increasing version code
- macOS: semantic app version + build number
- shared protocol: explicit API schema version
- profile manifest: explicit schema version
