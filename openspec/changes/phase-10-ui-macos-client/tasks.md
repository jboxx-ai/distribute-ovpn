# Tasks: Phase 10 — macOS Client UI Architecture (SwiftUI & Kumo UI)

## 1. Design Token & Theme Foundation
- [ ] 1.1 Create SwiftUI `KumoTheme.swift` mapping color tokens and semantic surface styles.
- [ ] 1.2 Implement reusable SwiftUI views (`KumoCardView`, `KumoStatusBadge`, `KumoToggleStyle`).

## 2. MenuBar & Window Implementation
- [ ] 2.1 Build `MenuBarExtraView` with quick connect/disconnect switch and status pill.
- [ ] 2.2 Build `MainWindowView` with `NavigationSplitView` sidebar architecture.
- [ ] 2.3 Build `EnrollmentSheetView` with Keychain credential storage bindings.
- [ ] 2.4 Build `SyncJournalTimelineView` showing 7-state transactional sync status.

## 3. Integration & Testing
- [ ] 3.1 Bind views to shared KMP Kotlin framework state flows.
- [ ] 3.2 Verify accessibility and dark mode contrast ratios on macOS Sequoia / Sonoma.
