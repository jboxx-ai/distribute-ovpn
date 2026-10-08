# Tasks: Phase 08 — Web Console Profile Registry UI

## 1. Setup & Project Scaffolding
- [ ] 1.1 Initialize console frontend package in `backend/console-ui/` using Vite + React + TypeScript.
- [ ] 1.2 Install `@cloudflare/kumo`, Base UI primitives, and Tailwind CSS v4.
- [ ] 1.3 Configure Kumo semantic tokens and dark/light theme switching.

## 2. Component Implementation
- [ ] 2.1 Build App Shell (`SidebarNavigation`, `TopHeader`, `SystemHealthBadge`).
- [ ] 2.2 Implement Overview dashboard metric cards and activity stream.
- [ ] 2.3 Implement Profile catalog, `.ovpn` syntax viewer, and channel promotion dropdown.
- [ ] 2.4 Implement Device Fleet table, Token Generation modal, and Revoke confirmation modal.
- [ ] 2.5 Implement Audit Log searchable table with secret redaction pill indicators.

## 3. Integration & Testing
- [ ] 3.1 Wire API client against `:backend:registry-server` endpoints (`/api/v1/*`, `/healthz`).
- [ ] 3.2 Add mock integration tests for token generation and revocation workflows.
- [ ] 3.3 Bundle frontend build into backend static asset distribution.
