# Design Document: Web Console Profile Registry UI (Cloudflare Kumo UI)

## 1. Design System & Token Integration

The console is built strictly using **Cloudflare Kumo UI** semantic design tokens and Base UI accessible primitives.

### 1.1 Palette & Surface Tokens
- **Background Surfaces**:
  - `bg-kumo-base`: Main app canvas background (`light-dark(#f6f8fa, #0d1117)`).
  - `bg-kumo-elevated`: Cards, sidebars, modal surfaces, table headers (`light-dark(#ffffff, #161b22)`).
  - `bg-kumo-recessed`: Input fields, code editors, and `.ovpn` preview panels (`light-dark(#f0f2f5, #010409)`).
- **Border & Focus Tokens**:
  - `border-kumo-line`: Structural dividers (`light-dark(#d0d7de, #30363d)`).
  - `ring-kumo-hairline`: Keyboard focus ring and subtle active borders.
- **Semantic Status Indicators**:
  - `text-kumo-success` / `bg-kumo-success-subtle`: Active devices, valid policy verification, `STABLE` channel.
  - `text-kumo-warning` / `bg-kumo-warning-subtle`: `BETA` / `CANARY` channels, pending sync.
  - `text-kumo-danger` / `bg-kumo-danger-subtle`: Revoked devices, forbidden directive detection, server errors.
  - `text-kumo-brand`: Cloudflare Accent Orange (`#f38020`) for primary actions.

---

## 2. Information Architecture & Routing Map

```text
/ (App Layout)
├── /overview                   # Live fleet telemetry, server health & quick actions
├── /profiles                   # Logical profile catalog & version counters
│   ├── /profiles/new           # Modal / drawer to create a logical profile ID
│   └── /profiles/:id           # Version history, .ovpn AST inspector, channel promoter
├── /devices                    # Device trust inventory & enrollment management
│   ├── /devices/enroll-modal   # Modal to generate single-use enrollment token
│   └── /devices/:id/revoke     # Revocation confirmation dialog
├── /audit                      # Tamper-evident, secret-redacted audit trail viewer
└── /settings                   # Control plane configuration & Ed25519 public key viewer
```

---

## 3. Page Layouts & Component Specifications

### 3.1 Overview Dashboard (`/overview`)
- **Header**: `PageHeader` with live status indicator (`Server: ONLINE`, `Database: CONNECTED`).
- **Metric Cards Group** (4-column grid):
  - `Total Profiles` (count + active versions)
  - `Enrolled Devices` (active vs revoked count)
  - `Channel Distribution` (STABLE / BETA / CANARY ratios)
  - `Sync Activity (24h)` (successful sync queries)
- **Recent Audit Stream**: Compact table showing recent operator actions with actor name, action verb, and timestamp.

### 3.2 Profile Registry & Detail (`/profiles/:id`)
- **Version Timeline**: Vertical list of immutable version records with cryptographic signatures, SHA-256 digests, and publication channels.
- **OVPN Code Viewer (`bg-kumo-recessed`)**:
  - Syntax highlighted OpenVPN configuration text.
  - Interactive AST policy panel highlighting required vs allowed vs forbidden directives.
- **Channel Promotion Action Bar**: Dropdown to promote selected version to `STABLE`, `BETA`, or `CANARY` with instant optimistic update.

### 3.3 Device Fleet & Enrollment (`/devices`)
- **Device Table**:
  - Columns: `Device Name`, `Platform (Android/macOS)`, `Assigned Profile`, `Channel`, `Status (Badge)`, `Last Seen`, `Actions`.
- **Create Token Dialog**:
  - Form fields: `Device Identifier`, `Platform Selector`, `Profile Dropdown`, `Channel Selector`, `TTL (minutes)`.
  - Output: Single-use enrollment token with one-click copy and QR code generation.
- **Revoke Flow**: Destructive action button with confirmation dialog immediately disabling the device API key.

### 3.4 Tamper-Evident Audit Viewer (`/audit`)
- **Filter Bar**: Search by actor username, action type (`CREATE_PROFILE`, `REVOKE_DEVICE`), or target ID.
- **Audit Table**: Shows redacted JSON payloads (`[REDACTED_SECRET]`) ensuring no passwords, bearer tokens, or private keys leak into the UI.

---

## 4. State Management & API Integration
- **Framework**: React 19 + TypeScript + Vite.
- **Data Fetching**: TanStack Query (React Query) with automatic polling and background cache revalidation.
- **Client Networking**: Type-safe HTTP client consuming standard `ApiEnvelope<T>` responses from `:backend:registry-server`.
