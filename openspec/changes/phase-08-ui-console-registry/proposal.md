# Phase 08 — Web Console Profile Registry UI

## Why
Operators need a responsive, infrastructure-grade graphical web console to manage VPN profiles, inspect immutable `.ovpn` version histories, issue device enrollment tokens, monitor fleet authorization, and audit actions with zero secrets exposure.

## What Changes
- Implement a React / TypeScript Single Page Application (SPA) using Cloudflare's **Kumo UI** component library and design system tokens.
- Design high-density pages for Overview Dashboard, Profile Catalog & Version Inspection, Device Fleet Trust, and Tamper-Evident Audit Trails.
- Connect directly to `:backend:registry-server` REST endpoints (`/api/v1/profiles`, `/api/v1/devices`, `/healthz`, and audit endpoints).

## Capabilities

### New Capabilities
- `console-dashboard-ui`: Fleet telemetry, active version distributions, and server health.
- `profile-management-ui`: Profile creation, version history inspection, policy lint warnings, and channel promotion.
- `device-fleet-ui`: Device list, one-time enrollment token generator, and real-time revocation.
- `audit-trail-ui`: Searchable, secret-redacted audit log viewer with actor and timestamp filters.

## Impact
- Delivers a complete, modern web UI for operators without requiring direct CLI access.
