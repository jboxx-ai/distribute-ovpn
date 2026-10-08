# 3bc5413d-e3ea-4977-8aed-ae9cdffb965c implementation handoff

This archive is the source of truth for turning the design into production code. Start from `index.html`, then preserve the visual system, responsive behavior, and interactions found in the exported files.

## Implementation target
- Build production UI from the exported design, not a loose reinterpretation.
- Preserve typography scale, spacing rhythm, color tokens, border radii, shadows, motion timing, and component states.
- Replace static placeholders only when the target app has real data or functional equivalents.
- Keep generated product UI free of OpenDesign chrome, preview labels, or design-process annotations.
- Treat this handoff as a visual contract: if implementation choices conflict, match the exported pixels and behavior first, then refactor internals.

## Source map
- Primary entry: `index.html`
- HTML screens detected: 3
- Stylesheets detected: 0
- Script/component files detected: 0
- Supporting assets detected: 99

## Responsive contract
Validate the implementation across this 2025–2026 viewport matrix:
- Mobile compact: 360×800
- Mobile standard: 390×844
- Mobile large: 430×932
- Foldable / small tablet: 600×960
- Tablet portrait: 820×1180
- Tablet landscape: 1024×768
- Laptop: 1366×768
- Desktop: 1440×900
- Wide desktop: 1920×1080

For responsive web exports, treat these as a modern breakpoint system for one adaptive web experience, not three fixed screenshots. Do not split responsive web into unrelated native app screens unless the project explicitly includes native targets. Use semantic layout thresholds, fluid `clamp()` type/spacing, and container queries where component width matters more than viewport width. Preserve any CSS media queries, container queries, fluid `clamp()` scales, and layout changes already present in the exported files.

## Design fidelity contract
- Extract reusable tokens before writing components: background, surface, foreground, muted text, border, accent, radius, shadow, spacing, type scale, and motion duration/easing.
- Map product screens, in-app modules/components, optional landing page, and optional OS widget surfaces before coding. Keep these surfaces separate in the target architecture.
- Match layout geometry: max-widths, gutters, grid columns, card proportions, sticky/fixed elements, and viewport-specific navigation.
- Preserve real copy, labels, and data shown in the export. Do not replace specific text with generic marketing filler.
- Preserve interactive affordances: hover, focus, pressed, disabled, loading, validation, copy/share, tab/accordion, modal/sheet, and keyboard states where present.
- Preserve accessibility semantics when converting: headings stay hierarchical, controls remain buttons/links/inputs, focus states stay visible.
- Do not keep prototype-only annotations, frame labels, or OpenDesign chrome in the production UI.

## CJX-ready UX contract
- Use `DESIGN-MANIFEST.json` as the machine-readable map for screens, app modules, OS widgets, landing pages, tokens, interactions, and viewport checks.
- Screen-file-first: when multiple user-facing surfaces exist, implement each HTML screen as its own route/file. Treat `index.html` as a launcher/overview when the manifest marks it that way, not as a combined final UI.
- If `landing.html`, app screens, platform screens, or OS widget files exist, preserve those boundaries in the target app instead of merging them into one page.
- A single self-contained `index.html` is acceptable only when the export truly contains one user-facing screen and its CSS/JS are structured enough to extract tokens, components, states, and behavior.
- If separate `css/` or `js/` files exist, treat them as source of truth for token/component/interactions before porting to React, Vue, SwiftUI, Compose, or another target stack.
- In-app modules/components are product UI blocks inside the app. OS widgets are home-screen/lock-screen/quick-access surfaces outside the app. Do not merge those concepts.

## Color and brand contract
- Use the exported design tokens and product/domain context as the color source of truth.
- Do not introduce warm beige / cream / peach / pink / orange-brown background washes unless they are already explicit brand/reference colors in the export.
- A stylesheet or design/token file was detected; inspect it for canonical color variables before choosing framework theme tokens.

## Implementation sequence for AI coding tools
1. Open `index.html` and `DESIGN-MANIFEST.json`; identify every screen file, launcher/overview file, app module, and interaction before coding.
2. If multiple HTML screens exist, map them to separate routes/surfaces first; do not merge `landing.html`, product app screens, platform screens, or OS widgets into one route.
3. Extract a token table from CSS/root styles and inline styles before building framework components.
4. Build product screens and domain-specific in-app modules from largest layout regions down to controls; avoid starting with isolated atoms that lose spatial intent.
5. Port responsive behavior across the modern viewport matrix and test each semantic breakpoint before cleanup.
6. Port interactions and states, then replace static placeholders only with real app data or functional equivalents.
7. Keep optional landing page and OS widget surfaces as separate surfaces if present.
8. Compare final screenshots against the export at 360×800, 390×844, 430×932, 820×1180, 1024×768, 1366×768, 1440×900, and 1920×1080 before declaring done.

## Entry points
- `android-client.html`
- `index.html`
- `macos-client.html`

## Styles
- None detected

## Scripts/components
- None detected

## Assets and supporting files
- `admin-cli-1.md`
- `admin-cli.md`
- `android-app-1.md`
- `android-app.md`
- `design-1.md`
- `design-10.md`
- `design-2-1.md`
- `design-2.md`
- `design-3-1.md`
- `design-3.md`
- `design-4.md`
- `design-5.md`
- `design-6.md`
- `design-7.md`
- `design-8.md`
- `design-9.md`
- `design.md`
- `docker-deployment-1.md`
- `docker-deployment.md`
- `drawing-2026-10-07T14-36-37-937Z.png`
- `drawing-2026-10-07T14-52-28-665Z.png`
- `drawing-2026-10-07T14-59-54-405Z.png`
- `drawing-2026-10-07T15-02-23-521Z.png`
- `FILE_INDEX-1.md`
- `FILE_INDEX.md`
- `macos-app-1.md`
- `macos-app.md`
- `proposal-1.md`
- `proposal-10.md`
- `proposal-2-1.md`
- `proposal-2.md`
- `proposal-3-1.md`
- `proposal-3.md`
- `proposal-4.md`
- `proposal-5.md`
- `proposal-6.md`
- `proposal-7.md`
- `proposal-8.md`
- `proposal-9.md`
- `proposal.md`
- `README-1.md`
- `README-2.md`
- `README-3.md`
- `README-4.md`
- `README-5.md`
- `README.md`
- `registry-server-1.md`
- `registry-server.md`
- `shared-core-1.md`
- `shared-core.md`
- `spec-1.md`
- `spec-10.md`
- `spec-11.md`
- `spec-12.md`
- `spec-13.md`
- `spec-14.md`
- `spec-15.md`
- `spec-16.md`
- `spec-17.md`
- `spec-18.md`
- `spec-19.md`
- `spec-2.md`
- `spec-20.md`
- `spec-21.md`
- `spec-22.md`
- `spec-23.md`
- `spec-24.md`
- `spec-25.md`
- `spec-26.md`
- `spec-27.md`
- `spec-28.md`
- `spec-29.md`
- `spec-3.md`
- `spec-30.md`
- `spec-4.md`
- `spec-5.md`
- `spec-6.md`
- `spec-7.md`
- `spec-8.md`
- `spec-9.md`
- `spec.md`
- `sync-and-activation-1.md`
- `sync-and-activation.md`
- `system-architecture-1.md`
- `system-architecture.md`
- `tasks-1.md`
- `tasks-10.md`
- `tasks-2-1.md`
- `tasks-2.md`
- `tasks-3-1.md`
- `tasks-3.md`
- `tasks-4.md`
- `tasks-5.md`
- `tasks-6.md`
- `tasks-7.md`
- `tasks-8.md`
- `tasks-9.md`
- `tasks.md`
- `ui-design-systems.md`

## Coding checklist for AI tools
1. Inspect `index.html` and `DESIGN-MANIFEST.json` first and identify reusable components before coding.
2. Implement each user-facing screen file as its own route/surface; keep launcher, landing, app, platform, and OS widget files separate.
3. Extract design tokens into the target stack: colors, type scale, spacing, radius, shadows, and motion.
4. Implement layout with real 2025–2026 responsive breakpoints, fluid type/spacing, and container-query-aware component behavior; test with no horizontal overflow.
5. Preserve interactive controls, hover/focus/pressed states, form behavior, validation, and copy actions where present.
6. Implement domain-specific in-app modules with real states; do not flatten them into generic cards.
7. Keep landing page, product screens, and OS widget/quick-access surfaces separate when present.
8. Confirm the production result visually matches the exported design before refactoring internals.
9. Reject implementation shortcuts that flatten the design into generic cards, generic gradients, placeholder stats, or framework-default typography.
10. If a detail is ambiguous, keep the exported HTML/CSS/JS behavior rather than inventing a new pattern.
