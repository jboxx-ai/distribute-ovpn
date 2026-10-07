# Tasks

## 1. Repository bootstrap

- [ ] 1.1 Create Gradle root, version catalog, wrapper, convention plugins, and module directories.
- [ ] 1.2 Configure Kotlin Multiplatform targets required for JVM, Android, and macOS.
- [ ] 1.3 Add shared dependency-direction verification.

## 2. Shared contracts

- [ ] 2.1 Define versioned API envelope and identifier value objects.
- [ ] 2.2 Define TunnelController, SecureStorage, Clock, FileStore, and telemetry interfaces.
- [ ] 2.3 Add common tests proving platform-neutral state ownership.

## 3. Quality gates

- [ ] 3.1 Configure formatting and static analysis.
- [ ] 3.2 Configure secret scanning and dependency/license reporting.
- [ ] 3.3 Add CI jobs for JVM, Android, macOS framework, tests, and OpenSpec validation.

## 4. Developer experience

- [ ] 4.1 Document bootstrap commands and supported toolchain versions.
- [ ] 4.2 Add one-command local verification entry point.
- [ ] 4.3 Verify a clean checkout passes the complete pipeline.
